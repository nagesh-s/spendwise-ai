# Buckets & Investment Schedules

## Purpose

Coordinate planned salary allocation with actual investment holdings, scheduled SIP expectations, source reconciliation, and bucket suggestions.

## Main Screens

- `BucketsFragment`: compact bucket grid, Planned/Actual modes, month allocation status, manual allocation action, and net-worth visibility.
- `BucketDetailFragment`: holdings, expected SIP occurrence states, manual resolve/edit actions, and uncertain bucket-assignment confirmation.

## Salary Planning

1. Salary signal arrives from SMS or notification parsing.
2. `EventIngestionPipeline` records a deduplicated `IngestionEvent`.
3. `SalaryAllocationOrchestrator` applies monthly guardrails/idempotency.
4. The plan updates `Bucket.currentBalanceCents`; status is stored in `BucketSalaryPrefs`.

**Planned bucket balances are not actual investment holdings.**

## Schedules and Confirmation

1. A recurring investment creates an `InvestmentSchedule`.
2. `InvestmentScheduleEngine` materializes expected `InvestmentOccurrence` rows, including detailed-history limits/older rollups.
3. SMS/notification/recovery signals flow through `InvestmentAutoIngestionEngine` into `InvestmentLedgerService.processEvidence(...)`.
4. The service resolves Provider/Holding, creates or confirms an `InvestmentTransaction`, links evidence, and recomputes aggregates.
5. `OccurrenceReconciliationEngine` resolves expected occurrences and connects holding/transaction identifiers where supported.
6. Expected, skipped, or otherwise unconfirmed occurrences do not contribute to net worth; actual holdings/transactions do.

`InvestmentOccurrence.linkedInvestmentId` is a historical field name for a holding. `transactionId` separately identifies the logical transaction.

## Holdings Repair and Review

- Bucket detail can start investments-only SMS repair, using an investment cursor separate from expense import.
- `ExpenseInvestmentRecoveryEngine` finds likely investment transfers in expense history and feeds evidence into the same ingestion/ledger path.
- `InvestmentUnderstandingEngine` prioritizes correction memory, deterministic parsing, then lightweight semantic interpretation; uncertain cases can enter review.
- `InvestmentMaintenanceWorker` repairs stale occurrence links, removes orphan event links, and recomputes aggregates.
- Do not describe all historical-plan merge/conflict invariants as implemented without source/test verification.

## Duplicate and Identity Rules

- Unique `IngestionEvent.sourceKey` suppresses repeated source delivery.
- Ledger correlation keys can connect matching evidence to a logical transaction; inspect `InvestmentLedgerService` for current matching scope.
- Logical transaction keys and unique event links provide additional identity constraints.
- Recovery metadata (`canonicalSourceKey`, optional `linkedInvestmentId`) supports expense-recovery idempotency.
- Provider/instrument identity matters: neither automatically counting a debit and allotment twice nor promising universal merging is correct.

## Accounting and Bucket Assignment

- Investment-transfer expenses remain visible in Calendar and merchant history.
- Consumption analytics exclude `Expense.isInvestmentTransfer = 1`.
- Active net worth uses `Holding`/`InvestmentTransaction`, not the inactive legacy `Investment` table or expected occurrences.
- Bucket assignment priority: learned `InstrumentBucketMapping` → local text-embedding classifier → deterministic asset/keyword fallback → Unassigned confirmation.

## Key Files

```
buckets/BucketsFragment.java
buckets/BucketDetailFragment.java
domain/InvestmentLedgerService.java
engine/SalaryAllocationOrchestrator.java
engine/EventIngestionPipeline.java
engine/InvestmentScheduleEngine.java
engine/OccurrenceReconciliationEngine.java
engine/InvestmentAutoIngestionEngine.java
engine/InvestmentUnderstandingEngine.java
engine/ExpenseInvestmentRecoveryEngine.java
engine/BucketAssignmentEngine.java
work/SalaryAutomationWorker.java
work/InvestmentScheduleWorker.java
work/InvestmentDigestWorker.java
work/InvestmentMaintenanceWorker.java
```

Paths are relative to `app/src/main/java/com/spendwise/ai/`.

## Related Guides

- [Investment ledger](investment-ledger.md)
- [Data layer](data-layer.md)
- [Memory Bank: Architecture](../../memory-bank/architecture.md)
- [Memory Bank: Data model](../../memory-bank/data-model.md)

## Last Updated
2026-11-10 — provider/holding/logical-transaction flow and review/maintenance ownership.
