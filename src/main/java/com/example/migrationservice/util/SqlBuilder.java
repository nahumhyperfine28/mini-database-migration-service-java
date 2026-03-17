package com.example.migrationservice.util;

import java.util.List;
import java.util.stream.Collectors;

import com.example.migrationservice.schema.ColumnMetadata;
import com.example.migrationservice.schema.TableMetadata;

public final class SqlBuilder {

    private SqlBuilder() {
    }

    public static String buildSelectAllSql(TableMetadata metadata) {
        return "SELECT %s FROM %s".formatted(
            joinMySqlQuotedIdentifiers(metadata.getColumnNames()),
            qualifyMySqlName(metadata.getSchemaName(), metadata.getTableName())
        );
    }

    public static String buildCreateTableSql(TableMetadata metadata, String targetSchema) {
        List<String> columnDefinitions = metadata.getColumns().stream()
            .map(column -> "%s %s%s".formatted(
                quote(column.getName()),
                mapToPostgresType(column),
                column.isNullable() ? "" : " NOT NULL"
            ))
            .toList();

        String primaryKey = metadata.getPrimaryKeyColumns().stream()
            .map(SqlBuilder::quote)
            .collect(Collectors.joining(", "));

        return """
            CREATE TABLE IF NOT EXISTS %s (
                %s,
                PRIMARY KEY (%s)
            )
            """.formatted(
            qualifyName(targetSchema, metadata.getTableName()),
            String.join(",\n    ", columnDefinitions),
            primaryKey
        ).trim();
    }

    public static String buildInsertSql(TableMetadata metadata, String targetSchema) {
        String columns = joinQuotedIdentifiers(metadata.getColumnNames());
        String placeholders = placeholders(metadata.getColumns().size());
        return "INSERT INTO %s (%s) VALUES (%s)".formatted(
            qualifyName(targetSchema, metadata.getTableName()),
            columns,
            placeholders
        );
    }

    public static String buildUpsertSql(TableMetadata metadata, String targetSchema) {
        String insertSql = buildInsertSql(metadata, targetSchema);
        String updateClause = metadata.getNonPrimaryKeyColumns().stream()
            .map(column -> "%s = EXCLUDED.%s".formatted(quote(column.getName()), quote(column.getName())))
            .collect(Collectors.joining(", "));

        if (updateClause.isBlank()) {
            return insertSql + " ON CONFLICT (" + joinQuotedIdentifiers(metadata.getPrimaryKeyColumns()) + ") DO NOTHING";
        }

        return insertSql + " ON CONFLICT (" + joinQuotedIdentifiers(metadata.getPrimaryKeyColumns()) + ") DO UPDATE SET " + updateClause;
    }

    public static String buildUpdateSql(TableMetadata metadata, String targetSchema) {
        String setClause = metadata.getNonPrimaryKeyColumns().stream()
            .map(column -> "%s = ?".formatted(quote(column.getName())))
            .collect(Collectors.joining(", "));

        String whereClause = metadata.getPrimaryKeyColumns().stream()
            .map(column -> "%s = ?".formatted(quote(column)))
            .collect(Collectors.joining(" AND "));

        return "UPDATE %s SET %s WHERE %s".formatted(
            qualifyName(targetSchema, metadata.getTableName()),
            setClause,
            whereClause
        );
    }

    public static String buildDeleteSql(TableMetadata metadata, String targetSchema) {
        String whereClause = metadata.getPrimaryKeyColumns().stream()
            .map(column -> "%s = ?".formatted(quote(column)))
            .collect(Collectors.joining(" AND "));

        return "DELETE FROM %s WHERE %s".formatted(
            qualifyName(targetSchema, metadata.getTableName()),
            whereClause
        );
    }

    private static String joinQuotedIdentifiers(List<String> identifiers) {
        return identifiers.stream().map(SqlBuilder::quote).collect(Collectors.joining(", "));
    }

    private static String joinMySqlQuotedIdentifiers(List<String> identifiers) {
        return identifiers.stream().map(SqlBuilder::quoteMySql).collect(Collectors.joining(", "));
    }

    private static String placeholders(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(index -> "?")
            .collect(Collectors.joining(", "));
    }

    private static String qualifyName(String schema, String table) {
        return quote(schema) + "." + quote(table);
    }

    private static String qualifyMySqlName(String schema, String table) {
        return quoteMySql(schema) + "." + quoteMySql(table);
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String quoteMySql(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private static String mapToPostgresType(ColumnMetadata column) {
        String dataType = column.getDataType().toLowerCase();
        return switch (dataType) {
            case "bigint" -> "BIGINT";
            case "int", "integer" -> "INTEGER";
            case "smallint" -> "SMALLINT";
            case "tinyint" -> column.getColumnType().startsWith("tinyint(1)") ? "BOOLEAN" : "SMALLINT";
            case "decimal", "numeric" -> "DECIMAL(%d, %d)".formatted(
                defaultValue(column.getNumericPrecision(), 10),
                defaultValue(column.getNumericScale(), 0)
            );
            case "double" -> "DOUBLE PRECISION";
            case "float" -> "REAL";
            case "varchar" -> "VARCHAR(%d)".formatted(defaultValue(column.getCharacterMaximumLength(), 255L));
            case "char" -> "CHAR(%d)".formatted(defaultValue(column.getCharacterMaximumLength(), 1L));
            case "text", "mediumtext", "longtext", "tinytext" -> "TEXT";
            case "datetime", "timestamp" -> "TIMESTAMP";
            case "date" -> "DATE";
            case "time" -> "TIME";
            case "bit" -> "BIT";
            case "blob", "binary", "varbinary" -> "BYTEA";
            default -> "TEXT";
        };
    }

    private static int defaultValue(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static long defaultValue(Long value, long fallback) {
        return value == null ? fallback : value;
    }
}
