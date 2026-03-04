# Data Layer Module

> Purpose: local persistence, aggregates, and automation-safe idempotent ingestion.

## Status
Implemented and active on Room DB version `6`.

## Core Design
- Offline-first Room architecture.
- Write path: `Fragment/ViewModel -> Repository or Engine -> AppDatabase.databaseWriteExecutor -> DAO`.
- Read path: `DAO LiveData -> ViewModel -> UI observers`.
- Amounts in cents (`long`), dates as epoch-day (`long`) for stable queries.

## Entity Groups
- Expense tracking: `Expense`, `Category`, `Merchant`, `Budget`, `QuickTile`, `RecurringExpense`, `Tag`, `ExpenseTag`, `Account`, `FinancialGoal`.
- Buckets and holdings: `Bucket`, `BucketCategory`, `Investment`.
- Automation/schedules: `InvestmentSchedule`, `InvestmentOccurrence`, `IngestionEvent`, `InstrumentBucketMapping`.

## Ledger Model
- Planned ledger: monthly allocation plan in `Bucket.currentBalanceCents`.
- Actual ledger: confirmed holdings from `Investment`.
- Net worth/portfolio totals use actual ledger only.

## DB Migration Snapshot
- `4 -> 5`: buckets + investments baseline.
- `5 -> 6`: adds SIP schedule and ingestion pipeline model:
  - new tables: `investment_schedules`, `investment_occurrences`, `ingestion_events`, `instrument_bucket_mappings`
  - added columns on `investments`: `primaryCorrelationKey`, `lastSourceType`, `lastSourceKey`
  - unique index on `ingestion_events.sourceKey` for strict idempotency

## Key DAOs Added in v6
- `InvestmentScheduleDao`: schedule CRUD and active schedule queries.
- `InvestmentOccurrenceDao`: expected occurrence lifecycle, reconciliation resolution, and digest aggregates.
- `IngestionEventDao`: global event dedupe registry.
- `InstrumentBucketMappingDao`: learned bucket assignment memory.

## Operational Guarantees
- Event dedupe works across SMS, notifications, manual actions, and schedule workers.
- Worker retries and app restarts do not duplicate logical financial events.
- Expected SIP values do not affect net worth until confirmed.

## Key Files
- `app/src/main/java/com/spendwise/ai/data/AppDatabase.java`
- `app/src/main/java/com/spendwise/ai/data/dao/*.java`
- `app/src/main/java/com/spendwise/ai/data/entity/*.java`
- `app/src/main/java/com/spendwise/ai/data/repository/*.java`

## Last Updated
2026-03-04
