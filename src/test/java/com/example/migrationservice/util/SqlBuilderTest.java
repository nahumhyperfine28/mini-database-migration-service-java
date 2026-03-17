package com.example.migrationservice.util;

import java.util.List;

import com.example.migrationservice.schema.ColumnMetadata;
import com.example.migrationservice.schema.TableMetadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlBuilderTest {

    @Test
    void buildsInsertSql() {
        String sql = SqlBuilder.buildInsertSql(sampleMetadata(), "public");

        assertThat(sql).isEqualTo(
            "INSERT INTO \"public\".\"customers\" (\"customer_id\", \"full_name\", \"loyalty_points\") VALUES (?, ?, ?)"
        );
    }

    @Test
    void buildsUpdateSql() {
        String sql = SqlBuilder.buildUpdateSql(sampleMetadata(), "public");

        assertThat(sql).isEqualTo(
            "UPDATE \"public\".\"customers\" SET \"full_name\" = ?, \"loyalty_points\" = ? WHERE \"customer_id\" = ?"
        );
    }

    @Test
    void buildsDeleteSql() {
        String sql = SqlBuilder.buildDeleteSql(sampleMetadata(), "public");

        assertThat(sql).isEqualTo("DELETE FROM \"public\".\"customers\" WHERE \"customer_id\" = ?");
    }

    private TableMetadata sampleMetadata() {
        return new TableMetadata(
            "source_db",
            "customers",
            List.of(
                new ColumnMetadata("customer_id", "bigint", "bigint", false, 1, false, null, 19, 0),
                new ColumnMetadata("full_name", "varchar", "varchar(150)", false, 2, false, 150L, null, null),
                new ColumnMetadata("loyalty_points", "int", "int", false, 3, false, null, 10, 0)
            ),
            List.of("customer_id")
        );
    }
}
