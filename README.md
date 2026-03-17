# mini-database-migration-service-java

A Spring Boot backend project that replicates data from MySQL to PostgreSQL with a full-load snapshot, MySQL binlog CDC, and checkpoint-based recovery.

## Project Overview

This repository is a local database replication service built for backend portfolio and interview demos. It reads from a MySQL source database, performs an initial snapshot copy into PostgreSQL, and then keeps the target in sync by consuming row-based MySQL binlog events.

In 30 seconds, the repo shows:

- full load vs incremental replication
- binlog-based CDC
- ordered event processing
- checkpoint recovery after restart
- pragmatic modular Java service design

## Why This Project Exists

Real migration systems rarely stop at a one-time copy. They usually need to:

1. copy the existing dataset safely
2. continue replicating changes after the snapshot
3. recover from restarts without reprocessing everything

This project exists to demonstrate those ideas clearly in a self-contained Java codebase that is easy to run on a laptop and easy to explain in interviews.

## Architecture Diagram

```text
                        +------------------------+
                        |     MySQL Source DB    |
                        | schema + seed data     |
                        | row-based binlog       |
                        +-----------+------------+
                                    |
                         JDBC snapshot / metadata
                                    |
                  +-----------------v-----------------+
                  |       SchemaDiscoveryService      |
                  +-----------------+-----------------+
                                    |
                                    v
                  +-----------------+-----------------+
                  |         FullLoadService           |
                  |           TableCopier             |
                  +-----------------+-----------------+
                                    |
                         batched idempotent writes
                                    |
                                    v
                        +------------------------+
                        |  PostgreSQL Target DB  |
                        | auto-created tables    |
                        +------------------------+

                                    ^
                                    |
                  +-----------------+-----------------+
                  |          TargetApplier            |
                  | retry + SQL generation            |
                  +-----------------+-----------------+
                                    ^
                                    |
                  +-----------------+-----------------+
                  |         EventTransformer          |
                  +-----------------+-----------------+
                                    ^
                                    |
                  +-----------------+-----------------+
                  |          BinlogCdcReader          |
                  | ordered binlog event stream       |
                  +-----------------+-----------------+
                                    |
                                    v
                        +------------------------+
                        | checkpoint.json        |
                        | file + position        |
                        +------------------------+
```

## Key Features

- Initial full-load snapshot from MySQL into PostgreSQL
- Row-based MySQL binlog CDC for `INSERT`, `UPDATE`, and `DELETE`
- Checkpoint persistence and restart recovery
- Ordered event application based on binlog sequence
- Automatic target table creation from source schema metadata
- Idempotent target writes with PostgreSQL upsert behavior
- Retry handling for transient target write failures
- Docker Compose environment for easy local demos
- Spring Boot CLI commands for `full-load`, `cdc`, and `run-all`

## Tech Stack

- Java 17
- Maven
- Spring Boot
- Spring JDBC
- MySQL JDBC driver
- PostgreSQL JDBC driver
- `mysql-binlog-connector-java`
- Jackson
- SLF4J + Logback
- Docker Compose

## Project Structure

```text
.
├── .env.example
├── .github/
├── checkpoint/
├── docker-compose.yml
├── pom.xml
├── README.md
├── scripts/
│   ├── reset_demo.sh
│   └── run_demo.sh
├── sql/
│   ├── mysql_init.sql
│   ├── postgres_init.sql
│   └── seed.sql
└── src/
    ├── main/
    │   ├── java/com/example/migrationservice/
    │   │   ├── cdc/
    │   │   ├── checkpoint/
    │   │   ├── cli/
    │   │   ├── config/
    │   │   ├── fullload/
    │   │   ├── replication/
    │   │   ├── schema/
    │   │   └── util/
    │   └── resources/
    └── test/java/com/example/migrationservice/
```

## How Full Load Works

1. Discover tables from the configured MySQL schema.
2. Read column metadata and primary key metadata from `INFORMATION_SCHEMA`.
3. Create matching PostgreSQL tables if they do not exist.
4. Stream rows from MySQL through JDBC.
5. Copy rows to PostgreSQL in batches.
6. Use upsert semantics so reruns are safe for the demo.

For `run-all`, the service captures the current MySQL binlog position before the snapshot begins. That snapshot boundary is used as the CDC starting point after the full load completes.

## How CDC Works

1. Open a binlog stream against the MySQL source.
2. Read ordered row events from the binlog.
3. Convert raw binlog payloads into `ReplicationEvent` objects.
4. Apply those events to PostgreSQL:
   - `INSERT` -> upsert
   - `UPDATE` -> primary-key update, with upsert fallback
   - `DELETE` -> primary-key delete
5. Persist the next binlog position after a successful apply.

The pipeline is intentionally single-threaded so event ordering stays simple and easy to reason about.

## How Checkpoint Recovery Works

Checkpoint state is stored in:

- `checkpoint/checkpoint.json`

The file records:

- current binlog filename
- current binlog position
- last update timestamp

Recovery behavior:

- `cdc` resumes from the checkpoint if it exists
- `run-all` resumes CDC from the checkpoint if it exists
- if no checkpoint exists, the service captures the current source binlog position and starts from there
- the checkpoint only moves forward after a target write succeeds

## Setup Instructions

### Prerequisites

- Java 17
- Maven 3.9+
- Docker Desktop or Docker Engine with Compose support

### Default Local Ports

To avoid clashing with local database installs, the demo uses:

- MySQL: `localhost:3307`
- PostgreSQL: `localhost:5433`

The Docker setup uses MySQL `8.0` because it works cleanly with the chosen binlog connector in this local demo.

### Build and Test

```bash
mvn test
```

### Reset the Local Environment

```bash
./scripts/reset_demo.sh
```

### Run the Service

Full load only:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=full-load
```

CDC only:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=cdc
```

Full pipeline:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=run-all
```

Demo script:

```bash
./scripts/run_demo.sh run-all
```

## Step-by-Step Demo Instructions

### 1. Start Docker Containers

```bash
./scripts/reset_demo.sh
```

### 2. Run the Migration Service

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=run-all
```

Expected result:

- target tables are created automatically
- existing MySQL rows are copied to PostgreSQL
- the service stays running and starts listening to the MySQL binlog

### 3. Make Changes in MySQL

Open a second terminal and run the SQL commands in the next section.

### 4. Verify Replication in PostgreSQL

Use the verification commands in the demo SQL section below.

## Example SQL Commands For Testing Replication

### Write Changes in MySQL

```bash
docker exec -i mini-migration-mysql mysql -uroot -prootpassword source_db <<'SQL'
INSERT INTO customers (email, full_name, status, loyalty_points)
VALUES ('demo.user@example.com', 'Demo User', 'ACTIVE', 25);

UPDATE products
SET price = 44.99
WHERE product_id = 102;

DELETE FROM inventory
WHERE inventory_id = 5003;
SQL
```

### Verify Changes in PostgreSQL

```bash
docker exec -i mini-migration-postgres psql -U migration_user -d target_db -c "SELECT customer_id, email, loyalty_points FROM public.customers ORDER BY customer_id;"
docker exec -i mini-migration-postgres psql -U migration_user -d target_db -c "SELECT product_id, price, active FROM public.products ORDER BY product_id;"
docker exec -i mini-migration-postgres psql -U migration_user -d target_db -c "SELECT inventory_id, product_id, warehouse_code FROM public.inventory ORDER BY inventory_id;"
```

You should see:

- the inserted `customers` row in PostgreSQL
- the updated `products.price`
- the deleted `inventory` row removed from PostgreSQL

### Demonstrate Checkpoint Recovery

1. Stop the running Spring Boot process with `Ctrl+C`.
2. Make another MySQL change.
3. Restart the service with:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=cdc
```

4. The service resumes from `checkpoint/checkpoint.json` and applies the missed change.

## Configuration

Main configuration lives in:

- `src/main/resources/application.yml`
- environment variables from `.env.example`

Default local values:

- MySQL host: `localhost`
- MySQL port: `3307`
- MySQL database/schema: `source_db`
- PostgreSQL host: `localhost`
- PostgreSQL port: `5433`
- PostgreSQL database: `target_db`
- PostgreSQL schema: `public`
- checkpoint file: `checkpoint/checkpoint.json`

## Current Limitations

- Focused on a single MySQL source schema and a single PostgreSQL target schema.
- Assumes row-based MySQL binlog events with full row images.
- Supports the common column types used in the demo schema, not the full MySQL type system.
- Does not replicate DDL changes or schema drift.
- Does not preserve source indexes, foreign keys, or default expressions on the target.
- Primary key updates are not modeled as delete-plus-insert.
- Snapshot consistency is pragmatic rather than production-grade.
- CDC processing is intentionally single-threaded to keep correctness and ordering easy to explain.

## Interview Talking Points

- Full load vs incremental replication:
  This repo cleanly separates the snapshot problem from the continuous replication problem.
- Binlog event handling:
  CDC is driven by ordered MySQL row events, not periodic polling.
- Event ordering:
  Changes are applied in binlog order, which keeps correctness straightforward.
- Checkpoint recovery:
  The service stores binlog file and position locally and resumes from a known boundary.
- Idempotent writes:
  Inserts use upsert semantics and updates can fall back to upsert when needed.
- Modular backend design:
  Schema discovery, full load, CDC reading, event transformation, target apply, and checkpointing are separated into focused services.
- Failure handling:
  Transient target-side failures retry; unrecoverable errors fail loudly.

## Future Improvements

- DDL change handling
- richer type mapping and compatibility validation
- target index and constraint replication
- metrics and health endpoints
- dead-letter handling for poison events
- configurable table filtering and richer selection rules
- stronger snapshot consistency for more demanding production scenarios
