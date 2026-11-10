# Data model

The active schema is **Room version 10**. `app/schemas/com.spendwise.ai.data.AppDatabase/10.json` is the generated schema reference; entity annotations are authoritative for implementation details.

## Expense and planning entities

- `Expense(id, dateEpochDay, amountCents, description, categoryId, merchantId, photoPath, accountId, isInvestmentTransfer, linkedInvestmentId, recoveryStatus, canonicalSourceKey)`
- `Category(id, name, colorArgb, iconKey, isDefault)`
- `Merchant(id, name)`
- `Budget(id, monthKeyYYYYMM, categoryId, limitCents)`; month keys use `YYYYMM`.
- `QuickTile(id, categoryId, merchantId, label, amountCents)`
- `RecurringExpense`: amount/category/merchant, frequency, scheduling bounds, and reminder fields.
- `Tag(id, name, colorArgb)`, `ExpenseTag(expenseId, tagId)`
- `Account`: label/type/balance/presentation metadata.
- `FinancialGoal`: target/current amounts, dates, status, and optional linked category.
- `Bucket`: target allocation, planned current balance, goal metadata, and type; system types include EMERGENCY, SHORT_TERM, MEDIUM_TERM, LONG_TERM, RETIREMENT, CUSTOM, and UNASSIGNED.
- `BucketCategory`: bucket-to-category relationship.

## Investment ledger entities

- `Provider(id, normalizedName, name, type, packageName, createdAtMillis)`; normalized name is unique.
- `Holding`: provider/bucket references, normalized instrument identity, folio hint, SIP/maturity metadata, materialized contribution/withdrawal/income/fee/tax totals, net cash invested, current value, absolute P/L, and canonical instrument key (unique).
- `InvestmentTransaction`: logical `CONTRIBUTION`, `WITHDRAWAL`, `INCOME`, `FEE`, `TAX`, or `ADJUSTMENT`; `PENDING`, `CONFIRMED`, or `CANCELLED` state; amount, unique transaction key, timestamps, reference/correlation IDs, and optional superseding transaction. Amounts are positive cents.
- `InvestmentEventLink`: unique ingestion-event link to holding and optional logical transaction, with parsed signal type, matching strategy, and confidence.
- `ValuationSnapshot`: holding/date/source-unique value observation.
- `InvestmentSchedule`: bucket and nullable holding, amount/frequency/date bounds, and active state.
- `InvestmentOccurrence`: expected schedule occurrence, confirmed amount/timestamp, nullable `linkedInvestmentId` (references `Holding` despite the historical column name), nullable logical `transactionId`, correlation/source metadata.
- `IngestionEvent`: globally unique source key, correlation key, source/domain, amount/timestamps, processing status, optional payload fingerprint, and optional `sourceSender`/`sourceBody` raw evidence.
- `InstrumentBucketMapping`: learned assignment memory.
- `UserCorrection`: correction type + normalized key (unique together), corrected value, confirmation count, and update timestamp.
- `ReviewCandidate`: ambiguous evidence and proposed interpretation/holding/transaction with reason/confidence/status.

`Investment` and `InvestmentSource` are legacy, unregistered Java entity source files; they are not active schema tables.

## Relationships and accounting

```text
Bucket 1 ─── n Holding n ─── 1 Provider (optional)
Holding 1 ─── n InvestmentTransaction
Holding 1 ─── n ValuationSnapshot
IngestionEvent 1 ─── 0..1 InvestmentEventLink → Holding / InvestmentTransaction
InvestmentSchedule 1 ─── n InvestmentOccurrence
InvestmentSchedule n ─── 0..1 Holding
InvestmentOccurrence n ─── 0..1 InvestmentTransaction
```

- `Bucket.currentBalanceCents` is planned salary allocation, not market value.
- Logical transactions determine holding aggregates; raw source messages are evidence and do not automatically become separate investments.
- `recomputeHoldingAggregates` includes only confirmed transactions; pending and cancelled transactions do not affect materialized aggregates.
- `netCashInvested = contributions + fees + taxes - withdrawals`.
- `absolutePnl = currentValue + withdrawals + income - contributions - fees - taxes`.
- Net cash invested is not tax cost basis; units/NAV-level cost basis is not modeled.
- Current valuation comes from holding/snapshot data, not expected occurrences.
- Expense investment transfers remain in history but are excluded from consumption-only spending aggregates.

## Database upgrade boundary

Registered explicit migrations cover 3→9. There is deliberately **no 9→10 migration/backfill**. Release builds must not assume old investment data is preserved when upgrading to version 10. A production migration policy is required before releasing to existing users; the current destructive fallback is enabled only for debug builds.

See [Room migrations](room-migrations.md).
