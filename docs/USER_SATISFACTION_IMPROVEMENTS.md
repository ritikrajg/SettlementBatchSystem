# User Satisfaction Improvement Plan

This plan is tailored to the current Settlement Batch System and focuses on improvements that end users (operators, analysts, and support teams) will feel directly.

## 1) Highest-impact quick wins (do first)

1. **Add startup checks for DB connectivity + schema validation**
   - Before showing the menu, verify DB connection and required tables/columns.
   - Show clear error with fix hints (wrong URL, missing table, bad credentials).
   - Why users care: avoids confusing runtime failures during batch submission.

2. **Add input validation rules beyond enum checks**
   - Reject duplicate batch IDs in the same day (or warn with override).
   - Reject duplicate transaction IDs.
   - Reject zero/negative amounts unless explicitly allowed.
   - Prevent sender and receiver bank being identical when business rules forbid it.
   - Why users care: fewer data-entry mistakes and fewer reversals.

3. **Add better operator flow protections**
   - Confirm before submitting a batch (`Are you sure? y/n`).
   - Warn when exiting with unsaved transactions.
   - Add `View current unsaved batch` option to review before submit.
   - Why users care: confidence and fewer accidental losses.

4. **Export reports to CSV**
   - Add menu options to export Batch Summary and Clearing House report.
   - Why users care: users can share, audit, and open in Excel instantly.

5. **Improve error messages with root context**
   - Replace generic `Error: <message>` with categorized errors (`Validation`, `Database`, `Unexpected`).
   - Include recommended next action.
   - Why users care: faster issue resolution without developer help.

---

## 2) Functional features users will notice next

1. **Search and filters for transactions**
   - Filter by batch ID, date range, bank, status, channel, amount range.
   - Why users care: easier investigation and reconciliation.

2. **End-of-day reconciliation report**
   - Show opening count, processed count, failed/pending count, totals by DR/CR, net.
   - Why users care: daily closure is faster and auditable.

3. **Transaction status lifecycle**
   - Support updates (e.g., `PENDING -> SUCCESS/FAILED`) with audit trail.
   - Why users care: mirrors real settlement lifecycle.

4. **Auto batch ID generation**
   - Optional format like `BATCH-YYYYMMDD-001`.
   - Why users care: fewer collisions, less manual typing.

5. **Role-based CLI actions (basic)**
   - Operator: create/add/submit.
   - Supervisor: reports/export/reconciliation.
   - Why users care: safer operations and clearer accountability.

---

## 3) Reliability and trust improvements

1. **Transactional DB writes for batch + transactions**
   - Save batch and all txns atomically (`commit/rollback`).
   - Why users care: no partial saves.

2. **Add unique constraints and indexes**
   - Unique: `settlement_batch.batch_id`, `transactions.txn_id`.
   - Indexes: `transactions(batch_id)`, `transactions(status)`, `transactions(txn_time)`.
   - Why users care: faster reports and stronger data integrity.

3. **Audit logging**
   - Log key events: batch init, txn add, submit, report generation.
   - Why users care: easier compliance and support troubleshooting.

4. **Graceful retries for transient DB failures**
   - Retry idempotent reads, careful retry policy for writes.
   - Why users care: fewer disruptions during temporary outages.

---

## 4) UX improvements (CLI still, but much better)

1. **Consistent screen formatting**
   - Section headers, aligned columns, currency formatting, separators.
2. **Add help command and inline examples**
   - Example inputs for each prompt.
3. **Color/emphasis for statuses**
   - Success/warning/error visibility.
4. **Pagination for large report outputs**
   - Avoid huge terminal dumps.

Why users care: clarity, less cognitive load, lower training time.

---

## 5) Engineering improvements that increase user happiness indirectly

1. **Schema migration scripts (e.g., V1__init.sql)**
   - Remove manual DB setup uncertainty.
2. **Automated tests (unit + integration)**
   - Service logic, repository SQL, and happy/negative flows.
3. **Seed/demo data script**
   - Lets new users evaluate quickly.
4. **Configuration profiles**
   - `dev`, `test`, `prod` DB properties.
5. **Packaging**
   - Build runnable JAR with one command.

---

## Suggested implementation order (practical roadmap)

- **Sprint 1 (1-2 weeks):** startup checks, validation rules, submit/exit confirmations, improved errors.
- **Sprint 2 (1-2 weeks):** CSV exports, search/filter, reconciliation report.
- **Sprint 3 (1-2 weeks):** transactional writes, DB constraints/indexes, audit logs.
- **Sprint 4 (optional):** status lifecycle, role-based actions, usability polish.

---

## Success metrics to know users are truly happier

- Reduction in invalid submissions and correction tickets.
- Faster average time to complete a batch.
- Faster support resolution time for user-reported issues.
- Increase in report/export usage.
- Lower rate of failed end-of-day closures.
