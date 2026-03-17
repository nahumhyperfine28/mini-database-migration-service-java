package com.example.migrationservice.replication;

import java.util.Optional;

import com.example.migrationservice.cdc.BinlogCdcReader;
import com.example.migrationservice.checkpoint.CheckpointManager;
import com.example.migrationservice.checkpoint.CheckpointState;
import com.example.migrationservice.fullload.FullLoadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Coordinates the three operator-facing execution modes:
 * full load only, CDC only, and full pipeline.
 */
@Service
public class ReplicationEngine {

    private static final Logger log = LoggerFactory.getLogger(ReplicationEngine.class);

    private final FullLoadService fullLoadService;
    private final BinlogCdcReader binlogCdcReader;
    private final TargetApplier targetApplier;
    private final CheckpointManager checkpointManager;

    public ReplicationEngine(
        FullLoadService fullLoadService,
        BinlogCdcReader binlogCdcReader,
        TargetApplier targetApplier,
        CheckpointManager checkpointManager
    ) {
        this.fullLoadService = fullLoadService;
        this.binlogCdcReader = binlogCdcReader;
        this.targetApplier = targetApplier;
        this.checkpointManager = checkpointManager;
    }

    public void runFullLoad() {
        fullLoadService.runFullLoad();
    }

    public void runCdc() {
        CheckpointState startState = checkpointManager.load().orElseGet(() -> {
            CheckpointState current = binlogCdcReader.captureCurrentCheckpoint();
            log.info("No checkpoint found, starting CDC from current source binlog position {}", current);
            return current;
        });
        startCdcFrom(startState);
    }

    public void runAll() {
        Optional<CheckpointState> checkpoint = checkpointManager.load();
        if (checkpoint.isPresent()) {
            log.info("Existing checkpoint found, resuming CDC from {}", checkpoint.get());
            startCdcFrom(checkpoint.get());
            return;
        }

        // Capture the binlog boundary before the snapshot begins so CDC can replay
        // changes that happen while the full-load copy is in progress.
        CheckpointState snapshotStart = binlogCdcReader.captureCurrentCheckpoint();
        log.info("Captured snapshot start checkpoint {}", snapshotStart);
        fullLoadService.runFullLoad();
        checkpointManager.save(snapshotStart);
        startCdcFrom(snapshotStart);
    }

    private void startCdcFrom(CheckpointState checkpointState) {
        binlogCdcReader.start(checkpointState, (event, updatedCheckpoint) -> {
            targetApplier.applyEvent(event);
            // The checkpoint only moves forward after the target write succeeds.
            checkpointManager.save(updatedCheckpoint);
        });
    }
}
