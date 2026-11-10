# SpendWise AI — Complete Architecture Guide

> **Purpose:** Locate components, understand their data flow, and know where to investigate a problem. Field-level persistence rules live in [Memory Bank: Architecture](../memory-bank/architecture.md).

---

## 🏗️ System Architecture

```text
┌───────────────────────────────────────────────────────────────────────┐
│ UI LAYER                                                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │
│  │Dashboard │ │Calendar  │ │ Buckets  │ │Merchants │ │ Settings │     │
│  │Fragment  │ │Fragment  │ │Fragment  │ │Fragment  │ │Fragment  │     │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘     │
│       │ LiveData   │            │            │            │           │
│  ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐     │
│  │Dashboard │ │Calendar  │ │ Buckets  │ │Merchants │ │ Settings │     │
│  │ViewModel │ │ViewModel │ │ViewModel │ │ViewModel │ │ViewModel │     │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘     │
└───────┼────────────┼────────────┼────────────┼────────────┼───────────┘
        │            │            │            │            │
        ▼            ▼            ▼            ▼            ▼
┌───────────────────────────────────────────────────────────────────────────┐
│ REPOSITORIES AND DOMAIN SERVICES (background execution)                   │
│  ┌──────────────┐ ┌──────────────┐ ┌─────────────────────────────────┐    │
│  │ExpenseRepo   │ │ BudgetRepo   │ │ InvestmentLedgerService         │    │
│  │CategoryRepo  │ │ MerchantRepo │ │ processEvidence(...) boundary   │    │
│  │and others    │ │              │ │ allocation / reconciliation     │    │
│  └───────┬──────┘ └───────┬──────┘ └────────────────┬────────────────┘    │
└──────────┼────────────────┼─────────────────────────┼─────────────────────┘
           │                │                         │
           └────────────────┴─────────────┬───────────┘
                                          ▼
┌───────────────────────────────────────────────────────────────────────────┐
│ DAO AND STORAGE LAYER                                                     │
│  ┌───────────┐ ┌───────────┐ ┌──────────┐ ┌───────────────────────────┐   │
│  │ExpenseDao │ │CategoryDao│ │BudgetDao │ │InvestmentDao / Holding    │   │
│  └─────┬─────┘ └─────┬─────┘ └────┬─────┘ │InvestmentTransactionDao   │   │
│        │             │            │       └────────────┬──────────────┘   │
│        └─────────────┴────────────┴────────────────────┘                  │
│                                      ▼                                    │
│                    ROOM DATABASE → AppDatabase                            │
│                  schema 10 / 23 registered entities                       │
└───────────────────────────────────────────────────────────────────────────┘
```

`MainActivity` hosts the `NavHostFragment` and `BottomNavigationView`. Categories, Quick Tiles, recurring items, goals, analytics, auto transactions, profile, and bucket details are secondary destinations rather than primary tabs.

`InvestmentDao` retains its historical name but queries `Holding` entities. Parsers and background workers must not bypass `InvestmentLedgerService.processEvidence(...)` to directly change holding totals.

### Storage and compatibility conventions

- `SpendWiseApplication` initializes ThreeTenABP and schedules background jobs. Use `org.threeten.bp`, not `java.time`, with `minSdk 24`.
- Monetary amounts use integer cents. Month keys use `YYYYMM`; entity fields distinguish epoch days from epoch milliseconds.
- Java source/target compatibility is 11. Use JDK 21 for Gradle because MediaPipe GenAI includes Java 21 bytecode.
- Debug builds permit destructive migration; releases do not. Explicit migrations cover 3→9, **not 9→10**.

---

## 📁 Component File Map

### Core entry points

| File | Responsibility | When it runs |
|---|---|---|
| `MainActivity.java` | Navigation, bottom tabs, notification deep links | App launch |
| `SpendWiseApplication.java` | ThreeTen initialization and worker scheduling | Before activities |
| `AppDatabase.java` | Room singleton, migrations, default-category creation | First database access |
| `settings/SettingsFragment.java` | Secondary-feature navigation, import/export, preferences | Settings selected |

### Primary screens

| Screen | Fragment | ViewModel | Layout |
|---|---|---|---|
| Dashboard | `dashboard/DashboardFragment.java` | `DashboardViewModel.java` | `fragment_dashboard.xml` |
| Calendar | `calendar/CalendarFragment.java` | `CalendarViewModel.java` | `fragment_calendar.xml` |
| Buckets | `buckets/BucketsFragment.java` | `BucketsViewModel.java` | `fragment_buckets.xml` |
| Bucket detail | `buckets/BucketDetailFragment.java` | `BucketDetailViewModel.java` | `fragment_bucket_detail.xml` |
| Merchants | `merchants/MerchantsFragment.java` | `MerchantsViewModel.java` | `fragment_merchants.xml` |
| Settings | `settings/SettingsFragment.java` | `SettingsViewModel.java` | `fragment_settings.xml` |

### Secondary screens and integrations

| Feature | Entry point / component | Role |
|---|---|---|
| Categories & budgets | `categories/CategoriesFragment.java` | Category management and planned spending limits |
| Quick Tiles | `quicktiles/QuickTilesFragment.java` | Reusable expense shortcuts |
| Recurring Expenses / Investments | `recurring/RecurringExpensesFragment.java` | Recurring expense and SIP schedules |
| Goals / Analytics | `goals/` / `analytics/` | Goal tracking and spending analysis |
| Auto Transactions | `sms/AutoTransactionsFragment.java` | SMS import and automation controls |
| Review Queue | `review/ReviewQueueViewModel.java` | Correct ambiguous ingestion results |
| Investment ingestion | `engine/InvestmentUnderstandingEngine.java` | Correction memory, parsing, interpretation, review routing |
| Domain boundary | `domain/InvestmentLedgerService.java` | Provider/holding/transaction/evidence writes and aggregates |
| Background automation | `work/` | SMS, salary, reminders, SIP, digest, reconciliation, maintenance |

The resource source of truth for destinations and arguments is `app/src/main/res/navigation/nav_graph.xml`.

### Persistence and data exchange

| Type | Files | Responsibility |
|---|---|---|
| Entities | `data/entity/*.java` | Active table definitions |
| DAOs | `data/dao/*.java` | SQL queries and useful synchronous/background operations |
| POJOs | `data/pojo/*.java` | Aggregate and joined query results |
| Repositories | `data/repository/*.java` | Expense/planning data access and background execution |
| Backup | `data/DataBackupManager.java` | **Partial** JSON format 3 import/export |
| CSV / PDF export | `data/export/` | Report generation |

`Investment` and `InvestmentSource` are legacy, unregistered Java entities. They are not active Room tables.

---

## 💰 Expense and Planning Flow

```text
┌─────────────────────┐     ┌──────────────────────┐
│ Expense editor / UI │     │ SMS / notification   │
│ add or adjust entry │     │ Salary candidate     │
└──────────┬──────────┘     └───────────┬──────────┘
           │                            ▼
           ▼                   EventIngestionPipeline
 ExpenseEditorViewModel                  │
           │                            ▼
           ▼                  SalaryAllocationOrchestrator
    ExpenseRepository                    │
           │                            ▼
           ▼                   Bucket.currentBalanceCents
       ExpenseDao                     (planned allocation)
           │
           ▼
 AppDatabase / Room ────────────────────────────────────────────┐
                                                               │
                                                               ▼
                                                  Holding + InvestmentTransaction
                                                   (actual investment ledger)
```

Consumption analytics exclude `Expense.isInvestmentTransfer = 1`, while Calendar and merchant history can retain the corresponding cash movements. Planned bucket balances and expected SIP occurrences are **not** market value or confirmed net worth.

---

## 📨 Investment Evidence Flow

```text
┌─────────────────────────────────────────────────────────────────────────┐
│ SOURCES                                                                 │
│     SMS                Notifications             Expense recovery       │
└───────────┬───────────────────┬───────────────────────────┬─────────────┘
            └───────────────────┼───────────────────────────┘
                                ▼
                  InvestmentUnderstandingEngine
              correction memory / deterministic parsing
              interpretation and review gating
                                │
                                ▼
                    ParsedInvestmentSignal
                                │
                                ▼
                 InvestmentAutoIngestionEngine
                                │
                                ▼
         ┌──────────────────────────────────────────────────────┐
         │ @Transaction: InvestmentLedgerService.processEvidence│
         │                                                      │
         │ Provider → Holding → InvestmentTransaction           │
         │                       │                              │
         │ IngestionEvent → InvestmentEventLink                 │
         │                       │                              │
         │            confirmed transactions only               │
         │                       ▼                              │
         │        recomputeHoldingAggregates(...)               │
         └──────────────────────┬───────────────────────────────┘
                                ▼
                       Materialized Holding aggregates
```

- Parsers return signals; they do not own ledger writes.
- Evidence can create pending transactions or be confirmed by later correlated evidence. Only confirmed transactions count toward holding aggregates.
- `BucketAssignmentEngine` prioritizes learned mappings, on-device embeddings, and deterministic fallback. Uncertain assignments await confirmation in Unassigned.
- `OccurrenceReconciliationEngine` connects expected SIP occurrences to confirmed activity. Expected rows remain planning artifacts.
- `InvestmentMaintenanceWorker` recomputes aggregates, removes orphan event links, and repairs stale occurrence links.

See [Investment ledger module](modules/investment-ledger.md) for detailed semantics and limitations.

---

## 🔄 Data Flow Examples

### Adding an expense

```text
User completes the expense form → Save
                  │
                  ▼
ExpenseEditorDialogFragment.setupButtons()
                  │
                  ▼
viewModel.setAmountFromString(...), setDescription(...), ...
                  │
                  ▼
ExpenseEditorViewModel.saveExpense()
                  │
                  ▼
validate() → amount and required category checks
                  │
                  ▼
AppDatabase.databaseWriteExecutor — background work
                  │
          ┌───────┴────────┐
          ▼                ▼
merchantRepository     expenseRepository
.insertOrGetId()       .insert(expense)
          │                │
          ▼                ▼
   MerchantDao         ExpenseDao → Room → SQLite
                           │
                           ▼
             LiveData observers update visible UI
```

Related tags, accounts, media, and editing/deleting cases follow the editor/ViewModel paths rather than being separate write pipelines.

### Selecting the next dashboard month

```text
User taps Next Month
         │
         ▼
DashboardFragment.binding.btnNextMonth.onClick
         │
         ▼
DashboardViewModel.goToNextMonth()
         │
         ▼
currentMonth.setValue(current.plusMonths(1))
         │
         ▼
Transformations.switchMap(currentMonth, ...)
         │
         ├── month epoch-day range → consumption-only expense queries
         └── YYYYMM month key + epoch-day range → budget queries
         │
         ▼
LiveData delivers the new totals / categories / budget progress
         │
         ▼
DashboardFragment observers update text and invalidate charts
```

---

## ⚙️ Background Work

| Component | Responsibility |
|---|---|
| `SmsAutoImportWorker` | Incremental expense SMS import and source repair |
| `SalaryAutomationWorker` | Salary detection/fallback and planned allocations |
| `InvestmentScheduleWorker` | SIP occurrence materialization and catch-up |
| `InvestmentDigestWorker` | Daily action-oriented investment digest |
| `InvestmentMaintenanceWorker` | Aggregate/link/occurrence maintenance; immediate and periodic runs |
| `RecurringExpenseWorker` / `ReminderWorker` | Recurring entries and due reminders |

Workers can receive duplicate deliveries. Preserve `IngestionEvent.sourceKey` idempotency and route investment evidence through the ledger boundary.

---

## 🔍 Troubleshooting by Symptom

| Symptom | First files | What to inspect |
|---|---|---|
| App crashes on launch | `SpendWiseApplication.java`, manifest, `values*/themes.xml` | ThreeTen initialization, application declaration, Material DayNight theme |
| Expense totals are wrong | `DashboardViewModel.java`, `ExpenseDao.java`, `DashboardFragment.java` | Month epoch-day range, consumption-only queries, chart bindings |
| Calendar dates are wrong | `DateUtils.java`, `CalendarFragment.java` | `org.threeten.bp.LocalDate`, epoch conversion |
| Expense will not save | `ExpenseEditorDialogFragment.java`, `ExpenseEditorViewModel.java`, `ExpenseRepository.java` | Required category, `validate()`, background insertion |
| Budget progress is wrong | `CategoriesViewModel.java`, `BudgetDao.java` | `YYYYMM`, category ID (not name), consumption-only join |
| Theme/night-mode issue | `values/themes.xml`, `values-night/themes.xml`, version-qualified resources | Dark-theme colors and API-gated status/navigation attributes |
| Import/export seems incomplete | `SettingsFragment.java`, `DataBackupManager.java` | SAF URIs, JSON version 3 fields, merge semantics and omitted data |
| Duplicate investment evidence | `EventIngestionPipeline.java`, `InvestmentEventLink.java`, ledger tests | `sourceKey`, correlation key, event-link uniqueness |
| Wrong holding total | `InvestmentLedgerService.java`, `InvestmentTransaction.java` | Type, correlation, **confirmed-only status**, aggregate recomputation |
| SIP inflates net worth | `OccurrenceReconciliationEngine.java`, occurrence tables | Confirmed linkage; expected occurrences must not count |
| Wrong bucket assignment | `BucketAssignmentEngine.java`, `InstrumentBucketMapping.java` | Learned mapping, embedding availability/fallback, Unassigned review |
| Voice behaves differently | Voice parser/understanding classes and installed speech provider | Permission, provider, confidence, separately supplied Gemma weights |
| Existing user loses access/data after upgrade | `AppDatabase.java`, Room schema JSON, migration tests | Missing 9→10 migration; release has no destructive fallback |

See [Troubleshooting](troubleshooting.md) and [Data layer](modules/data-layer.md) for deeper diagnostics.

---

## 🔒 Release-Critical Boundaries

- Instrumentation currently covers migration 3→4 only. No full production-upgrade proof exists.
- Exported SAF documents are not encrypted by SpendWise; raw SMS/notification source evidence may be sensitive.
- There is no `INTERNET` manifest permission or SpendWise backend. Experimental P2P/federated-sync code is not a production synchronization system.
- Speech audio can be processed by Android's installed recognition provider. Subsequent text parsing is local; optional Gemma weights are supplied separately and are not bundled.

See [Release smoke checklist](release-smoke-checklist.md).

---

## 📅 Version History

Detailed milestones and validation belong in [progress-log.md](progress-log.md); older results are not proof that this working tree was tested.

| Version / change | Key architectural additions |
|---|---|
| v0.1–v0.5 | Room data layer, repositories, primary screens, import/export, smart helpers |
| v1.0–v1.1 | Calendar decorators, Quick Tiles, recurring expenses, reminders, UX fixes |
| v1.2 | Tags, accounts, goals, analytics, SMS reader |
| v1.3–v1.6 | Buckets, investment ingestion/reconciliation, dual planned/actual ledgers, review work |
| v1.7–v1.9 | Investment ledger boundary, understanding/correction flow, maintenance worker, on-device sorting |

## Last Updated

2026-11-10 — Restored readable diagrams and component tables while updating navigation, evidence boundaries, and schema-10 release limitations.
