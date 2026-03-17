package com.example.migrationservice.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {

    private Source source = new Source();
    private Target target = new Target();
    private FullLoad fullLoad = new FullLoad();
    private Cdc cdc = new Cdc();
    private Checkpoint checkpoint = new Checkpoint();
    private Retry retry = new Retry();
    private Tables tables = new Tables();

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public FullLoad getFullLoad() {
        return fullLoad;
    }

    public void setFullLoad(FullLoad fullLoad) {
        this.fullLoad = fullLoad;
    }

    public Cdc getCdc() {
        return cdc;
    }

    public void setCdc(Cdc cdc) {
        this.cdc = cdc;
    }

    public Checkpoint getCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(Checkpoint checkpoint) {
        this.checkpoint = checkpoint;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public Tables getTables() {
        return tables;
    }

    public void setTables(Tables tables) {
        this.tables = tables;
    }

    public static class Source {

        private String host = "localhost";
        private int port = 3306;
        private String database = "source_db";
        private String schema = "source_db";
        private String username = "migration_user";
        private String password = "migration_password";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String jdbcUrl() {
            return "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC".formatted(
                host, port, database
            );
        }
    }

    public static class Target {

        private String host = "localhost";
        private int port = 5432;
        private String database = "target_db";
        private String schema = "public";
        private String username = "migration_user";
        private String password = "migration_password";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String jdbcUrl() {
            return "jdbc:postgresql://%s:%d/%s?reWriteBatchedInserts=true".formatted(host, port, database);
        }
    }

    public static class FullLoad {

        private int batchSize = 200;

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class Cdc {

        private long serverId = 5401L;
        private long keepaliveIntervalMs = 1000L;
        private long connectTimeoutMs = 5000L;

        public long getServerId() {
            return serverId;
        }

        public void setServerId(long serverId) {
            this.serverId = serverId;
        }

        public long getKeepaliveIntervalMs() {
            return keepaliveIntervalMs;
        }

        public void setKeepaliveIntervalMs(long keepaliveIntervalMs) {
            this.keepaliveIntervalMs = keepaliveIntervalMs;
        }

        public long getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(long connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }
    }

    public static class Checkpoint {

        private String file = "checkpoint/checkpoint.json";

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }
    }

    public static class Retry {

        private int maxAttempts = 3;
        private long delayMs = 500L;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getDelayMs() {
            return delayMs;
        }

        public void setDelayMs(long delayMs) {
            this.delayMs = delayMs;
        }
    }

    public static class Tables {

        private List<String> include = new ArrayList<>();

        public List<String> getInclude() {
            return include;
        }

        public void setInclude(List<String> include) {
            this.include = include;
        }
    }
}
