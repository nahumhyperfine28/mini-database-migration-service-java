package com.example.migrationservice.checkpoint;

import java.time.Instant;

public class CheckpointState {

    private String binlogFilename;
    private long binlogPosition;
    private Instant updatedAt;

    public CheckpointState() {
    }

    public CheckpointState(String binlogFilename, long binlogPosition, Instant updatedAt) {
        this.binlogFilename = binlogFilename;
        this.binlogPosition = binlogPosition;
        this.updatedAt = updatedAt;
    }

    public static CheckpointState of(String binlogFilename, long binlogPosition) {
        return new CheckpointState(binlogFilename, binlogPosition, Instant.now());
    }

    public String getBinlogFilename() {
        return binlogFilename;
    }

    public void setBinlogFilename(String binlogFilename) {
        this.binlogFilename = binlogFilename;
    }

    public long getBinlogPosition() {
        return binlogPosition;
    }

    public void setBinlogPosition(long binlogPosition) {
        this.binlogPosition = binlogPosition;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "CheckpointState{" +
            "binlogFilename='" + binlogFilename + '\'' +
            ", binlogPosition=" + binlogPosition +
            ", updatedAt=" + updatedAt +
            '}';
    }
}
