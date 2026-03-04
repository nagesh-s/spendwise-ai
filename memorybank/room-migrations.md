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
