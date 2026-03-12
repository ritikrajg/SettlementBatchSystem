# Settlement Batch System

This project is a console-based Java settlement engine. It captures transactions, groups them into settlement batches, stores records in PostgreSQL, and prints settlement-style reports.

---

## What The Project Is

It is a simple CLI workflow for settlement operations:

1. Initialize a batch.
2. Add transactions to the current batch.
3. Submit the batch to the database.
4. Generate batch summary and clearing-house style reports from persisted data.

The architecture follows a clean layered split:

- **App layer** for menu + user flow.
- **Service layer** for orchestration/report logic.
- **Repository layer** for SQL operations.
- **Entity + enum layer** for domain modeling.

---

## High-Level Flow

1. User starts `SettlementApp`.
2. User creates a `SettlementBatch.Builder` with batch ID + date.
3. User adds `Transaction` items (channel, amount, DR/CR, status).
4. User submits batch.
   - `SettlementBatchRepository` stores batch metadata.
   - `TransactionRepository` stores all transactions with `batch_id`.
5. User requests reports.
   - Service fetches from DB and prints summary/register outputs.

---

## Core Packages

- `com.iispl.app`
  - Entry point and menu navigation.
- `com.iispl.service`
  - Batch processing + report generation.
- `com.iispl.repository`
  - JDBC persistence and query methods.
- `com.iispl.entity`
  - Domain classes (`Transaction`, `SettlementBatch`).
- `com.iispl.enums`
  - Controlled values for channel, DR/CR, and status.
- `com.iispl.connectionpool`
  - DataSource initialization from property file.

---

## File-By-File Walkthrough (Friend-Style)

### 1) `SettlementApp.java` (Entry Point)

**Purpose:** Console UI + menu loop.

Key flow:

- Creates `Scanner` for input.
- Creates `SettlementService` with repositories.
- Maintains current in-memory `SettlementBatch.Builder`.
- Displays menu:
  1. Initialize batch.
  2. Add transaction.
  3. Submit batch.
  4. View batch summary.
  5. View clearing house report.
  6. View bank-wise settlement summary.
  7. View all transactions.
  8. View current unsaved batch.
  9. Exit.

Helper methods:

- `readMenuChoice()` validates numeric menu input.
- `readAmount()` loops until valid positive `BigDecimal`.
- `readChannel()`, `readDrCr()`, `readStatus()` validate enum input.
- Startup checks verify DB connectivity and required tables before menu launch.
- Confirmation prompts prevent accidental submit/exit with unsaved records.

Behavior note:

- Transactions remain in-memory in the builder until user selects **Submit**.

### 2) `SettlementService.java` (Business Logic Layer)

**Purpose:** Orchestrates repositories and prints reports.

Main methods:

- `processBatch(batch)`
  - Saves batch metadata and all batch transactions in a single DB transaction (`commit/rollback`).
  - Guards against duplicate transaction IDs in persisted records.
- `processEndOfDayBatch(batch)`
  - Calls `processBatch` and prints success message.
- `printBatchSummary(batchId)`
  - Reads batch date + transactions + total amount.
  - Prints concise summary.
- `printClearingHouseReport(batchId)`
  - Loads transactions for the batch.
  - Aggregates by `Channel`.
  - Calculates receive count/amount, pay count/amount, and net (`CR - DR`).
  - Prints tabular register + totals.

### 3) `TransactionRepository.java` (DAO for transactions)

**Purpose:** JDBC SQL for transaction table.

Methods:

- `save(txn, batchId)` and `save(connection, txn, batchId)` → insert into `transactions`.
- `findByBatchId(batchId)` → fetch transaction rows for a batch.
- `totalAmountForBatch(batchId)` → sum batch amounts (`COALESCE`).

### 4) `SettlementBatchRepository.java` (DAO for batch metadata)

**Purpose:** JDBC SQL for `settlement_batch` table.

Methods:

- `save(batch)` and `save(connection, batch)` → insert batch ID + date.
- `findBatchDate(batchId)` → fetch batch date by ID.

### 5) `ConnectionPool.java` (Datasource bootstrap)

**Purpose:** Creates a reusable `DataSource`.

Behavior:

- Loads `resources/db.properties`.
- Reads:
  - `DRIVER_CLASS`
  - `CONNECTION_STRING`
  - `USERNAME`
  - `PASSWORD`
- Registers driver (if provided).
- Exposes `ConnectionPool.getDataSource()`.

Implementation note:

- Current implementation uses a lightweight `DriverManager`-backed `DataSource`.

### 6) Entities (`Transaction.java`, `SettlementBatch.java`)

- `Transaction`
  - One financial entry with ID, channel, amount, timestamp, DR/CR, status.
- `SettlementBatch`
  - Batch ID, date, and transaction list.
  - Builder pattern used during in-memory collection before submit.

### 7) Enums (`Channel.java`, `DrCr.java`, `Status.java`)

- `Channel` controls allowed channels.
- `DrCr` controls debit/credit direction.
- `Status` controls success/failure states.

---

## Database Schema (PostgreSQL)

Expected tables:

- `settlement_batch(batch_id, batch_date)`
- `transactions(txn_id, channel, amount, txn_time, dr_cr, status, batch_id)`

> Create these tables before running (schema file is not present in this repo).

---

## Configuration

File: `resources/db.properties`

Required keys:

- `DRIVER_CLASS`
- `CONNECTION_STRING`
- `USERNAME`
- `PASSWORD`

---

## Reports Available

From menu options:

- **Batch Summary Report** (count + total amount for one batch)
- **Clearing House Report** (channel-wise receive/pay + net)

---

## In-Memory vs Persisted

- **In-memory**
  - Current batch builder and pending transactions before submission.
- **Persisted**
  - Submitted batch + transactions stored in PostgreSQL.
  - Report screens use persisted records.

---

## Improvement Roadmap

For a prioritized, user-focused enhancement plan, see `docs/USER_SATISFACTION_IMPROVEMENTS.md`.

---

## Run Commands

Compile:

```bash
javac -d out $(find src -name '*.java')
```

Run:

```bash
java -cp out com.iispl.app.SettlementApp
```
