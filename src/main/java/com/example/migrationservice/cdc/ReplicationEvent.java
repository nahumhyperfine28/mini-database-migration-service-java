package com.example.migrationservice.cdc;

import java.util.Map;

import com.example.migrationservice.schema.TableMetadata;

public class ReplicationEvent {

    private final ReplicationEventType type;
    private final TableMetadata tableMetadata;
    private final Map<String, Object> beforeValues;
    private final Map<String, Object> afterValues;

    private ReplicationEvent(
        ReplicationEventType type,
        TableMetadata tableMetadata,
        Map<String, Object> beforeValues,
        Map<String, Object> afterValues
    ) {
        this.type = type;
        this.tableMetadata = tableMetadata;
        this.beforeValues = beforeValues;
        this.afterValues = afterValues;
    }

    public static ReplicationEvent insert(TableMetadata tableMetadata, Map<String, Object> afterValues) {
        return new ReplicationEvent(ReplicationEventType.INSERT, tableMetadata, Map.of(), afterValues);
    }

    public static ReplicationEvent update(
        TableMetadata tableMetadata,
        Map<String, Object> beforeValues,
        Map<String, Object> afterValues
    ) {
        return new ReplicationEvent(ReplicationEventType.UPDATE, tableMetadata, beforeValues, afterValues);
    }

    public static ReplicationEvent delete(TableMetadata tableMetadata, Map<String, Object> beforeValues) {
        return new ReplicationEvent(ReplicationEventType.DELETE, tableMetadata, beforeValues, Map.of());
    }

    public ReplicationEventType getType() {
        return type;
    }

    public TableMetadata getTableMetadata() {
        return tableMetadata;
    }

    public Map<String, Object> getBeforeValues() {
        return beforeValues;
    }

    public Map<String, Object> getAfterValues() {
        return afterValues;
    }
}
