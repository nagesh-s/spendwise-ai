# Data Layer Module

> **Purpose**: Explains how data is stored and accessed in the app.

---

## 📌 Status: ✅ IMPLEMENTED + TESTED BASELINE (Phase 1 + v1.2.x extensions)

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
| `Tag` | User-defined labels for expenses | `name`, `colorArgb` |
| `ExpenseTag` | Join table for expense-tag many-to-many | `expenseId`, `tagId` |
| `Account` | Spend source account | `name`, `type`, `openingBalanceCents`, `isDefault` |
| `FinancialGoal` | Savings/limit/debt goal tracking | `title`, `targetCents`, `currentCents`, `goalType` |

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
| Entities | `data/entity/Expense.java`, `data/entity/Category.java`, `data/entity/Merchant.java`, `data/entity/Budget.java`, `data/entity/QuickTile.java`, `data/entity/RecurringExpense.java`, `data/entity/Tag.java`, `data/entity/ExpenseTag.java`, `data/entity/Account.java`, `data/entity/FinancialGoal.java` |
| DAOs | `data/dao/ExpenseDao.java`, `data/dao/CategoryDao.java`, `data/dao/MerchantDao.java`, `data/dao/BudgetDao.java`, `data/dao/QuickTileDao.java`, `data/dao/RecurringExpenseDao.java`, `data/dao/TagDao.java`, `data/dao/AccountDao.java`, `data/dao/FinancialGoalDao.java` |
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
6. SMS import safety now uses DAO duplicate checks to avoid double inserts.
7. Budget aggregate query has sync and LiveData variants for UI and instrumentation tests.

---

## 🧪 Test Coverage Baseline (v1.2.2)

- `AppDatabaseMigrationTest` validates migration `3→4`:
  - creates `tags`, `expense_tags`, `accounts`, `financial_goals`
  - adds `accountId` column to `expenses`
  - preserves existing expense rows
  - seeds default Cash account

- `ExpenseEditorViewModelSaveTest` validates save flow writes:
  - expense row
  - merchant lookup/creation
  - account linkage
  - expense-tag linkage

- `CategoryBudgetIntegrityTest` validates:
  - default categories are protected from deletion
  - custom categories can be removed
  - month-scoped budget spend joins map correctly by `categoryId`

---

## 📅 Last Updated
2026-02-14 - v1.2.2 CI/QA hardening updates (duplicate guard + budget sync query + integrity test)
