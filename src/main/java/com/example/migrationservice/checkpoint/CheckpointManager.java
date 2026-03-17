package com.example.migrationservice.checkpoint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;

import com.example.migrationservice.config.MigrationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Persists the last successfully applied CDC position to a local JSON file
 * so the service can resume from a stable binlog boundary after restart.
 */
@Component
public class CheckpointManager {

    private static final Logger log = LoggerFactory.getLogger(CheckpointManager.class);

    private final ObjectMapper objectMapper;
    private final Path checkpointPath;

    public CheckpointManager(ObjectMapper objectMapper, MigrationProperties properties) {
        this.objectMapper = objectMapper;
        this.checkpointPath = Path.of(properties.getCheckpoint().getFile());
    }

    public synchronized Optional<CheckpointState> load() {
        if (!Files.exists(checkpointPath)) {
            return Optional.empty();
        }

        try {
            CheckpointState state = objectMapper.readValue(checkpointPath.toFile(), CheckpointState.class);
            log.info("Loaded checkpoint from {}: {}", checkpointPath, state);
            return Optional.of(state);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read checkpoint file " + checkpointPath, exception);
        }
    }

    public synchronized void save(CheckpointState state) {
        try {
            Files.createDirectories(checkpointPath.getParent());
            state.setUpdatedAt(Instant.now());
            // Write to a temp file first, then atomically replace the checkpoint.
            Path tempFile = checkpointPath.resolveSibling(checkpointPath.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), state);
            Files.move(tempFile, checkpointPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Saved checkpoint {}", state);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist checkpoint to " + checkpointPath, exception);
        }
    }

    public synchronized void clear() {
        try {
            Files.deleteIfExists(checkpointPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete checkpoint file " + checkpointPath, exception);
        }
    }

    public Path getCheckpointPath() {
        return checkpointPath;
    }
}
