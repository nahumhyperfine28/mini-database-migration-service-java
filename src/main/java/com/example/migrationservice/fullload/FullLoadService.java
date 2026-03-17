package com.example.migrationservice.fullload;

import java.util.List;

import com.example.migrationservice.schema.SchemaDiscoveryService;
import com.example.migrationservice.schema.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Executes the initial snapshot phase by discovering source tables
 * and copying them into the target one table at a time.
 */
@Service
public class FullLoadService {

    private static final Logger log = LoggerFactory.getLogger(FullLoadService.class);

    private final SchemaDiscoveryService schemaDiscoveryService;
    private final TableCopier tableCopier;

    public FullLoadService(SchemaDiscoveryService schemaDiscoveryService, TableCopier tableCopier) {
        this.schemaDiscoveryService = schemaDiscoveryService;
        this.tableCopier = tableCopier;
    }

    public List<TableMetadata> runFullLoad() {
        List<TableMetadata> tables = schemaDiscoveryService.discoverTables();
        long totalRows = 0L;

        for (TableMetadata table : tables) {
            totalRows += tableCopier.copyTable(table);
        }

        log.info("Full load complete for {} table(s), copied {} total row(s)", tables.size(), totalRows);
        return tables;
    }
}
