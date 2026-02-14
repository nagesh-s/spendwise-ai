# Changelog

All notable changes to SpendWise AI will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.6.0] - 2026-02-14
### Added - Phase 6 (Release Prep)
- **Onboarding Flow** - 3-slide introduction (Welcome, Insights, Voice) for first-time users (OnboardingActivity, ViewPager2).
- **App Icon** - New vector drawable icon features a wallet with growth graph.
- **Splash Screen** - Android 12+ splash screen support via themes.
- **Unit Tests** - `GamificationEngine` logic fully verified (XP, Leveling, Streaks).
- **Unit Tests** - `ReportGeneratorTest` for CSV export escaping/fallback behavior.
- **ProGuard Optimization** - Enabled minification for release builds with custom rules for Room/Gson/Charts.

### Fixed
- **Report Generator** - Fixed compilation errors by implementing synchronous Merchant/Category data fetching in SettingsViewModel.
- **Release Metadata** - Synced app version to `versionCode 160` and `versionName 1.6.0`.
- **Lint Blockers** - Resolved `MissingSuperCall` in lock screen back handling and telephony feature declaration warning for SMS permission.
- **Calendar Crash** - Fixed `MaterialCalendarView` inflate crash (`NoClassDefFoundError: android.support.v4.view.ViewPager$OnPageChangeListener`) by enabling AndroidX Jetifier compatibility.
- **Back Callback Warning** - Enabled `android:enableOnBackInvokedCallback="true"` in manifest application config.
- **Biometric Setup Routing** - Added API-level guard for biometric enrollment intent with secure fallback to system security settings.
- **Gamification Save UX** - Edit flow no longer shows incorrect `+10 XP` reward message.
- **Gamification Logic** - Streak and Early Bird badge unlock checks are now wired during new-expense save events.
- **Calendar Regression Coverage** - Added instrumentation smoke test to ensure calendar destination inflates without runtime crash.

---

## [1.5.0] - 2026-02-14
### Added - Phase 4 & 5 (Complete)
- **Gamification System** - Earn XP for logging expenses! Track progress with Levels and unlockable Badges (e.g., First Step, Streak Master).
- **Profile Screen** - New profile view (Star icon on Dashboard) showing Level, XP, and Achievement collection.
- **Export Reports** - Export your spending data as **CSV** (Spreadsheet) or **PDF** (Printable Report) from Settings.
- **Save Feedback** - Immediate visual feedback (Toasts) when saving expenses shows XP gained and badges unlocked.

### Technical
- **GamificationEngine** - Core logic for XP calculation, leveling (sqrt formula), and achievement conditions.
- **ReportGenerator** - Native PDF generation using `android.graphics.pdf` and CSV string building.
- **ProfileFragment** - New UI destination in `nav_graph`.
- **Dashboard Integration** - Header Profile button for quick access.

---

## [1.4.0] - 2026-02-14
### Added
- **Voice Input for Expenses** - Tap the mic on Dashboard to add expenses by speaking (e.g., "500 for lunch at cafe").
- **Smart Parsing** - Automatically extracts amount, category, and merchant from speech.

---

## [1.3.0] - 2026-02-14
### Added - Phase 3 (Partial)
- **AI-Powered Insights** - Dashboard cards for spending spikes, budget alerts, streaks, and month comparisons (InsightsEngine, Insight entities)
- **Smart Predictions** - Month-end spending forecasts and budget risk alerts (PredictionEngine, Prediction entities)
- **Dashboard Integration** - Horizontal scrollable insight and prediction cards integrated into the main dashboard

### Technical
- **InsightsEngine** - On-device SQL aggregation for generating insights
- **PredictionEngine** - Daily run-rate extrapolation logic for forecasting
- **New Pattern** - `Engine` classes for domain logic separation from ViewModels
- **UI Entry Points** - Verified reachability via Dashboard

---

## [1.2.2] - 2026-02-14
### Added
- **Android CI Workflow** - New GitHub Actions gate for `assembleDebug`, `testDebugUnitTest`, and `assembleDebugAndroidTest`
- **SMS Parser Unit Tests** - Coverage for transaction detection and parse extraction in `TransactionParserTest`
- **DB Migration Instrumentation Test** - Verifies migration `3→4` creates new tables and `expenses.accountId`
- **Expense Save Flow Instrumentation Test** - Verifies `ExpenseEditorViewModel.saveExpense()` persists expense, merchant, account, and tags
- **Expense Edit/Delete Instrumentation Coverage** - Regression checks for `initForEdit`, update-save, and delete flow
- **Category/Budget Integrity Instrumentation Coverage** - Validates default-category delete protection and budget spend mapping by `categoryId`
- **Release Smoke Checklist** - Added release runbook in `project-knowledge-base/release-smoke-checklist.md`

### Fixed
- Auto Transaction import now skips duplicates using exact match check (date, amount, category, description)
- SMS import failure path is retry-safe (item is not marked imported on failure)
- SMS permission UX now handles rationale and permanently-denied flows with app settings action
- SMS sender validation no longer treats generic `XX-*` headers as bank transaction senders
- `assembleDebug` duplicate-class failure resolved by excluding legacy `com.android.support` transitive deps from `material-calendarview`
- Category-budget instrumentation test stabilized against async default-category seeding

### Technical
- Added first test suites under:
  - `app/src/test/java/com/spendwise/ai/sms/`
  - `app/src/androidTest/java/com/spendwise/ai/data/`
  - `app/src/androidTest/java/com/spendwise/ai/common/`
- Added androidTest package:
  - `app/src/androidTest/java/com/spendwise/ai/categories/`
- Added CI file:
  - `.github/workflows/android-ci.yml`
- Release metadata prepared:
  - `versionCode 122`
  - `versionName 1.2.2`
- Reduced build/tooling noise by switching to Java 11 compile target and removing deprecated Gradle constraint flags
- Verification commands:
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:assembleDebugAndroidTest`
  - `./gradlew :app:connectedDebugAndroidTest`

---

## [1.2.1] - 2026-02-14
### Fixed
- Categories FAB now opens an **Add Category** dialog instead of a TODO placeholder
- Add Category flow now validates empty and duplicate names before insert
- Added user feedback toast after successful category creation
- Expense save path remains off main thread (stability follow-up)
- Settings shortcuts for new Phase 2 screens (Goals, Analytics, SMS Reader) are wired

### Changed
- Category color selection is now deterministic from app palette for newly created categories
- Cleaned stale `Unreleased` roadmap section (removed outdated Phase 2 placeholders)

### Technical
- Build verified with `./gradlew :app:assembleDebug`

---

## [1.2.0] - 2026-02-14
### Added - Phase 2 Complete
- **Tags & Custom Labels** - Flexible expense tagging beyond categories (Tag, ExpenseTag entities, TagDao, ChipGroup UI in expense editor)
- **Multi-Account Support** - Track spending across Cash, Bank, Credit Card, Wallet accounts (Account entity, dropdown in expense editor, default Cash seed)
- **Financial Goals Tracking** - Savings goals, spending limits, debt payoff targets with progress bars (FinancialGoal entity, GoalsFragment, add/update dialogs)
- **Advanced Analytics Dashboard** - Period-selectable analytics with spending trend LineChart, category PieChart, and top-5 breakdown (AnalyticsFragment, week/month/quarter/year)
- **Auto Transaction Reader (SMS)** - Regex-based Indian bank SMS parser (SBI, HDFC, ICICI, UPI, etc.) with one-tap import as expense

### Technical
- Database version 4 with explicit Migration(3→4): creates `tags`, `expense_tags`, `accounts`, `financial_goals` tables; adds `accountId` to `expenses`
- Default Cash account seeded during migration
- `READ_SMS` permission added to AndroidManifest
- New packages: `analytics/`, `goals/`, `sms/`
- Build verified with `./gradlew assembleDebug`

---

## [1.1.2] - 2026-02-08
### Added
- Quick Tiles usage hint text: tap to add expense, long-press to edit/delete
- Version-compatible bottom navigation checked-state background drawables

### Changed
- Rebalanced dark theme palette to teal-forward surfaces for calmer finance UX
- Unified card styling (surface container + outline) across categories, merchants, quick tiles, and recurring lists
- Switched budget action buttons (`Set Budget`, `Copy Previous Month Budgets`) to outlined style for better visual hierarchy
- Updated dashboard spend-highlight amounts (Today/This Week) to primary brand color for consistent emphasis
- Pie chart presentation now favors legend readability over crowded in-chart labels
- Add Expense dialog layout is now scrollable with stacked Date and Merchant fields

### Fixed
- Quick Tile tap now immediately inserts an expense and refreshes widget state
- Biometric lock toggle is no longer hard-disabled when enrollment is missing; users are guided to system setup
- Added biometric permissions to manifest for broader device compatibility
- Add Expense date text clipping in modal resolved (shorter format + single-line ellipsize)
- Replaced unsupported `BottomNavigationView` attribute to fix resource linking on older Material component versions

### Technical
- Build verified with `./gradlew :app:assembleDebug`

---

## [1.1.1] - 2026-02-08
### Added
- **Dashboard Budget Health Card** - usage, remaining/overshoot, and over-budget category count
- **Dashboard Spending Pace Insights** - average/day and projected monthly total cards
- **Calendar “Today” Jump Action** - one-tap return to current day and month
- **Categories Budget Summary** - monthly aggregate summary with progress indicator
- **Copy Previous Month Budgets Action** - one-tap carry-forward when current month has no budgets
- **Merchant Summary Card** - total merchant spend and top merchant highlight for selected month
- **QuickTiles & Recurring Screen Headers** - clearer context and empty-state guidance

### Changed
- Improved bottom navigation state styling for better tab affordance
- Updated text and empty-state messaging across key screens for better user guidance

### Fixed
- **Expense edit prefill flow** now reliably loads amount, date, category, description, and merchant
- Smart category keyword mapping now aligns with seeded default category names
- Budget-to-category mapping now uses `categoryId` instead of category name matching to avoid mismatches

### Technical
- Added `bottom_nav_item_colors.xml` for state-aware nav colors
- Extended budget aggregate query to return `categoryId` in `BudgetWithSpend`
- Added sync expense lookup in repository for edit-mode data loading

---

## [1.1.0] - 2026-02-08
### Added - Phase 1 Complete
- **QuickTiles Home Screen Widget** - One-tap expense creation from home screen
- **Recurring Expenses System** - Auto-generate daily/weekly/monthly/yearly expenses
- **Bill Reminders & Notifications** - WorkManager-based reminders with configurable lead time
- **Photo Attachments** - Attach and store receipt photos with expenses
- **Biometric Lock** - Fingerprint/face authentication with 5-minute timeout

### Technical
- Added `widget/`, `quicktiles/`, `recurring/`, `work/`, `notifications/`, `security/`, `util/` packages
- Added `RecurringExpense` entity, `photoPath` field to `Expense` entity
- Database version 3 with destructive migration
- Added WorkManager, Biometric dependencies
- Added FileProvider for photo storage
- Added POST_NOTIFICATIONS permission

---

## [Unreleased]
### Planned - Post v1.6
- Notification-based transaction parsing (beyond SMS inbox)
- Expanded test coverage for navigation flows and report export edge cases
- Budget anomaly nudges with actionable recommendations

---

## [0.3.0] - 2026-01-22
### Added
- **Dashboard Screen** - Monthly overview with PieChart and BarChart
- **Calendar Screen** - MaterialCalendarView with expense list per day
- **Expense Editor** - Dialog for add/edit with form validation
- **Categories Screen** - Category list with budget progress bars
- **Merchants Screen** - Top merchants with ranking badges
- **Settings Screen** - Dark mode toggle, export/import placeholders
- 4 new adapters: CategoryTotal, Expense, CategoryBudget, Merchant
- 10+ drawable resources and item layouts

### Notes
- Git tag: `v0.3.0`

---

## [0.2.0] - 2026-01-22
### Added
- **Repository Layer** - Clean data access abstraction
  - ExpenseRepository, CategoryRepository, MerchantRepository, BudgetRepository
- **ViewModel Layer** - MVVM architecture for all screens
  - DashboardViewModel with month navigation and reactive totals
  - CalendarViewModel with date selection and daily totals
  - CategoriesViewModel with category CRUD and budget management
  - MerchantsViewModel with analytics and merchant selection
  - SettingsViewModel with dark mode persistence
  - ExpenseEditorViewModel with form validation and save logic
- **DateUtils** - Helper for month keys, epoch days, and amount formatting

### Notes
- Git tag: `v0.2.0`

---

## [0.1.0] - 2026-01-22
### Added
- **Data Layer** - Complete Room database implementation
  - 5 Entities: Expense, Category, Merchant, Budget, QuickTile
  - 5 DAOs with CRUD and aggregate queries
  - 4 POJOs for query results
  - AppDatabase with singleton pattern and background executor
  - 10 default categories prepopulated on first run

### Notes
- Git tag: `v0.1.0`

---

## [0.0.1] - 2026-01-22
### Added
- Initial project setup with Android Gradle configuration
- MainActivity with BottomNavigationView (4 navigation tabs)
- Navigation graph with 5 fragment destinations
- Empty fragment shells: Dashboard, Calendar, Categories, Merchants, Settings
- ExpenseEditorDialogFragment placeholder
- Dependencies: Room 2.6.1, Navigation 2.7.7, MPAndroidChart v3.1.0, MaterialCalendarView 2.0.1
- Memory bank documentation (design specs)
- Project Knowledge Base structure for living documentation

### Notes
- This is the initial scaffold version
- No functional features yet - all fragments show placeholder text only
- Git tag: `v0.0.1`

---

## Version History Summary

| Version | Date | Milestone |
|---------|------|-----------|
| 0.0.1 | 2026-01-22 | Navigation scaffold |
| 0.1.0 | TBD | Data layer complete |
| 0.2.0 | TBD | Dashboard functional |
| 0.3.0 | TBD | Expense creation working |
| 1.0.0 | TBD | First production release |
| 1.1.0 | 2026-02-08 | QuickTiles, recurring expenses, reminders, biometric lock |
| 1.1.1 | 2026-02-08 | UX refinement + dashboard/category/merchant insights + edit-flow fixes |
| 1.1.2 | 2026-02-08 | UX polish + quick-tile action fix + biometric/setup + modal layout fixes |
| 1.2.0 | 2026-02-14 | Phase 2: tags, accounts, goals, analytics, SMS reader |
| 1.2.1 | 2026-02-14 | Stabilization: category add flow + settings wiring + changelog cleanup |
| 1.2.2 | 2026-02-14 | CI + regression tests + SMS hardening + release prep |
| 1.3.0 | 2026-02-14 | AI insights + smart predictions |
| 1.4.0 | 2026-02-14 | Voice input for expenses |
| 1.5.0 | 2026-02-14 | Export reports + gamification profile |
| 1.6.0 | 2026-02-14 | Release prep: onboarding + icon/splash + release hardening |
