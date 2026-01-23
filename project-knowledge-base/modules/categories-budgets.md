# Categories & Budgets Module

> **Purpose**: Explains how categories and budget tracking work.

---

## 📌 Status: ✅ IMPLEMENTED (v1.0.0)

---

## 🎯 What This Screen Does

Categories screen lets users:
- View all expense categories
- Set/edit monthly budgets per category
- See budget progress (spent vs limit)
- Navigate between months

---

## 🔄 How It Works

### Viewing Categories
1. `CategoriesFragment` loads `getAllCategories()` from ViewModel
2. `CategoryBudgetAdapter` displays categories with budget progress
3. Progress bars show % of budget used (green/orange/red)

### Setting Budget
1. User taps "Set Budget" button on a category
2. Opens `BudgetEditorDialog` with category name
3. User enters monthly limit amount
4. `viewModel.setBudget(categoryId, amountCents)` called
5. Budget saved for current month's monthKey (YYYYMM format)

### Budget Progress Calculation
1. ViewModel calls `getBudgetsWithSpend(monthKey, startDay, endDay)`
2. DAO joins budgets with expenses, sums spending per category
3. Returns `BudgetWithSpend` with limit and spent amounts
4. Adapter calculates `(spent / limit) * 100` for progress bar

---

## 📁 Files

| File | Purpose |
|------|---------|
| `categories/CategoriesFragment.java` | UI, month navigation |
| `categories/CategoriesViewModel.java` | Data loading, budget operations |
| `categories/CategoryBudgetAdapter.java` | RecyclerView with progress bars |
| `categories/BudgetEditorDialog.java` | Dialog for setting budgets |
| `fragment_categories.xml` | Screen layout |
| `item_category_budget.xml` | Category list item |
| `dialog_budget_editor.xml` | Budget dialog layout |
| `data/entity/Budget.java` | Budget table entity |
| `data/dao/BudgetDao.java` | CRUD + getBudgetsWithSpend |

---

## 🎨 Budget Progress Colors

| Color | Usage Level | Meaning |
|-------|-------------|---------|
| 🟢 Green | < 80% | On track |
| 🟠 Orange | 80-100% | Warning |
| 🔴 Red | > 100% | Over budget |

Colors set in `CategoryBudgetAdapter.ViewHolder.bind()`.

---

## 🐛 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Budget not saving | Dialog not calling ViewModel | Check BudgetEditorDialog.onBudgetSaved |
| Progress shows 0% | No expenses for month | Add expenses in that category |
| Wrong month budget | monthKey mismatch | Check YYYYMM format in DateUtils |

---

## 📅 Last Updated
2026-01-23 - v1.0.0 Complete
