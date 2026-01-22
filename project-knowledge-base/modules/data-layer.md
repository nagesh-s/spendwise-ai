# Data Layer Module

> **Purpose**: Explains how data is stored and accessed in the app.

---

## 📌 Status: ✅ IMPLEMENTED (Phase 1 Complete)

---

## 🎯 What This Module Does

The data layer is the foundation of the app. It handles:
- **Storing** expenses, categories, merchants, and budgets in a local SQLite database via Room
- **Retrieving** data for display on screens
- **Calculating** totals, averages, and aggregates

---

## 📊 Data Entities (Tables)

| Entity | Purpose | Key Fields |
|--------|---------|------------|
| **Expense** | Individual spending records | amount (cents), date (epochDay), category, merchant, description |
| **Category** | Expense categories (Food, Transport, etc.) | name, color (ARGB), icon key, isDefault flag |
| **Merchant** | Where money was spent (Amazon, Starbucks) | name (unique) |
| **Budget** | Monthly spending limits per category | monthKey (YYYYMM), category, limit (cents) |
| **QuickTile** | Frequently used expense templates | label, amount, category, merchant |

---

## 🔄 How It Works

### Adding an Expense
1. User enters expense details in ExpenseEditorDialog
2. Fragment calls `ExpenseRepository.insert(expense)`
3. Repository runs insert on background thread via `AppDatabase.databaseWriteExecutor`
4. Room inserts into `expenses` table
5. LiveData observers in UI automatically update

### Viewing Dashboard Totals
1. DashboardFragment observes `ExpenseDao.getCategoryTotalsForRange(startDay, endDay)`
2. Room executes the aggregate query joining expenses with categories
3. Returns `List<CategoryTotal>` with name, total, and color
4. Fragment updates PieChart with the data

### Budget Progress
1. CategoriesFragment observes `BudgetDao.getBudgetsWithSpend(monthKey, startDay, endDay)`
2. Room joins budgets with expenses summed by category
3. Returns `List<BudgetWithSpend>` with limit and spent amounts
4. Fragment calculates percentage and displays progress bars

---

## 📁 Files

| File | Purpose |
|------|---------|
| **Entities** | |
| [`Expense.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/entity/Expense.java) | Expense table: id, dateEpochDay, amountCents, description, categoryId, merchantId |
| [`Category.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/entity/Category.java) | Category table: id, name, colorArgb, iconKey, isDefault |
| [`Merchant.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/entity/Merchant.java) | Merchant table: id, name |
| [`Budget.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/entity/Budget.java) | Budget table: id, monthKeyYYYYMM, categoryId, limitCents |
| [`QuickTile.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/entity/QuickTile.java) | QuickTile table: id, categoryId, merchantId, label, amountCents |
| **DAOs** | |
| [`ExpenseDao.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/dao/ExpenseDao.java) | CRUD + getTotalForRange, getCategoryTotals, getDailyTotals |
| [`CategoryDao.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/dao/CategoryDao.java) | CRUD + getAllCategories, searchByName |
| [`MerchantDao.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/dao/MerchantDao.java) | CRUD + getTopMerchants, searchByName |
| [`BudgetDao.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/dao/BudgetDao.java) | CRUD + getBudgetsWithSpend, copyBudgetsToNewMonth |
| [`QuickTileDao.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/dao/QuickTileDao.java) | CRUD for quick-add tiles |
| **POJOs** | |
| [`CategoryTotal.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/pojo/CategoryTotal.java) | Query result: name, total, color |
| [`MerchantAgg.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/pojo/MerchantAgg.java) | Query result: name, txnCount, total, avg |
| [`DailyTotal.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/pojo/DailyTotal.java) | Query result: epochDay, totalCents |
| [`BudgetWithSpend.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/pojo/BudgetWithSpend.java) | Query result: budget + actual spent |
| **Database** | |
| [`AppDatabase.java`](file:///Users/nagesh.s/dev/Personal/SpendWise/app/src/main/java/com/spendwise/ai/data/AppDatabase.java) | Room DB config, singleton, prepopulates 10 default categories |

---

## 🏷️ Default Categories (Prepopulated)

| Category | Color | Icon |
|----------|-------|------|
| Food & Dining | Orange | fa_utensils |
| Transportation | Blue | fa_car |
| Shopping | Purple | fa_shopping_bag |
| Entertainment | Pink | fa_film |
| Bills & Utilities | Gray | fa_file_invoice |
| Health & Fitness | Green | fa_heartbeat |
| Travel | Cyan | fa_plane |
| Education | Indigo | fa_graduation_cap |
| Personal Care | Amber | fa_spa |
| Other | Brown | fa_ellipsis_h |

---

## 💡 Key Design Decisions

1. **Amounts in cents**: Stored as `long` to avoid floating-point precision issues
2. **Dates as epochDay**: Efficient for range queries and storage
3. **Foreign keys**: Ensure referential integrity between tables
4. **Indices**: On dateEpochDay, categoryId, merchantId for fast queries
5. **Background execution**: All DB writes via `databaseWriteExecutor`
6. **LiveData**: For automatic UI updates when data changes

---

## 📅 Last Updated
2026-01-22 - Phase 1 implementation complete
