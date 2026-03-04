- Single Activity: `MainActivity` hosts `NavHostFragment`; `BottomNavigationView` wired to `NavController`.
- Primary tabs: `DashboardFragment`, `CalendarFragment`, `BucketsFragment`, `MerchantsFragment`, `SettingsFragment`.
- Secondary flows: `RecurringExpensesFragment`, `QuickTilesFragment`, `GoalsFragment`, `AnalyticsFragment`, `AutoTransactionsFragment`, `ProfileFragment`.
- Dialog flows: `ExpenseEditorDialogFragment`, `RecurringExpenseEditorDialog`, `InvestmentScheduleEditorDialog`, bucket/investment editor dialogs.
- Data layer: Room entities + DAOs + repositories; LiveData observers drive reactive UI.

Background automation (WorkManager)
- `RecurringExpenseWorker`: materializes recurring expenses.
- `ReminderWorker`: reminders and due signals.
- `SmsAutoImportWorker`: incremental SMS import (expenses + salary/investment signals).
- `SalaryAutomationWorker`: expected-payday fallback salary allocation.
- `InvestmentScheduleWorker`: expected SIP occurrence generation + catch-up.
- `InvestmentDigestWorker`: action-based daily automation digest.

Salary-to-Buckets automation architecture
- Salary source detection:
  - SMS credits via `TransactionParser` + `SalaryDetector`
  - notification text via `InvestmentNotificationListenerService` + `TransactionParser` + `SalaryDetector`
- Canonical salary handling:
  - `SalaryAllocationOrchestrator` enforces month-level idempotency and allocation status.
  - `BucketSalaryPrefs` stores expected salary/payday, monthly allocation status/source/time, and detection state.
- Planned vs Actual ledger:
  - Planned ledger uses `Bucket.currentBalanceCents` (allocation plan).
  - Actual ledger uses confirmed holdings (`investments`) and drives net worth.

Investment ingestion and reconciliation pipeline
- Source parsers:
  - `InvestmentSmsParser` for SMS and notification text.
- Unified event registry:
  - `EventIngestionPipeline` writes `ingestion_events` with unique `sourceKey`.
  - Duplicate delivery from retries/restarts is ignored deterministically.
- Holding updates:
  - `InvestmentAutoIngestionEngine` inserts/updates confirmed holdings.
  - Supports debit/allotment/correction flows and keeps audit metadata on `Investment`.
- Expected SIP lifecycle:
  - `InvestmentScheduleEngine` generates `investment_occurrences` (24-month detailed cap + opening aggregate for older history).
  - `OccurrenceReconciliationEngine` links confirmed events to expected occurrences and marks `CONFIRMED` or `ADJUSTED`.

Bucket assignment strategy
- `BucketAssignmentEngine` order:
  1) learned mapping (`instrument_bucket_mappings`)
  2) on-device inference heuristics
  3) rules fallback
- Low-confidence paths mark map-pending and trigger one-time user prompt in bucket detail; chosen mapping is saved for reuse.

Gamification and digest
- `InvestmentGamificationEngine` is reward-only (no deductions), tracks investment XP, and also increments global profile XP.
- `InvestmentDigestWorker` sends one compact daily summary: salary allocation status, reconciled/pending SIPs, weighted progress, execution score.

Notification routing
- `NotificationHelper.showInvestmentDigestNotification` deep links with action `view_buckets`.
- `MainActivity` handles `view_buckets` and `view_recurring` intent actions for direct navigation.
