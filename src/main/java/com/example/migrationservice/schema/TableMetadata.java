package com.example.migrationservice.schema;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TableMetadata {

    private final String schemaName;
    private final String tableName;
    private final List<ColumnMetadata> columns;
    private final List<String> primaryKeyColumns;
    private final Map<String, ColumnMetadata> columnsByName;

    public TableMetadata(String schemaName, String tableName, List<ColumnMetadata> columns, List<String> primaryKeyColumns) {
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.columns = List.copyOf(columns);
        this.primaryKeyColumns = List.copyOf(primaryKeyColumns);
        this.columnsByName = this.columns.stream().collect(Collectors.toMap(ColumnMetadata::getName, Function.identity()));
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public List<ColumnMetadata> getColumns() {
        return columns;
    }

    public List<String> getPrimaryKeyColumns() {
        return primaryKeyColumns;
    }

    public List<ColumnMetadata> getNonPrimaryKeyColumns() {
        return columns.stream()
            .filter(column -> !primaryKeyColumns.contains(column.getName()))
            .toList();
    }

    public List<String> getColumnNames() {
        return columns.stream().map(ColumnMetadata::getName).toList();
    }

    public ColumnMetadata getColumn(String columnName) {
        return columnsByName.get(columnName);
    }

    public String cacheKey() {
        return schemaName + "." + tableName;
    }
}
