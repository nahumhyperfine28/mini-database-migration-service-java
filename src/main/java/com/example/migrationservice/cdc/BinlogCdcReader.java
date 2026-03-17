package com.example.migrationservice.cdc;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.example.migrationservice.checkpoint.CheckpointState;
import com.example.migrationservice.config.MigrationProperties;
import com.example.migrationservice.replication.EventTransformer;
import com.example.migrationservice.schema.SchemaDiscoveryService;
import com.example.migrationservice.schema.TableMetadata;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.EventHeaderV4;
import com.github.shyiko.mysql.binlog.event.RotateEventData;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reads ordered MySQL row-based binlog events and turns them into
 * application-level replication events with checkpoint positions.
 */
@Component
public class BinlogCdcReader {

    private static final Logger log = LoggerFactory.getLogger(BinlogCdcReader.class);

    private final MigrationProperties properties;
    private final JdbcTemplate sourceJdbcTemplate;
    private final SchemaDiscoveryService schemaDiscoveryService;
    private final EventTransformer eventTransformer;

    public BinlogCdcReader(
        MigrationProperties properties,
        @Qualifier("sourceJdbcTemplate") JdbcTemplate sourceJdbcTemplate,
        SchemaDiscoveryService schemaDiscoveryService,
        EventTransformer eventTransformer
    ) {
        this.properties = properties;
        this.sourceJdbcTemplate = sourceJdbcTemplate;
        this.schemaDiscoveryService = schemaDiscoveryService;
        this.eventTransformer = eventTransformer;
    }

    public CheckpointState captureCurrentCheckpoint() {
        Map<String, Object> masterStatus;
        try {
            // MySQL 8 prefers SHOW BINARY LOG STATUS; SHOW MASTER STATUS is kept as a fallback.
            masterStatus = sourceJdbcTemplate.queryForMap("SHOW BINARY LOG STATUS");
        } catch (BadSqlGrammarException exception) {
            masterStatus = sourceJdbcTemplate.queryForMap("SHOW MASTER STATUS");
        }
        String binlogFile = String.valueOf(masterStatus.get("File"));
        long binlogPosition = ((Number) masterStatus.get("Position")).longValue();
        return CheckpointState.of(binlogFile, binlogPosition);
    }

    public void start(CheckpointState startState, BinlogEventHandler eventHandler) {
        BinaryLogClient client = new BinaryLogClient(
            properties.getSource().getHost(),
            properties.getSource().getPort(),
            properties.getSource().getDatabase(),
            properties.getSource().getUsername(),
            properties.getSource().getPassword()
        );
        client.setServerId(properties.getCdc().getServerId());
        client.setKeepAlive(true);
        client.setKeepAliveInterval(properties.getCdc().getKeepaliveIntervalMs());
        client.setConnectTimeout(properties.getCdc().getConnectTimeoutMs());

        if (startState != null && startState.getBinlogFilename() != null && !startState.getBinlogFilename().isBlank()) {
            client.setBinlogFilename(startState.getBinlogFilename());
            client.setBinlogPosition(Math.max(startState.getBinlogPosition(), 4L));
        }

        AtomicReference<String> currentBinlogFile = new AtomicReference<>(startState != null ? startState.getBinlogFilename() : null);
        // Table-map events provide the table id to table name mapping required for row events.
        Map<Long, TableMapEventData> tableMapCache = new ConcurrentHashMap<>();

        client.registerLifecycleListener(new BinaryLogClient.AbstractLifecycleListener() {
            @Override
            public void onConnect(BinaryLogClient binaryLogClient) {
                log.info("Connected to MySQL binlog at {}:{}", binaryLogClient.getBinlogFilename(), binaryLogClient.getBinlogPosition());
            }

            @Override
            public void onCommunicationFailure(BinaryLogClient binaryLogClient, Exception ex) {
                log.error("Binlog communication failure", ex);
            }

            @Override
            public void onEventDeserializationFailure(BinaryLogClient binaryLogClient, Exception ex) {
                log.error("Binlog event deserialization failure", ex);
            }

            @Override
            public void onDisconnect(BinaryLogClient binaryLogClient) {
                log.warn("Disconnected from MySQL binlog stream");
            }
        });

        client.registerEventListener(event -> processEvent(event, client, currentBinlogFile, tableMapCache, eventHandler));

        try {
            log.info(
                "Starting CDC from binlog file {} position {}",
                client.getBinlogFilename(),
                client.getBinlogPosition()
            );
            client.connect();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start MySQL CDC reader", exception);
        }
    }

    private void processEvent(
        Event event,
        BinaryLogClient client,
        AtomicReference<String> currentBinlogFile,
        Map<Long, TableMapEventData> tableMapCache,
        BinlogEventHandler eventHandler
    ) {
        EventData eventData = event.getData();
        if (eventData == null) {
            return;
        }

        if (eventData instanceof RotateEventData rotateEventData) {
            currentBinlogFile.set(rotateEventData.getBinlogFilename());
            log.info("MySQL rotated binlog to {}", rotateEventData.getBinlogFilename());
            return;
        }

        if (eventData instanceof TableMapEventData tableMapEventData) {
            tableMapCache.put(tableMapEventData.getTableId(), tableMapEventData);
            return;
        }

        Long tableId = extractTableId(eventData);
        if (tableId == null) {
            return;
        }

        TableMapEventData tableMap = tableMapCache.get(tableId);
        if (tableMap == null) {
            log.warn("Skipped row event because table map for table id {} was not available", tableId);
            return;
        }

        Optional<TableMetadata> metadata = schemaDiscoveryService.resolveTable(tableMap.getDatabase(), tableMap.getTable());
        if (metadata.isEmpty()) {
            log.debug("Skipping row event for {}.{} because it is outside the configured source schema/filter", tableMap.getDatabase(), tableMap.getTable());
            return;
        }

        CheckpointState checkpointState = buildCheckpoint(event, currentBinlogFile.get(), client);
        for (ReplicationEvent replicationEvent : eventTransformer.transform(metadata.get(), eventData)) {
            eventHandler.handle(replicationEvent, checkpointState);
        }
    }

    private Long extractTableId(EventData eventData) {
        if (eventData instanceof WriteRowsEventData writeRowsEventData) {
            return writeRowsEventData.getTableId();
        }
        if (eventData instanceof UpdateRowsEventData updateRowsEventData) {
            return updateRowsEventData.getTableId();
        }
        if (eventData instanceof DeleteRowsEventData deleteRowsEventData) {
            return deleteRowsEventData.getTableId();
        }
        return null;
    }

    private CheckpointState buildCheckpoint(Event event, String currentBinlogFile, BinaryLogClient client) {
        String binlogFile = currentBinlogFile != null ? currentBinlogFile : client.getBinlogFilename();
        long position = client.getBinlogPosition();
        if (event.getHeader() instanceof EventHeaderV4 header) {
            // nextPosition points to the next event boundary, which is the correct resume offset.
            position = header.getNextPosition();
        }
        return CheckpointState.of(binlogFile, position);
    }
}
