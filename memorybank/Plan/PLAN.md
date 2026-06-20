# SpendWise Investment Ledger and Intelligent Ingestion Plan

## Summary

Rebuild investment tracking around four separate concepts:

```text
Provider → Holding → Logical Transaction
                   ↘ Valuation Snapshot

Ingestion Event → Parsed Evidence → Logical Transaction
Investment Schedule → Holding
```

A debit and allotment are evidence for one logical contribution, not two investments. The implementation will ship incrementally:

1. Foundation and ledger
2. Deterministic parsing and reliability
3. Frictionless investment UX
4. Optional on-device ML

Existing development data is disposable. Version 10 will use a fresh schema without legacy investment backfill.

## 1. Foundation Release

### Schema

Add these entities:

- `Provider`
  - `id`
  - unique `normalizedName`
  - display `name`
  - `type`: `BROKER`, `BANK`, `AMC`, `OTHER`
  - optional `packageName`
- `Holding`
  - provider and bucket foreign keys
  - instrument name, normalized name and asset type
  - optional folio/account hint
  - SIP metadata, maturity data and canonical identity
  - materialized totals for efficient lists
- `InvestmentTransaction`
  - represents one logical financial movement
  - types: `CONTRIBUTION`, `WITHDRAWAL`, `INCOME`, `FEE`, `TAX`, `ADJUSTMENT`
  - statuses: `PENDING`, `CONFIRMED`, `CANCELLED`
  - positive `amountCents`
  - unique `transactionKey`
  - event and effective timestamps
  - optional reference ID
- `InvestmentEventLink`
  - links an `IngestionEvent` to a holding and logical transaction
  - records parsed signal type and matching strategy
  - permits multiple messages to support one transaction
- `ValuationSnapshot`
  - holding, date, value and source
  - unique per holding/date/source
- `UserCorrection`
  - correction type, normalized key, corrected value and confirmation count
- `ReviewCandidate`
  - ambiguous parsing, dedup or holding-match decisions awaiting review

Modify:

- `InvestmentSchedule` gains nullable `holdingId`.
- `InvestmentOccurrence` links to `transactionId` in addition to its holding.
- `IngestionEvent` retains immutable raw evidence and gains indexed normalized payload fingerprint.
- Replace `Investment` with `Holding`.
- Remove `InvestmentSource`; `IngestionEvent` plus `InvestmentEventLink` replaces it.

### Accounting rules

One logical contribution may have multiple evidence events:

```text
Bank debit → create PENDING CONTRIBUTION
Allotment confirmation → link evidence and mark same transaction CONFIRMED
Repeated notification → link or discard as duplicate evidence
```

Only logical transactions affect totals.

Derived metrics:

```text
totalContributions = confirmed CONTRIBUTION amounts
totalWithdrawals   = confirmed WITHDRAWAL amounts
netCashInvested    = contributions + fees + taxes - withdrawals
currentValue       = latest valuation snapshot
absolutePnL        = currentValue + withdrawals + income
                     - contributions - fees - taxes
```

Do not describe `netCashInvested` as tax cost basis. Partial redemption cost basis requires units/NAV data not currently available.

Holding materialized fields are updated in the same Room transaction as ledger writes. A maintenance reconciliation recomputes them from the ledger and repairs discrepancies.

### Database policy

- Bump Room schema to version 10 and recreate development databases.
- Do not implement migration 9→10 or legacy backfill.
- Enable Room schema export and commit exported schemas.
- Allow destructive migration only in debug builds.
- Release builds must require explicit migrations from version 10 onward.
- Use local timezone conversion for epoch-day fields.

### Domain service

Introduce one transaction-bound service responsible for:

- Resolving or creating providers and holdings
- Matching evidence to an existing logical transaction
- Creating or transitioning transactions
- Linking ingestion evidence
- Updating holding aggregates
- Reconciling schedule occurrences
- Returning an explicit result: inserted, confirmed, duplicate, review-required or rejected

No worker, listener, ViewModel or parser may directly mutate ledger aggregates.

## 2. Deterministic Reliability Release

### Parser contract

Both SMS and notification parsing return a common structured result:

- Financial/investment classification
- Signal type and confidence
- Amount, merchant, provider, instrument and asset type
- Direction and reference identifiers
- Field-level confidence
- Normalized payload fingerprint
- Reasons for confidence and matching decisions

Parsers never write directly to the database.

### Deduplication and correlation order

Apply this precedence:

1. Existing source key → exact duplicate
2. Exact normalized payload plus compatible timestamp → duplicate evidence
3. Strong reference ID → correlate
4. Existing lifecycle/correlation key → correlate
5. Provider + instrument + amount + compatible signal type and narrow date window → candidate
6. Weak fuzzy match → review queue
7. No candidate → create a new holding or transaction as appropriate

Amount-and-time similarity alone must never automatically discard an event.

Debit and allotment signals are compatible lifecycle evidence, not duplicates. Two same-direction debits remain separate unless supported by exact source, payload or reference evidence.

### Holding identity

Canonical holding matching uses:

```text
provider + asset type + normalized instrument + optional folio/account hint
```

Weak names such as “Investment Allocation” cannot automatically merge holdings unless a strong reference or established learned mapping exists.

### Personalization

Correction lookup runs before classification.

Supported correction types:

- Merchant display name
- Merchant category
- Provider alias
- Instrument alias
- Asset type
- Holding assignment
- Bucket assignment

Auto-apply a correction after two consistent confirmations. Conflicting corrections reset automatic application and create a review candidate.

### Review workflow

Review candidates must show:

- Raw source
- Proposed interpretation
- Confidence and reason
- Possible matching holding/transaction
- Actions: accept, edit, keep separate, merge, ignore

Accepted decisions create correction mappings where appropriate.

### Maintenance

Remove investment repair scans from the ingestion hot path.

Add a daily constrained WorkManager job that:

- Recomputes holding aggregates
- Detects orphan event links
- Detects duplicate canonical holdings without merging automatically
- Repairs stale occurrence links
- Reports counts without deleting evidence

## 3. Investment UX Release

### Navigation

```text
Bucket Detail
  → Provider group
    → Holding
      → Logical transaction
        → Source evidence
```

Bucket detail displays:

- Provider groups
- Holding count
- Net cash invested
- Current value and absolute P/L
- Active SIP summary
- Pending/review counts

Holding detail displays:

- Provider and instrument identity
- Confirmed contributions and withdrawals
- Current valuation and P/L
- Linked SIP schedule
- Logical transaction timeline
- Pending events and review actions

Transaction detail displays:

- Logical accounting effect
- Status and timestamps
- Linked debit/allotment/notification evidence
- Reference identifiers
- Matching explanation

Do not show debit and allotment as two contributions. They may appear as two evidence entries inside one contribution.

### Manual operations

Manual add creates a confirmed logical transaction through the same domain service.

Editing a confirmed amount creates an auditable adjustment or replacement operation; it must not silently rewrite evidence.

Deleting a transaction changes it to cancelled and recalculates aggregates. Raw ingestion evidence remains retained.

### SIP behavior

- A schedule belongs to one holding.
- Expected occurrences do not affect portfolio totals.
- A matched debit marks the logical contribution pending.
- Allotment or explicit confirmation marks it confirmed.
- Monthly completion is based on confirmed logical contributions, not evidence count.

## 4. Measurement and ML Release

### Golden corpus

Create a sanitized fixture corpus covering:

- Expense and investment messages
- SMS and notification variants
- Debit/allotment pairs
- Repeated source delivery
- Multiple identical legitimate transactions
- Corrections and redemptions
- Unknown providers and instruments
- Promotional and non-financial messages

No real unsanitized SMS is committed.

Track:

- Financial and investment classification precision/recall
- Field accuracy
- Holding-match accuracy
- Transaction-correlation accuracy
- False merges and false splits
- Review rate
- Parser latency

Initial release gates:

- Amount accuracy ≥99%
- Direction accuracy ≥98%
- Investment classification precision ≥97%
- Automatic false-merge rate = 0 in the corpus
- Exact replay creates no additional logical transactions
- Debit/allotment pair produces one contribution

### ML escalation

Use this sequence:

```text
Correction memory
→ deterministic parser
→ lightweight ML model (TFLite or Semantic)
→ review queue
```

ML output passes through the same validation and correlation layer and cannot bypass ledger invariants.

TFLite is accepted only after meeting:

- Model size below 10 MB
- Median inference below 100 ms on target mid-range hardware
- Resident memory below 50 MB
- Measurable corpus improvement over deterministic parsing

### Future Scope: Offline Assistant (Gemma)
Gemma (or other large generative models ~1.5GB) is massive overkill for 160-character SMS extraction. However, it is reserved as a **future scope** for a "Google Assistant-like" feature inside the app without internet (e.g., "Did I spend 500 rs on petrol last month?"). 
When implemented, Gemma will remain optional in the `offlineLlm` flavor and requires:
- Play Asset Delivery or Sideloading (to maintain "true offline" constraint without INTERNET permission)
- Device capability checks (>= 6GB RAM)
- Cold-start, latency, memory and battery measurements

## Test Plan

- Fresh version-10 database creation and Room schema validation
- Release database refuses destructive fallback
- Provider and canonical holding uniqueness
- New contribution creation and aggregate update are atomic
- Debit followed by allotment confirms one contribution without doubling totals
- Notification/SMS replay remains idempotent
- Two legitimate equal debits remain separate
- Strong references correlate across source types
- Weak matching creates review candidates
- Contribution, withdrawal, fee, income and cancellation calculations
- Valuation snapshot selection and P/L calculation
- Schedule occurrence reconciliation
- Correction precedence and conflict handling
- Maintenance reconciliation repairs deliberately corrupted aggregates
- Bucket → provider → holding totals agree with ledger queries
- Parser golden-corpus regression tests
- Worker/listener concurrency tests for simultaneous duplicate delivery
- Process-restart test between event insertion and ledger processing

## Delivery Sequence

1. Version-10 schema, domain service and aggregate tests
2. Rewrite investment ingestion and schedule reconciliation
3. Unified parser result, dedup cascade and golden corpus
4. Correction memory and review queue
5. Provider and holding UX
6. Maintenance worker and production database safeguards
7. Optional TFLite experiment
8. Optional Gemma integration after benchmark approval

## Assumptions

- Existing development data may be deleted.
- Provider identity is brand-level in this release.
- Multiple accounts under one provider are deferred until reliable account identifiers exist.
- Java, Room, LiveData, WorkManager and the existing Android UI stack remain in use.
- The app remains offline-first; raw financial messages never leave the device.
- Features ship incrementally, with ML excluded from the foundation release.
