# Room migrations

The active schema is **10**. `AppDatabase` exports Room schemas; version 10 is committed at `app/schemas/com.spendwise.ai.data.AppDatabase/10.json`.

## Current upgrade behavior

Explicit migrations are registered for 3→4, 4→5, 5→6, 6→7, 7→8, and 8→9. There is **no 9→10 migration or legacy-investment backfill**. Version 10 uses the new ledger schema.

- Debug builds permit Room destructive fallback through `ALLOW_DESTRUCTIVE_DATABASE_MIGRATION`.
- Release builds do not permit destructive fallback.
- Do not ship the version-10 schema to existing production users without a designed 9→10 migration/backfill or an explicit documented data-loss/upgrade policy.
- No existing-user release upgrade is considered verified merely because a fresh debug build passes.
- `AppDatabaseMigrationTest` covers the 3→4 schema. It does not validate a production upgrade through version 10.

Before increasing the schema from version 10, add an explicit migration, commit the exported schema, add upgrade tests that retain representative data, and document the changes here.

## Historical migrations

| Migration | Tables created | Main changes | Notes |
| --- | --- | --- | --- |
| 1→2 | `quick_tiles` | `photoPath` on expenses | QuickTiles and photo attachments. |
| 2→3 | `recurring_expenses` | Recurring schedule fields | Recurring expenses. |
| 3→4 | `tags`, `expense_tags`, `accounts`, `financial_goals` | `accountId` on expenses | Tags, accounts, goals; default Cash seeded. |
| 4→5 | `buckets`, `bucket_categories`, `investments` | Goal bucket metadata | Legacy bucket/holding baseline. |
| 5→6 | `investment_schedules`, `investment_occurrences`, `ingestion_events`, `instrument_bucket_mappings` | Correlation/source columns on legacy investments | SIP lifecycle and idempotent ingestion. |
| 6→7 | — | Investment-transfer/recovery metadata on expenses | Spend analytics remain separated from transfers. |
| 7→8 | — | `sourceSender`, `sourceBody` on ingestion events | Source-message traceability. |
| 8→9 | `investment_sources` | Canonical instrument identity on legacy investments | Legacy multi-source holding merge. |
| 9→10 | `providers`, `holdings`, `investment_transactions`, `investment_event_links`, `valuation_snapshots`, `user_corrections`, `review_candidates` | New logical ledger, provider/holding identity, correction/review state | Fresh schema; no migration/backfill implemented; removes former legacy tables. |

## Ledger ownership after version 10

Write investment evidence through `InvestmentLedgerService`, not ad hoc DAO updates from parsers/workers/UI. The service coordinates provider/holding resolution, logical transaction state, evidence links, and aggregate materialization.

Expected SIP rows never directly affect net worth. Event evidence is not equivalent to two separate financial movements. Maintenance recomputes holding aggregates without deleting raw ingestion events.
