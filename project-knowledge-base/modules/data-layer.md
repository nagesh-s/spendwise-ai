# Data Layer Module

## Status

Active Room database version: **10**, with **23 registered entities**.

The authoritative references are `AppDatabase.java`, the entity/DAO annotations, and `app/schemas/com.spendwise.ai.data.AppDatabase/10.json`. See [Memory Bank: Data Model](../../memory-bank/data-model.md) for field-level rules.

## Entity groups

- Expenses: `Expense`, `Category`, `Merchant`, `Tag`, `ExpenseTag`, `Account`, `Budget`, `QuickTile`, `RecurringExpense`, `FinancialGoal`.
- Bucket planning: `Bucket`, `BucketCategory`, `InstrumentBucketMapping`.
- Investment ledger: `Provider`, `Holding`, `InvestmentTransaction`, `InvestmentEventLink`, `ValuationSnapshot`.
- Learning and review: `UserCorrection`, `ReviewCandidate`.
- Schedules and evidence: `InvestmentSchedule`, `InvestmentOccurrence`, `IngestionEvent`.

`Investment` and `InvestmentSource` remain in Java source but are **not registered in the active database**. `InvestmentDao` is a legacy class name whose active queries use `Holding`.

## Storage and domain rules

- `AppDatabase` supplies DAOs and Room/PersistentStore configuration. Debug builds can use destructive migration; release builds cannot.
- `ExpenseRepository` coordinates expense-facing reads/writes.
- `InvestmentLedgerService.processEvidence(...)` is the investment write boundary. It resolves provider/holding, creates or confirms a logical transaction, links ingestion evidence, and recomputes aggregates.
- Holding aggregate recomputation excludes cancelled transactions. Net cash invested is contributions + fees + taxes − withdrawals.
- Monetary values use integer cents. Dates use epoch day or epoch milliseconds as declared by the entity; month keys use `YYYYMM`.
- `Bucket.currentBalanceCents` is planned salary allocation, not actual portfolio value. Net worth comes from confirmed holdings, not expected occurrences.
- `IngestionEvent` can retain raw sender/message evidence. Treat source contents as sensitive financial data.
- `Expense` investment-transfer/recovery metadata separates transfers from consumption analytics without hiding them from transaction history.

## Representative DAOs

- `ExpenseDao`: monthly/category aggregates, expense CRUD, sync and consumption-only analytics.
- `BucketDao`: planned bucket balance and allocation queries.
- `InvestmentDao`: active holding access despite its legacy name.
- `InvestmentTransactionDao`, `InvestmentEventLinkDao`, `ProviderDao`, `ValuationSnapshotDao`: ledger/evidence access.
- `InvestmentScheduleDao`: schedule CRUD and active schedule queries.
- `InvestmentOccurrenceDao`: occurrence lifecycle, source reconciliation, and digest aggregates. Historical `linkedInvestmentId` references a holding; `transactionId` links a logical transaction.
- `IngestionEventDao`: idempotent evidence ingestion.
- `UserCorrectionDao`, `ReviewCandidateDao`: learned corrections and uncertain-review state.

## Migrations and backup

Explicit migrations cover **3→4 through 8→9**. There is no **9→10** migration or legacy investment backfill. Instrumentation currently checks only 3→4. Do not ship the version-10 schema to existing users without a migration/upgrade policy and retained-data tests. See [Room migrations](../../memory-bank/room-migrations.md).

JSON backup format version **3** is separate from Room schema 10. It exports only selected categories, merchants, expenses, budgets, buckets, holdings, and bucket mappings; import merges rather than replacing the database. It does **not** preserve the full ledger, raw ingestion evidence, review/correction state, preferences, or receipts. See [JSON import/export](../../memory-bank/import-export-json.md).

## Current Verification Notes

- Version-10 entity and schema alignment has been inspected statically.
- Full connected-device migration tests have not been run during public-repository preparation.
- Do not use previous version-7 smoke results as evidence of a successful upgrade to version 10.

## Common Failures

| Failure | Likely Cause | Check |
|---|---|---|
| `Cannot find implementation for AppDatabase` | Room annotation processing/configuration | `app/build.gradle`, clean rebuild |
| Migration crash on upgrade | Missing migration path | `AppDatabase` registered migrations and schema JSON |
| Holding totals look wrong | Ad hoc writes or stale aggregates | Route writes through `InvestmentLedgerService`; run maintenance/recompute |
| Duplicate evidence creates unexpected results | Source identity/transaction-key handling | `IngestionEventDao`, `InvestmentEventLink`, ledger matching tests |
| Expected SIP affects net worth | Occurrence treated as a confirmed holding | Occurrence status and holding transaction linkage |
| Restored JSON is incomplete | Partial export format mistaken for full backup | Export coverage table in the memory bank |

## References

- [Memory Bank: Data model](../../memory-bank/data-model.md)
- [Memory Bank: DAO queries](../../memory-bank/room-daos-and-queries.md)
- [Memory Bank: Import/export](../../memory-bank/import-export-json.md)
- [Investment ledger guide](investment-ledger.md)

## Last Updated
2026-11-10 — schema 10, active ledger entities, migration and backup limitations.
