# Architecture

This is the current component reference. The [complete architecture guide](../project-knowledge-base/architecture.md) adds screen/file maps, sequence flows, and symptom-based troubleshooting.

## Application shell

```text
┌─────────────────────────────────────────────────────────────────────┐
│ OnboardingActivity (first run)     LockScreenActivity (app unlock) │
└─────────────────────────────────┬───────────────────────────────────┘
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│ MainActivity — NavHostFragment + BottomNavigationView              │
│                                                                    │
│ Dashboard       Calendar       Buckets       Merchants   Settings │
│     │               │              │               │         │     │
│     └───────────────┴──────────────┴───────────────┴─────────┘     │
│                              │                                     │
│                     UI actions / LiveData                          │
│                              │                                     │
│                              ▼                                     │
│                  Fragments ↔ ViewModels                           │
└──────────────────────────────┬──────────────────────────────────────┘
                               ▼
                 ┌─────────────────────────────┐
                 │ Secondary destinations      │
                 │ Categories · Quick Tiles    │
                 │ Recurring · Goals           │
                 │ Analytics · Auto Transactions│
                 │ Profile · Bucket Detail     │
                 │ Expense editor              │
                 └─────────────────────────────┘
```

- Java + Views/XML + View Binding; ViewModels expose LiveData to fragments.
- `SpendWiseApplication` initializes ThreeTenABP and schedules background work.
- Dates use `org.threeten.bp`, not `java.time`, because `minSdk` is 24.
- Monetary values use integer cents. Month keys use `YYYYMM`; fields distinguish epoch days from epoch milliseconds.
- [Navigation XML](../app/src/main/res/navigation/nav_graph.xml) is authoritative for destination IDs and arguments.

## Persistence and domain boundaries

```text
┌────────────────────────────────────────────────────────────┐
│ FRAGMENTS / DIALOGS / WORKERS                              │
└──────────────────────────┬─────────────────────────────────┘
                           ▼
┌────────────────────────────────────────────────────────────┐
│ REPOSITORIES AND DOMAIN SERVICES                           │
│ Expense / Category / Merchant / Budget / Bucket            │
│ SalaryAllocationOrchestrator / OccurrenceReconciliation    │
│ InvestmentLedgerService                                    │
└──────────────┬──────────────────────────┬──────────────────┘
               ▼                          ▼
┌──────────────────────────┐  ┌──────────────────────────────┐
│ Expense / planning DAOs  │  │ Holding / evidence DAOs      │
└──────────────┬───────────┘  └──────────────┬───────────────┘
               └───────────────┬──────────────┘
                               ▼
┌────────────────────────────────────────────────────────────┐
│ Room AppDatabase — schema 10 / 23 entities                 │
│ Expenses · categories · merchants · budgets · buckets      │
│ Providers → Holdings → InvestmentTransactions              │
│ IngestionEvents → InvestmentEventLinks · ValuationSnapshots│
└────────────────────────────────────────────────────────────┘
```

`InvestmentLedgerService.processEvidence(...)` is the investment-ingestion write boundary. It resolves providers/holdings, creates or confirms logical transactions, links source evidence, and updates materialized aggregates in a Room transaction. `recomputeHoldingAggregates(...)` includes **confirmed** logical transactions only.

Parsers return `ParsedInvestmentSignal`; they do not own database updates. `InvestmentAutoIngestionEngine` routes parsed signals into `InvestmentLedgerService`. New ingestion paths must use this boundary rather than directly changing holding totals.

`InvestmentDao` retains its historical name but operates on `Holding`. `Investment` and `InvestmentSource` are legacy, unregistered Java files, not active Room tables.

Debug permits destructive migration; release does not. Explicit migrations cover 3→9, **not 9→10**. Only migration 3→4 has instrumentation coverage. See [Room migrations](room-migrations.md).

## Investment evidence and interpretation

```text
┌───────────────┐   ┌────────────────┐   ┌──────────────────────┐
│ SMS           │   │ Notifications  │   │ Expense recovery     │
└───────┬───────┘   └────────┬───────┘   └───────────┬──────────┘
        └────────────────────┼────────────────────────┘
                             ▼
               InvestmentUnderstandingEngine
                         │         │
              UserCorrection     InvestmentSmsParser
               (before parse)    (SMS / notification)
                         │         │
                         └────┬────┘
                              ▼
                    ParsedInvestmentSignal
                              │
                              ▼
               InvestmentAutoIngestionEngine
                              │
                              ▼
               InvestmentLedgerService.processEvidence
                              │
                   ┌──────────┴──────────┐
                   ▼                     ▼
         Provider → Holding       IngestionEvent
                   │                     │
                   ▼                     ▼
         InvestmentTransaction ─ InvestmentEventLink
                   │
                   ▼
      Confirmed-only materialized holding aggregates
```

- `ExpenseInvestmentRecoveryEngine` recovers likely transfers from expense rows. Those rows remain visible in history/merchant views while consumption analytics exclude them.
- `BucketAssignmentEngine` prioritizes learned `InstrumentBucketMapping`, then on-device embedding similarity, with deterministic fallback. Uncertain assignments go to Unassigned for user confirmation.
- `SemanticBucketClassifier` loads the bundled MediaPipe `text_embedder.tflite` model.
- P2P/federated-sync classes are experimental, not a production synchronization feature.
- `SourceDetailBottomSheet` exposes source evidence through `IngestionEvent`, including sender/body when present. Notification actions `view_buckets` and `view_recurring` route through `MainActivity`.

## Background work

```text
SpendWiseApplication / WorkManager
 │
 ├── RecurringExpenseWorker ───── recurring expenses
 ├── ReminderWorker ───────────── due reminders
 ├── SmsAutoImportWorker ──────── incremental import / source repair
 ├── SalaryAutomationWorker ──── planned salary allocation fallback
 ├── InvestmentScheduleWorker ── expected SIP occurrences / catch-up
 ├── InvestmentDigestWorker ──── daily action-oriented digest
 └── InvestmentMaintenanceWorker
      ├── recompute holding aggregates
      ├── remove orphan event links
      └── repair stale occurrence links
```

Maintenance is scheduled periodically and requested immediately during application startup; import paths can request a fresh pass. Preserve `IngestionEvent.sourceKey` idempotency because WorkManager can redeliver work.

## Planned versus actual money

```text
Salary or notification credit
               │
               ▼
   TransactionParser + SalaryDetector
               │
               ▼
      EventIngestionPipeline
       (source-key idempotency)
               │
               ▼
  SalaryAllocationOrchestrator
   + BucketSalaryPrefs (month state)
               │
               ▼
 Bucket.currentBalanceCents = planned allocation


Confirmed investment evidence ──► Holding + InvestmentTransaction
                                   = actual ledger state

Expected SIP occurrences = planning/reconciliation artifacts only
```

Planned bucket balances and expected SIP occurrences must not be presented as confirmed net worth or market value.

## Guides

| Area | Reference |
|---|---|
| Screens, file map, sequence diagrams, troubleshooting | [Complete architecture guide](../project-knowledge-base/architecture.md) |
| Current entities and relationships | [Data model](data-model.md) |
| Investment semantics and limitations | [Investment ledger](../project-knowledge-base/modules/investment-ledger.md) |
| Navigation | [Navigation map](navigation-map.md) |
| Claims visible in the UI | [UI/UX guidelines](ui-ux-guidelines.md) |
| Storage and sensitive-data handling | [Privacy and security](privacy-and-security.md) |
| Partial JSON export and SAF files | [Import/export](import-export-json.md) |
| Verification and unrun connected-device tests | [Testing and quality](testing-and-quality.md) |
