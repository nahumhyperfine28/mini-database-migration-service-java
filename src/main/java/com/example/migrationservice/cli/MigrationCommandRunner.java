package com.example.migrationservice.cli;

import java.util.List;
import java.util.Locale;

import com.example.migrationservice.replication.ReplicationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MigrationCommandRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrationCommandRunner.class);

    private final ReplicationEngine replicationEngine;

    public MigrationCommandRunner(ReplicationEngine replicationEngine) {
        this.replicationEngine = replicationEngine;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> commands = args.getNonOptionArgs();
        String command = commands.isEmpty() ? "run-all" : commands.get(0).toLowerCase(Locale.ROOT);

        log.info("Running migration command '{}'", command);

        switch (command) {
            case "full-load" -> replicationEngine.runFullLoad();
            case "cdc" -> replicationEngine.runCdc();
            case "run-all" -> replicationEngine.runAll();
            default -> throw new IllegalArgumentException(
                "Unknown command '%s'. Supported commands: full-load, cdc, run-all".formatted(command)
            );
        }
    }
}
