package com.example.migrationservice.checkpoint;

import java.nio.file.Path;
import java.time.Instant;

import com.example.migrationservice.config.MigrationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class CheckpointManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsCheckpointState() {
        MigrationProperties properties = new MigrationProperties();
        properties.getCheckpoint().setFile(tempDir.resolve("checkpoint.json").toString());

        CheckpointManager checkpointManager = new CheckpointManager(new ObjectMapper().findAndRegisterModules(), properties);
        CheckpointState checkpointState = new CheckpointState("mysql-bin.000003", 4567L, Instant.parse("2026-03-16T12:00:00Z"));

        checkpointManager.save(checkpointState);
        CheckpointState loadedState = checkpointManager.load().orElseThrow();

        assertThat(loadedState.getBinlogFilename()).isEqualTo("mysql-bin.000003");
        assertThat(loadedState.getBinlogPosition()).isEqualTo(4567L);
        assertThat(loadedState.getUpdatedAt()).isNotNull();
        assertThat(checkpointManager.getCheckpointPath()).exists();
    }
}
