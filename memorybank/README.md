# SpendWise AI — Android Expense Tracker

SpendWise AI is a modern, offline-first personal expense tracker built in Java for Android, designed around a single-activity, multi-fragment architecture with local persistence, rich charts, an interactive calendar, and privacy-preserving JSON import/export via the Storage Access Framework.

SpendWise AI emphasizes responsive Material design with dark mode support and clean information architecture to minimize friction in daily expense logging and monthly reviews.

## Key features
- Dashboard: monthly spend, category distribution, budget progress, top categories, budget health card, and spending pace insights (avg/day + projected total).
- AI insights + predictions: anomaly/spike cards, budget risk alerts, and month-end forecasts generated fully on-device.
- Voice expense entry: mic-to-text parsing with amount/category/merchant prefill into the expense editor.
- Calendar: interactive month view with per-day totals, spending decorators, selected-day expense list, and one-tap jump to today.
- Categories & Budgets: default non-deletable categories, budget progress per category, monthly summary card, and copy-previous-month budgets action.
- Merchant analytics: top merchants by spend with ranking badges plus monthly merchant summary (total spend + top merchant).
- Settings: dark mode toggle, JSON export/import, and CSV/PDF report export through the Android Storage Access Framework.
- Smart input: inline suggestions for category and merchant driven by local history, edit-mode prefill reliability, and quick-add tiles for frequent items.
- Automation features: recurring expenses, bill reminders, QuickTiles home screen widget, biometric app lock, and receipt photo attachments.
- Engagement features: profile screen with XP/level progress, achievements, and first-run onboarding flow.

## Architecture overview
- Single-activity with BottomNavigationView hosting a NavHostFragment that navigates between Dashboard, Calendar, Categories & Budgets, Merchant Analytics, and Settings fragments for a cohesive “single-page” feel with proper back-stack handling.
- Room provides the local database layer with entities, DAOs, and compile‑time query validation to ensure robust aggregates and fast offline workflows.
- Domain engines (`InsightsEngine`, `PredictionEngine`, `GamificationEngine`) compute intelligence and engagement features on-device.

## Tech stack
- Language/UI: Java + Android Views/XML with Material Components for consistent theming and accessibility across light/dark modes.
- Persistence: Room for entities, DAOs, indices, migrations, and observable data driving UI updates reliably offline.
- Navigation: Jetpack Navigation Component connects bottom navigation and fragments with actions and type‑safe arguments.
- Charts: MPAndroidChart for native PieChart and BarChart with value formatters and animations suitable for dashboards and analytics.
- Calendar: MaterialCalendarView for month display, decorators, and selection callbacks to open expense modals.
- Files: Android Storage Access Framework for compliant JSON export/import using system document pickers and URIs.
- Icons: Font Awesome via TTF for offline glyphs in bottom navigation, lists, and category badges.

## Data model
- Expense(id, dateEpochDay, amountCents, description, categoryId, merchantId, photoPath) captures atomic spend records and optional receipts for monthly/daily aggregations.
- Category(id, name, colorArgb, iconKey, isDefault) defines visual identity and protects default system categories from deletion in UI logic.
- Merchant(id, name) enables merchant-level analytics and suggestions from local history without network calls.
- Budget(id, monthKeyYYYYMM, categoryId, limitCents) stores per-category monthly budgets to compute progress and thresholds.
- QuickTile(id, categoryId, merchantId, label, amountCents) accelerates entry via one‑tap insertion of frequent tuples.

## Getting started
- Prerequisites: Recent Android Studio, Android SDKs, and an emulator or device with recommended minSdk per project configuration outlined in docs and Gradle files.
- Setup: Clone the repository, open in Android Studio, let Gradle sync, then run on an emulator or device to verify baseline flows and dark mode.
- Build: Follow standard Android Studio build steps to produce debug builds for testing and sign release builds when preparing distribution artifacts per team process.

## Screens and flows
- Dashboard: uses Room aggregates to render totals and category distributions in MPAndroidChart with smooth dataset updates on month switches.
- Dashboard AI: renders insight and prediction card rails, plus voice-input and profile entry points.
- Calendar: MaterialCalendarView decorators reflect spend tiers per day, with an on-click modal dialog for view/add/edit of day-specific expenses.
- Categories & Budgets: category management with Font Awesome glyphs and budget bars for current/selected month.
- Merchants: list and detail screens with totals, averages, counts, and recent expenses with charts.
- Settings: dark mode, feature shortcuts, JSON backup/restore, and CSV/PDF report export.
- Onboarding: first-run pager introducing insights and voice workflows before handing off to main app.

## Privacy and data
- All data is stored locally in Room; export/import is user-initiated via SAF with no broad storage permissions required on modern Android.
- No cloud services are required for core functionality, maintaining user privacy and offline reliability.

## Documentation map
- Architecture, data model, charts, calendar, and import/export are further detailed in the internal docs to support onboarding and feature ownership handoffs.
- The README remains a concise high-level overview and entry point, linking to deeper documents when needed to avoid duplication and drift.

## Contributing and roadmap
- Contribution guidelines follow standard README best practices: how to run tests locally, code style, and PR expectations to maintain quality and velocity.
- The roadmap references the sprint plan and acceptance criteria maintained in the documentation set to keep delivery predictable and testable across milestones.
