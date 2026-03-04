# Buckets & Investments Module

## Purpose
Turn buckets into an automatic monthly money-management system:
- run salary allocation automatically
- separate planned vs actual ledger
- model SIP schedules and expected occurrences
- reconcile expected vs confirmed signals with strict no-duplicate behavior

## Main Screens
- `BucketsFragment`
  - compact bucket grid
  - ledger mode chips: `Planned | Actual`
  - month status banner: pending/detected/auto/fallback/manual
  - `Run Allocation Now` manual override (idempotent for current month)
  - interactive net-worth eye toggle
- `BucketDetailFragment`
  - holdings summary and holdings list
  - expected SIP section with status chips (`Expected`, `Confirmed`, `Adjusted`, `Skipped`)
  - manual resolve/edit actions for unresolved expected entries
  - low-confidence bucket mapping prompt (ask-once-then-learn)

## Automation and Data Flow

### Salary month flow
1. Salary signal detected from SMS credit or notification text.
2. Event deduped via `EventIngestionPipeline` into `ingestion_events`.
3. `SalaryAllocationOrchestrator` checks monthly idempotency.
4. Allocation plan updates planned ledger (`Bucket.currentBalanceCents`).
5. Allocation status stored in `BucketSalaryPrefs`.

### SIP expected flow
1. User creates recurring investment schedule in recurring module.
2. `InvestmentScheduleEngine` materializes expected `InvestmentOccurrence` rows.
3. Backfill rule: 24 months detailed + older rollup aggregate block.

### SIP confirmation flow
1. SMS/notification investment signal parsed.
2. Event pipeline enforces unique `sourceKey`.
3. `InvestmentAutoIngestionEngine` updates confirmed holdings.
4. `OccurrenceReconciliationEngine` resolves expected occurrence to confirmed/adjusted.
5. Net worth updates only from `investments` (actual ledger).

## Duplicate Prevention Strategy
- Global idempotency: unique `ingestion_events.sourceKey`.
- Correlation matching: `correlationKey` links debit/allotment/correction paths.
- Source precedence: first accepted event creates path; later correlated events update path.
- Worker/restart safety: duplicate deliveries are ignored through registry checks.

## Bucket Assignment Strategy
- `BucketAssignmentEngine` priority:
  1) learned mapping table (`InstrumentBucketMapping`)
  2) on-device inference heuristics
  3) rules fallback
- Low-confidence inserts get map-pending marker.
- Bucket detail prompts user once and stores mapping for future auto-apply.

## Key Files
- `app/src/main/java/com/spendwise/ai/buckets/BucketsFragment.java`
- `app/src/main/java/com/spendwise/ai/buckets/BucketDetailFragment.java`
- `app/src/main/java/com/spendwise/ai/engine/SalaryAllocationOrchestrator.java`
- `app/src/main/java/com/spendwise/ai/engine/EventIngestionPipeline.java`
- `app/src/main/java/com/spendwise/ai/engine/InvestmentScheduleEngine.java`
- `app/src/main/java/com/spendwise/ai/engine/OccurrenceReconciliationEngine.java`
- `app/src/main/java/com/spendwise/ai/engine/InvestmentAutoIngestionEngine.java`
- `app/src/main/java/com/spendwise/ai/engine/BucketAssignmentEngine.java`
- `app/src/main/java/com/spendwise/ai/work/SalaryAutomationWorker.java`
- `app/src/main/java/com/spendwise/ai/work/InvestmentScheduleWorker.java`
- `app/src/main/java/com/spendwise/ai/work/InvestmentDigestWorker.java`
