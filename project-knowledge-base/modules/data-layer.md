# Data Layer Module

> **Purpose**: Explains how data is stored and accessed in the app.

---

## 📌 Status: ✅ IMPLEMENTED (Phase 1 + v1.1.x extensions)

---

## 🎯 What This Module Does

The data layer handles:
- Local persistence via Room (offline-first)
- CRUD operations for all business entities
- Aggregate analytics queries for dashboard/categories/merchants/calendar
- Backup/export and import compatibility
- Background-safe writes via shared executor

---

## 📊 Entities (Tables)

| Entity | Purpose | Key Fields |
|--------|---------|------------|
| `Expense` | Individual transactions | `dateEpochDay`, `amountCents`, `categoryId`, `merchantId`, `description`, `photoPath` |
| `Category` | Spend classification | `name`, `colorArgb`, `iconKey`, `isDefault` |
| `Merchant` | Merchant reference | `name` |
| `Budget` | Per-category monthly limit | `monthKeyYYYYMM`, `categoryId`, `limitCents` |
| `QuickTile` | One-tap expense preset | `label`, `amountCents`, `categoryId`, `merchantId` |
| `RecurringExpense` | Auto-generated scheduled expense | `frequency`, `dayOfPeriod`, `lastGeneratedEpochDay`, `reminderDaysBefore`, `isActive` |

---

## 🔄 Data Flow Patterns

### Write Path
Fragment/ViewModel → Repository → `AppDatabase.databaseWriteExecutor` → DAO write

### Read Path
DAO LiveData query → Repository passthrough → ViewModel exposure → Fragment observer

### Aggregate Path
Date range/month state in ViewModel → DAO aggregate query (`SUM/COUNT/GROUP BY`) → POJO result → UI chart/list

---

## 📁 Key Files

| Layer | Files |
|------|-------|
| Database | `data/AppDatabase.java` |
| Entities | `data/entity/Expense.java`, `data/entity/Category.java`, `data/entity/Merchant.java`, `data/entity/Budget.java`, `data/entity/QuickTile.java`, `data/entity/RecurringExpense.java` |
| DAOs | `data/dao/ExpenseDao.java`, `data/dao/CategoryDao.java`, `data/dao/MerchantDao.java`, `data/dao/BudgetDao.java`, `data/dao/QuickTileDao.java`, `data/dao/RecurringExpenseDao.java` |
| POJOs | `data/pojo/CategoryTotal.java`, `data/pojo/DailyTotal.java`, `data/pojo/MerchantAgg.java`, `data/pojo/BudgetWithSpend.java` |
| Repositories | `data/repository/ExpenseRepository.java`, `data/repository/CategoryRepository.java`, `data/repository/MerchantRepository.java`, `data/repository/BudgetRepository.java` |
| Backup | `data/DataBackupManager.java` |

---

## 💡 Design Notes

1. Amounts are stored in cents (`long`) to avoid floating-point errors.
2. Dates are stored as epoch day for cheap range queries.
3. Aggregates are computed in SQL, not in-memory loops, for scale.
4. Budget aggregate model now includes `categoryId` for stable mapping.
5. Default categories are seeded on database create/open when missing.

---

## 📅 Last Updated
2026-02-08 - v1.1.x data model and aggregate updates documented
