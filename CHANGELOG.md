# Changelog

All notable changes to SpendWise AI will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
### Planned - Phase 2
- Auto Transaction Reader (SMS + Notifications)
- Biometric Lock (App Security)
- Auto Transaction Reader (SMS + Notifications)

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
