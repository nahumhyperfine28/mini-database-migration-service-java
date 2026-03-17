package com.example.migrationservice.replication;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.example.migrationservice.cdc.ReplicationEvent;
import com.example.migrationservice.cdc.ReplicationEventType;
import com.example.migrationservice.schema.ColumnMetadata;
import com.example.migrationservice.schema.TableMetadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventTransformerTest {

    private final EventTransformer eventTransformer = new EventTransformer();

    @Test
    void transformsInsertRowsIntoReplicationEvents() {
        TableMetadata tableMetadata = sampleMetadata();

        List<ReplicationEvent> events = eventTransformer.transformWriteRows(
            tableMetadata,
            List.<Serializable[]>of(new Serializable[]{1L, "Ava Lee", 120})
        );

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getType()).isEqualTo(ReplicationEventType.INSERT);
        assertThat(events.get(0).getAfterValues())
            .containsEntry("customer_id", 1L)
            .containsEntry("full_name", "Ava Lee")
            .containsEntry("loyalty_points", 120);
    }

    @Test
    void transformsUpdateRowsIntoReplicationEvents() {
        TableMetadata tableMetadata = sampleMetadata();

        List<ReplicationEvent> events = eventTransformer.transformUpdateRows(
            tableMetadata,
            List.of(Map.entry(
                new Serializable[]{1L, "Ava Lee", 120},
                new Serializable[]{1L, "Ava Lee", 150}
            ))
        );

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getType()).isEqualTo(ReplicationEventType.UPDATE);
        assertThat(events.get(0).getBeforeValues()).containsEntry("loyalty_points", 120);
        assertThat(events.get(0).getAfterValues()).containsEntry("loyalty_points", 150);
    }

    @Test
    void transformsDeleteRowsIntoReplicationEvents() {
        TableMetadata tableMetadata = sampleMetadata();

        List<ReplicationEvent> events = eventTransformer.transformDeleteRows(
            tableMetadata,
            List.<Serializable[]>of(new Serializable[]{1L, "Ava Lee", 120})
        );

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getType()).isEqualTo(ReplicationEventType.DELETE);
        assertThat(events.get(0).getBeforeValues()).containsEntry("customer_id", 1L);
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
