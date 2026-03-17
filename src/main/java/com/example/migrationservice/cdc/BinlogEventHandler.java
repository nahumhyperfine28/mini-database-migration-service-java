package com.example.migrationservice.cdc;

import com.example.migrationservice.checkpoint.CheckpointState;

@FunctionalInterface
public interface BinlogEventHandler {

    void handle(ReplicationEvent event, CheckpointState checkpointState);
}
