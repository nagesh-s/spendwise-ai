# SpendWise AI - Complete Architecture Guide

> **Purpose**: Complete system overview for debugging and understanding. If something doesn't work, this guide tells you WHERE to look.

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        UI LAYER                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │Dashboard │ │Calendar  │ │Categories│ │Merchants │      │
│  │Fragment  │ │Fragment  │ │Fragment  │ │Fragment  │      │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘      │
│       │            │            │            │             │
│  ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐      │
│  │Dashboard │ │Calendar  │ │Categories│ │Merchants │      │
│  │ViewModel│ │ViewModel │ │ViewModel │ │ViewModel │      │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘      │
└───────┼────────────┼────────────┼────────────┼─────────────┘
        │            │            │            │
┌───────┼────────────┼────────────┼────────────┼─────────────┐
│       ▼            ▼            ▼            ▼             │
│                   REPOSITORY LAYER                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │ExpenseRepo  │ │CategoryRepo │ │MerchantRepo │          │
│  │BudgetRepo   │ │             │ │             │          │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘          │
└─────────┼───────────────┼───────────────┼──────────────────┘
          │               │               │
┌─────────┼───────────────┼───────────────┼──────────────────┐
│         ▼               ▼               ▼                  │
│                      DAO LAYER                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │ExpenseDao│ │CategoryDao│ │MerchantDao│ │BudgetDao │      │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘      │
└───────┼────────────┼────────────┼────────────┼─────────────┘
        │            │            │            │
        ▼            ▼            ▼            ▼
┌─────────────────────────────────────────────────────────────┐
│                    ROOM DATABASE                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │ expenses │ │categories│ │ merchants│ │ budgets  │      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘      │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 Complete File Map

### Core Entry Points

| File | What It Does | When It Runs |
|------|--------------|--------------|
| `MainActivity.java` | App entry, nav setup | App launch |
| `SpendWiseApplication.java` | ThreeTen init | Before anything |
| `AppDatabase.java` | DB singleton | First DB access |

### Screens (Fragments)

| Screen | Fragment | ViewModel | Layout |
|--------|----------|-----------|--------|
| Dashboard | `dashboard/DashboardFragment.java` | `DashboardViewModel.java` | `fragment_dashboard.xml` |
| Calendar | `calendar/CalendarFragment.java` | `CalendarViewModel.java` | `fragment_calendar.xml` |
| Categories | `categories/CategoriesFragment.java` | `CategoriesViewModel.java` | `fragment_categories.xml` |
| Merchants | `merchants/MerchantsFragment.java` | `MerchantsViewModel.java` | `fragment_merchants.xml` |
| Settings | `settings/SettingsFragment.java` | `SettingsViewModel.java` | `fragment_settings.xml` |

### Dialogs

| Dialog | Purpose | File |
|--------|---------|------|
| Expense Editor | Add/edit expenses | `common/ExpenseEditorDialogFragment.java` |
| Budget Editor | Set category budgets | `categories/BudgetEditorDialog.java` |

### Data Layer

| Type | Files | Purpose |
|------|-------|---------|
| Entities | `data/entity/*.java` | Table definitions |
| DAOs | `data/dao/*.java` | SQL queries |
| POJOs | `data/pojo/*.java` | Query results |
| Repositories | `data/repository/*.java` | Business logic |
| Backup | `data/DataBackupManager.java` | Import/export JSON |

---

## 🔍 Troubleshooting by Symptom

### App Crashes on Launch

| Check | File | What to Look For |
|-------|------|------------------|
| Theme issues | `values/themes.xml` | Must use `Theme.Material3` |
| ThreeTen init | `SpendWiseApplication.java` | `AndroidThreeTen.init(this)` in onCreate |
| Manifest | `AndroidManifest.xml` | `android:name=".SpendWiseApplication"` |

### Dashboard Shows Wrong Data

| Check | File | What to Look For |
|-------|------|------------------|
| Month range | `DashboardViewModel.java` | `getMonthStartEpochDay/getMonthEndEpochDay` |
| Query | `ExpenseDao.java` | `getCategoryTotalsForRange` query |
| Chart binding | `DashboardFragment.java` | `setupPieChart/updatePieChartData` |

### Calendar Dates Wrong

| Check | File | What to Look For |
|-------|------|------------------|
| ThreeTen imports | All files | Should use `org.threeten.bp.*` NOT `java.time.*` |
| Date conversion | `CalendarFragment.java` | `LocalDate.of(year, month, day)` |
| Epoch conversion | `DateUtils.java` | `epochDayToDate`/`toEpochDay` |

### Expense Not Saving

| Check | File | What to Look For |
|-------|------|------------------|
| Validation | `ExpenseEditorViewModel.java` | `validate()` method |
| Category selected | `ExpenseEditorDialogFragment.java` | `selectedCategoryId` must be > 0 |
| DB write | `ExpenseRepository.java` | `insert()` uses executor |

### Budget Progress Wrong

| Check | File | What to Look For |
|-------|------|------------------|
| Month key | `CategoriesViewModel.java` | Format must be "YYYYMM" |
| Join query | `BudgetDao.java` | `getBudgetsWithSpend` query |
| Progress calc | `CategoryBudgetAdapter.java` | `getUsagePercent()` |

### Dark Mode Issues

| Check | File | What to Look For |
|-------|------|------------------|
| Theme | `values-night/themes.xml` | Must have dark colors defined |
| Colors | `values-night/colors.xml` | `md_theme_dark_*` colors |
| Icons | `drawable/*.xml` | No hardcoded `#000000` fillColor |
| Calendar | `themes.xml` | `CalendarDateTextAppearance` style |

### Import/Export Fails

| Check | File | What to Look For |
|-------|------|------------------|
| SAF intent | `SettingsFragment.java` | `ACTION_CREATE_DOCUMENT/OPEN_DOCUMENT` |
| JSON structure | `DataBackupManager.java` | `exportData/importData` methods |
| DAO sync methods | `*Dao.java` | Need `*Sync()` methods (not LiveData) |

---

## 🔄 Data Flow Examples

### Adding Expense Flow

```
User fills form → Click Save
         ↓
ExpenseEditorDialogFragment.setupButtons()
         ↓
viewModel.setAmountFromString()/setDescription()/etc
         ↓
viewModel.saveExpense()
         ↓
ExpenseEditorViewModel.validate() → checks amount > 0, category selected
         ↓
merchantRepository.insertOrGetId() → creates merchant if new
         ↓
expenseRepository.insert(expense)
         ↓
ExpenseRepository → databaseWriteExecutor.execute()
         ↓
ExpenseDao.insert() → Room writes to SQLite
         ↓
LiveData observers → UI updates automatically
```

### Month Navigation Flow

```
User clicks next month →
         ↓
DashboardFragment.btnNextMonth.onClick
         ↓
viewModel.goToNextMonth()
         ↓
DashboardViewModel.currentMonth.setValue(month.plusMonths(1))
         ↓
Transformations.switchMap triggers
         ↓
New date range calculated → DAO queries re-execute
         ↓
LiveData delivers new data
         ↓
Fragment observers update UI
```

---

## 📅 Version History

| Version | Key Changes | Date |
|---------|-------------|------|
| v0.0.1 | Navigation scaffold | 01-22 |
| v0.1.0 | Data layer (Room, DAOs) | 01-22 |
| v0.2.0 | Repositories, ViewModels | 01-22 |
| v0.3.0 | All 5 screens | 01-22 |
| v0.3.2 | Theme fixes, budget dialog | 01-23 |
| v0.4.0 | Import/Export JSON | 01-23 |
| v0.5.0 | Smart features | 01-23 |
| v1.0.0 | Calendar decorators, final | 01-23 |

---

## 📅 Last Updated
2026-01-23 - v1.0.0 Complete
