# Investment Ledger Module

## Purpose

Record investment evidence as providers, holdings, logical transactions, and source-event links without letting parsers or UI components directly mutate holding aggregates.

## Current Model

```text
SMS / notification / expense recovery
                ↓
        ParsedInvestmentSignal
                ↓
       InvestmentLedgerService
                ↓
Provider → Holding → InvestmentTransaction
                       ↘ InvestmentEventLink → IngestionEvent
                       ↘ ValuationSnapshot (holding valuation history)
```

- `Provider`: normalized broker/platform identity.
- `Holding`: resolved instrument and materialized totals. `InvestmentDao` is the legacy-named DAO for this active entity.
- `InvestmentTransaction`: logical contribution/withdrawal/income/fee/tax/adjustment with pending, confirmed, or cancelled state.
- `InvestmentEventLink`: connects source evidence to a holding and transaction.
- `IngestionEvent`: unique source identity and optionally raw sender/body; sensitive financial data.
- `ValuationSnapshot`: valuation observations, not expected SIP occurrences.

`Investment` and `InvestmentSource` are inactive legacy Java files, not active Room tables.

## Write Boundary

Use `InvestmentLedgerService.processEvidence(event, signal, assignedBucketId)` for evidence ingestion. It currently:

1. Rejects non-positive signal amounts.
2. Resolves or creates provider and holding identities.
3. Uses a correlation key to find a candidate transaction, confirms a matching pending transaction for allotment/FD-booked evidence, or creates a contribution/withdrawal.
4. Links source evidence and updates holding metadata.
5. Recomputes holding aggregates.

This is not a guarantee that every historical-plan merge rule is implemented. Check source and tests before claiming conflict-resolution or cross-provider merge guarantees.

## Accounting Rules

Aggregate recomputation includes only confirmed transactions; pending and cancelled transactions are excluded:

```text
netCashInvested = contributions + fees + taxes − withdrawals
absolutePnl = currentValue + withdrawals + income
              − contributions − fees − taxes
```

When no independently known current value exists, current value tracks net cash invested to avoid artificial loss. `Bucket.currentBalanceCents` is planned salary allocation, not actual holdings. Expected `InvestmentOccurrence` rows do not increase net worth.

## Review and Maintenance

- `InvestmentUnderstandingEngine`: correction memory → deterministic parsing → lightweight semantic interpretation → uncertain review handling.
- `BucketAssignmentEngine`: learned mapping → on-device embedding classification → deterministic fallback → Unassigned confirmation for uncertainty.
- `OccurrenceReconciliationEngine`: links expected scheduled investments to confirmed activity.
- `InvestmentMaintenanceWorker`: repairs stale occurrence links, removes orphan event links, and recomputes aggregates; scheduled periodically and immediately by `SpendWiseApplication`.

## Tests and Diagnostics

- Unit coverage: `domain/InvestmentLedgerServiceTest.java`, `sms/InvestmentSmsParserTest.java`, and `sms/GoldenCorpusEvaluatorTest.java`.
- Inspect `ingestion_events`, `investment_event_links`, `holdings`, and `investment_transactions` together to diagnose mismatches.
- JSON export/import does not restore the complete ledger or source/review state. See [Data layer](data-layer.md).
- No 9→10 migration exists. Existing-user data retention needs an explicit migration and tests before release.

## Last Updated
2026-11-10 — current version-10 ledger boundary and limitations.
