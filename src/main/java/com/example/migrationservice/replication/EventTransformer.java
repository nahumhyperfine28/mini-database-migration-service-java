package com.example.migrationservice.replication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.migrationservice.cdc.ReplicationEvent;
import com.example.migrationservice.schema.ColumnMetadata;
import com.example.migrationservice.schema.TableMetadata;
import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData;
import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import org.springframework.stereotype.Component;

@Component
public class EventTransformer {

    public List<ReplicationEvent> transform(TableMetadata metadata, EventData eventData) {
        if (eventData instanceof WriteRowsEventData writeRows) {
            return transformWriteRows(metadata, writeRows.getRows());
        }
        if (eventData instanceof UpdateRowsEventData updateRows) {
            return transformUpdateRows(metadata, updateRows.getRows());
        }
        if (eventData instanceof DeleteRowsEventData deleteRows) {
            return transformDeleteRows(metadata, deleteRows.getRows());
        }
        return List.of();
    }

    public List<ReplicationEvent> transformWriteRows(TableMetadata metadata, List<Serializable[]> rows) {
        return rows.stream()
            .map(row -> ReplicationEvent.insert(metadata, toRowMap(metadata, row)))
            .toList();
    }

    public List<ReplicationEvent> transformUpdateRows(
        TableMetadata metadata,
        List<Map.Entry<Serializable[], Serializable[]>> rows
    ) {
        List<ReplicationEvent> events = new ArrayList<>(rows.size());
        for (Map.Entry<Serializable[], Serializable[]> row : rows) {
            events.add(ReplicationEvent.update(
                metadata,
                toRowMap(metadata, row.getKey()),
                toRowMap(metadata, row.getValue())
            ));
        }
        return events;
    }

    public List<ReplicationEvent> transformDeleteRows(TableMetadata metadata, List<Serializable[]> rows) {
        return rows.stream()
            .map(row -> ReplicationEvent.delete(metadata, toRowMap(metadata, row)))
            .toList();
    }

    private Map<String, Object> toRowMap(TableMetadata metadata, Serializable[] values) {
        if (values.length != metadata.getColumns().size()) {
            throw new IllegalStateException(
                "Row image size mismatch for table %s. Expected %d columns but received %d".formatted(
                    metadata.cacheKey(),
                    metadata.getColumns().size(),
                    values.length
                )
            );
        }

        Map<String, Object> row = new LinkedHashMap<>();
        List<ColumnMetadata> columns = metadata.getColumns();
        for (int index = 0; index < columns.size(); index++) {
            row.put(columns.get(index).getName(), values[index]);
        }
        return row;
    }
}
