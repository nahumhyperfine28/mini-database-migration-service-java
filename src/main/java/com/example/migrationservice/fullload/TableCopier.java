package com.example.migrationservice.fullload;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.example.migrationservice.config.MigrationProperties;
import com.example.migrationservice.replication.TargetApplier;
import com.example.migrationservice.schema.ColumnMetadata;
import com.example.migrationservice.schema.TableMetadata;
import com.example.migrationservice.util.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TableCopier {

    private static final Logger log = LoggerFactory.getLogger(TableCopier.class);

    private final JdbcTemplate sourceJdbcTemplate;
    private final TargetApplier targetApplier;
    private final int batchSize;

    public TableCopier(
        @Qualifier("sourceJdbcTemplate") JdbcTemplate sourceJdbcTemplate,
        TargetApplier targetApplier,
        MigrationProperties properties
    ) {
        this.sourceJdbcTemplate = sourceJdbcTemplate;
        this.targetApplier = targetApplier;
        this.batchSize = properties.getFullLoad().getBatchSize();
    }

    public long copyTable(TableMetadata metadata) {
        targetApplier.ensureTable(metadata);

        AtomicLong copiedRows = new AtomicLong();
        List<Map<String, Object>> batch = new ArrayList<>(batchSize);

        log.info("Starting full load for table {}", metadata.cacheKey());
        sourceJdbcTemplate.query(SqlBuilder.buildSelectAllSql(metadata), rs -> {
            batch.add(extractRow(rs, metadata));
            if (batch.size() >= batchSize) {
                flushBatch(metadata, batch, copiedRows);
            }
        });

        if (!batch.isEmpty()) {
            flushBatch(metadata, batch, copiedRows);
        }

        log.info("Finished full load for table {} with {} row(s)", metadata.cacheKey(), copiedRows.get());
        return copiedRows.get();
    }

    private void flushBatch(TableMetadata metadata, List<Map<String, Object>> batch, AtomicLong copiedRows) {
        List<Map<String, Object>> rows = List.copyOf(batch);
        targetApplier.applyFullLoadBatch(metadata, rows);
        copiedRows.addAndGet(rows.size());
        log.info("Copied batch of {} row(s) into {}", rows.size(), metadata.cacheKey());
        batch.clear();
    }

    private Map<String, Object> extractRow(ResultSet rs, TableMetadata metadata) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (ColumnMetadata column : metadata.getColumns()) {
            row.put(column.getName(), rs.getObject(column.getName()));
        }
        return row;
    }
}
