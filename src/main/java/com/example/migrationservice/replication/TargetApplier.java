package com.example.migrationservice.replication;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.example.migrationservice.cdc.ReplicationEvent;
import com.example.migrationservice.config.MigrationProperties;
import com.example.migrationservice.schema.ColumnMetadata;
import com.example.migrationservice.schema.TableMetadata;
import com.example.migrationservice.util.RetryUtils;
import com.example.migrationservice.util.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TargetApplier {

    private static final Logger log = LoggerFactory.getLogger(TargetApplier.class);

    private final JdbcTemplate targetJdbcTemplate;
    private final MigrationProperties properties;
    private final Set<String> ensuredTables = ConcurrentHashMap.newKeySet();

    public TargetApplier(
        @Qualifier("targetJdbcTemplate") JdbcTemplate targetJdbcTemplate,
        MigrationProperties properties
    ) {
        this.targetJdbcTemplate = targetJdbcTemplate;
        this.properties = properties;
    }

    public void ensureTable(TableMetadata metadata) {
        if (ensuredTables.add(metadata.getTableName())) {
            targetJdbcTemplate.execute(SqlBuilder.buildCreateTableSql(metadata, properties.getTarget().getSchema()));
            log.info("Ensured target table exists for {}", metadata.cacheKey());
        }
    }

    public void applyFullLoadBatch(TableMetadata metadata, List<Map<String, Object>> rows) {
        ensureTable(metadata);
        String sql = SqlBuilder.buildUpsertSql(metadata, properties.getTarget().getSchema());

        RetryUtils.runWithRetry(
            "full load batch for " + metadata.cacheKey(),
            properties.getRetry().getMaxAttempts(),
            Duration.ofMillis(properties.getRetry().getDelayMs()),
            () -> {
                targetJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int index) throws SQLException {
                        bindRow(ps, metadata, rows.get(index));
                    }

                    @Override
                    public int getBatchSize() {
                        return rows.size();
                    }
                });
            }
        );
    }

    public void applyEvent(ReplicationEvent event) {
        ensureTable(event.getTableMetadata());
        RetryUtils.runWithRetry(
            "cdc event " + event.getType() + " for " + event.getTableMetadata().cacheKey(),
            properties.getRetry().getMaxAttempts(),
            Duration.ofMillis(properties.getRetry().getDelayMs()),
            () -> applyEventInternal(event)
        );
    }

    private void applyEventInternal(ReplicationEvent event) {
        TableMetadata metadata = event.getTableMetadata();
        switch (event.getType()) {
            case INSERT -> {
                targetJdbcTemplate.update(
                    SqlBuilder.buildUpsertSql(metadata, properties.getTarget().getSchema()),
                    valuesForColumns(metadata, event.getAfterValues())
                );
                log.info("Applied INSERT to {} with key {}", metadata.cacheKey(), primaryKeySummary(metadata, event.getAfterValues()));
            }
            case UPDATE -> {
                int updated = targetJdbcTemplate.update(
                    SqlBuilder.buildUpdateSql(metadata, properties.getTarget().getSchema()),
                    Stream.concat(
                        metadata.getNonPrimaryKeyColumns().stream()
                            .map(column -> normalizeValue(column, event.getAfterValues().get(column.getName()))),
                        metadata.getPrimaryKeyColumns().stream()
                            .map(columnName -> normalizeValue(metadata.getColumn(columnName), event.getBeforeValues().get(columnName)))
                    ).toArray()
                );

                if (updated == 0) {
                    targetJdbcTemplate.update(
                        SqlBuilder.buildUpsertSql(metadata, properties.getTarget().getSchema()),
                        valuesForColumns(metadata, event.getAfterValues())
                    );
                }
                log.info("Applied UPDATE to {} with key {}", metadata.cacheKey(), primaryKeySummary(metadata, event.getAfterValues()));
            }
            case DELETE -> {
                targetJdbcTemplate.update(
                    SqlBuilder.buildDeleteSql(metadata, properties.getTarget().getSchema()),
                    metadata.getPrimaryKeyColumns().stream()
                        .map(columnName -> normalizeValue(metadata.getColumn(columnName), event.getBeforeValues().get(columnName)))
                        .toArray()
                );
                log.info("Applied DELETE to {} with key {}", metadata.cacheKey(), primaryKeySummary(metadata, event.getBeforeValues()));
            }
        }
    }

    private void bindRow(PreparedStatement ps, TableMetadata metadata, Map<String, Object> row) throws SQLException {
        int parameterIndex = 1;
        for (ColumnMetadata column : metadata.getColumns()) {
            ps.setObject(parameterIndex++, normalizeValue(column, row.get(column.getName())));
        }
    }

    private Object[] valuesForColumns(TableMetadata metadata, Map<String, Object> row) {
        return metadata.getColumns().stream()
            .map(column -> normalizeValue(column, row.get(column.getName())))
            .toArray();
    }

    private Object normalizeValue(ColumnMetadata column, Object value) {
        if (value == null) {
            return null;
        }
        if (isBooleanColumn(column)) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value instanceof Number number) {
                return number.intValue() != 0;
            }
            if (value instanceof byte[] bytes && bytes.length > 0) {
                return bytes[0] != 0;
            }
            if (value instanceof String stringValue) {
                return "1".equals(stringValue) || Boolean.parseBoolean(stringValue);
            }
        }
        if (value instanceof Date date && !(value instanceof java.sql.Date) && !(value instanceof Timestamp)) {
            return new Timestamp(date.getTime());
        }
        return value;
    }

    private boolean isBooleanColumn(ColumnMetadata column) {
        return "bit".equalsIgnoreCase(column.getDataType()) ||
            ("tinyint".equalsIgnoreCase(column.getDataType()) && column.getColumnType().startsWith("tinyint(1)"));
    }

    private String primaryKeySummary(TableMetadata metadata, Map<String, Object> row) {
        return metadata.getPrimaryKeyColumns().stream()
            .map(column -> column + "=" + row.get(column))
            .reduce((left, right) -> left + ", " + right)
            .orElse("n/a");
    }
}
