package com.example.migrationservice.schema;

public class ColumnMetadata {

    private final String name;
    private final String dataType;
    private final String columnType;
    private final boolean nullable;
    private final int ordinalPosition;
    private final boolean autoIncrement;
    private final Long characterMaximumLength;
    private final Integer numericPrecision;
    private final Integer numericScale;

    public ColumnMetadata(
        String name,
        String dataType,
        String columnType,
        boolean nullable,
        int ordinalPosition,
        boolean autoIncrement,
        Long characterMaximumLength,
        Integer numericPrecision,
        Integer numericScale
    ) {
        this.name = name;
        this.dataType = dataType;
        this.columnType = columnType;
        this.nullable = nullable;
        this.ordinalPosition = ordinalPosition;
        this.autoIncrement = autoIncrement;
        this.characterMaximumLength = characterMaximumLength;
        this.numericPrecision = numericPrecision;
        this.numericScale = numericScale;
    }

    public String getName() {
        return name;
    }

    public String getDataType() {
        return dataType;
    }

    public String getColumnType() {
        return columnType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public Long getCharacterMaximumLength() {
        return characterMaximumLength;
    }

    public Integer getNumericPrecision() {
        return numericPrecision;
    }

    public Integer getNumericScale() {
        return numericScale;
    }
}
