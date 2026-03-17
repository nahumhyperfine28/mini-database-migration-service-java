package com.example.migrationservice.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.example.migrationservice.config.MigrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SchemaDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(SchemaDiscoveryService.class);

    private final JdbcTemplate sourceJdbcTemplate;
    private final MigrationProperties properties;
    private final Map<String, TableMetadata> metadataCache = new ConcurrentHashMap<>();

    public SchemaDiscoveryService(
        @Qualifier("sourceJdbcTemplate") JdbcTemplate sourceJdbcTemplate,
        MigrationProperties properties
    ) {
        this.sourceJdbcTemplate = sourceJdbcTemplate;
        this.properties = properties;
    }

    public List<TableMetadata> discoverTables() {
        String schemaName = properties.getSource().getSchema();
        List<String> tables = sourceJdbcTemplate.queryForList("""
            SELECT TABLE_NAME
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = ?
              AND TABLE_TYPE = 'BASE TABLE'
            ORDER BY TABLE_NAME
            """, String.class, schemaName);

        Set<String> includedTables = Set.copyOf(properties.getTables().getInclude());
        List<String> filteredTables = includedTables.isEmpty()
            ? tables
            : tables.stream().filter(includedTables::contains).toList();

        List<TableMetadata> metadata = filteredTables.stream()
            .map(this::discoverTable)
            .toList();

        log.info("Discovered {} table(s) from source schema {}", metadata.size(), schemaName);
        return metadata;
    }

    public Optional<TableMetadata> resolveTable(String schemaName, String tableName) {
        if (!properties.getSource().getSchema().equalsIgnoreCase(schemaName)) {
            return Optional.empty();
        }
        return Optional.of(discoverTable(tableName));
    }

    public TableMetadata discoverTable(String tableName) {
        String cacheKey = properties.getSource().getSchema() + "." + tableName;
        return metadataCache.computeIfAbsent(cacheKey, key -> loadTableMetadata(tableName));
    }

    private TableMetadata loadTableMetadata(String tableName) {
        String schemaName = properties.getSource().getSchema();
        List<ColumnMetadata> columns = sourceJdbcTemplate.query("""
            SELECT COLUMN_NAME,
                   DATA_TYPE,
                   COLUMN_TYPE,
                   IS_NULLABLE,
                   ORDINAL_POSITION,
                   EXTRA,
                   CHARACTER_MAXIMUM_LENGTH,
                   NUMERIC_PRECISION,
                   NUMERIC_SCALE
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = ?
              AND TABLE_NAME = ?
            ORDER BY ORDINAL_POSITION
            """,
            (rs, rowNum) -> new ColumnMetadata(
                rs.getString("COLUMN_NAME"),
                rs.getString("DATA_TYPE"),
                rs.getString("COLUMN_TYPE"),
                "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")),
                rs.getInt("ORDINAL_POSITION"),
                rs.getString("EXTRA") != null && rs.getString("EXTRA").contains("auto_increment"),
                getNullableLong(rs.getObject("CHARACTER_MAXIMUM_LENGTH")),
                getNullableInteger(rs.getObject("NUMERIC_PRECISION")),
                getNullableInteger(rs.getObject("NUMERIC_SCALE"))
            ),
            schemaName,
            tableName
        );

        List<String> primaryKeys = sourceJdbcTemplate.query("""
            SELECT kcu.COLUMN_NAME
            FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
            JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
              ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
             AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA
             AND tc.TABLE_NAME = kcu.TABLE_NAME
            WHERE tc.TABLE_SCHEMA = ?
              AND tc.TABLE_NAME = ?
              AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
            ORDER BY kcu.ORDINAL_POSITION
            """, (rs, rowNum) -> rs.getString("COLUMN_NAME"), schemaName, tableName);

        if (columns.isEmpty()) {
            throw new IllegalStateException("No columns found for source table " + schemaName + "." + tableName);
        }
        if (primaryKeys.isEmpty()) {
            throw new IllegalStateException("Source table " + schemaName + "." + tableName + " does not have a primary key");
        }

        TableMetadata metadata = new TableMetadata(schemaName, tableName, new ArrayList<>(columns), primaryKeys);
        log.info("Loaded metadata for {}.{} with {} column(s)", schemaName, tableName, columns.size());
        return metadata;
    }

    private Long getNullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private Integer getNullableInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }
}
