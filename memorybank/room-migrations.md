## Room Migrations

Write explicit Migration objects for version increments and add regression tests that validate data retention across updates.

Document entity and index changes with clear rationale and upgrade steps per version.

### Migration History

| Migration | Tables Created | Columns Added | Notes |
|-----------|---------------|---------------|-------|
| 1→2 | `quick_tiles` | `photoPath` on `expenses` | Phase 1: QuickTiles + photo attachments |
| 2→3 | `recurring_expenses` | — | Phase 1: Recurring expenses system |
| 3→4 | `tags`, `expense_tags`, `accounts`, `financial_goals` | `accountId` on `expenses` | Phase 2: Tags, accounts, goals. Default Cash account seeded. |
| 4→5 | `buckets`, `bucket_categories`, `investments` | — | Bucket feature: default goal buckets seeded. |
| 5→6 | `investment_schedules`, `investment_occurrences`, `ingestion_events`, `instrument_bucket_mappings` | `primaryCorrelationKey`, `lastSourceType`, `lastSourceKey` on `investments` | Salary-to-Buckets automation: dual ledger support, SIP lifecycle, idempotent ingestion, learned assignment mapping. |
| 6→7 | — | `isInvestmentTransfer`, `linkedInvestmentId`, `recoveryStatus`, `canonicalSourceKey` on `expenses` | Holdings repair: recover investment-like expense rows into Buckets while keeping analytics/budget totals clean. |

### Migration 5→6 Details

Located in `AppDatabase.java` as `MIGRATION_5_6`.

Key DDL:
- `ALTER TABLE investments ADD COLUMN primaryCorrelationKey TEXT`
- `ALTER TABLE investments ADD COLUMN lastSourceType TEXT`
- `ALTER TABLE investments ADD COLUMN lastSourceKey TEXT`
- create `investment_schedules` with `bucketId` FK and active/index fields
- create `investment_occurrences` with:
  - unique index `(scheduleId, dueEpochDay)`
  - status/correlation/link indices for reconciliation
- create `ingestion_events` with:
  - unique index on `sourceKey` for strict idempotency
  - correlation/timestamp/status indices
- create `instrument_bucket_mappings` with:
  - unique composite index `(instrumentPattern, platformPattern, assetType)`
  - `bucketId` FK

Operational notes:
- Expected SIP rows (`investment_occurrences`) are planning artifacts and do not count toward net worth.
- Ingestion events are marked `RECEIVED` then transitioned to `PROCESSED|FAILED|SKIPPED` by the pipeline.
- Migration is additive and preserves existing holdings data.

### Migration 6→7 Details

Located in `AppDatabase.java` as `MIGRATION_6_7`.

Key DDL:
- `ALTER TABLE expenses ADD COLUMN isInvestmentTransfer INTEGER NOT NULL DEFAULT 0`
- `ALTER TABLE expenses ADD COLUMN linkedInvestmentId INTEGER`
- `ALTER TABLE expenses ADD COLUMN recoveryStatus TEXT NOT NULL DEFAULT 'NONE'`
- `ALTER TABLE expenses ADD COLUMN canonicalSourceKey TEXT`
- create index on `expenses.isInvestmentTransfer`
- create index on `expenses.recoveryStatus`
- create unique index on `expenses.canonicalSourceKey`

Operational notes:
- Existing expense rows are preserved and remain visible in Calendar and merchant history.
- Recovered investment transfers are excluded from budget/analytics queries through DAO-level filtered aggregates.
- `canonicalSourceKey` keeps expense-derived holding recovery idempotent across retries and later SMS repair runs.
