# Categories & Budgets Module

> **Purpose**: Explains how categories and budget tracking work.

---

## 📌 Status: ✅ IMPLEMENTED + ENHANCED (v1.2.1)

---

## 🎯 What This Screen Does

Categories screen lets users:
- View all categories
- Add new custom categories from the FAB dialog
- Set/edit/remove monthly budgets per category
- See category-level progress bars (spent vs limit)
- See month-level budget summary (aggregate usage + over-budget count)
- Copy previous month budgets into current month
- Navigate budget month
- Use lower-emphasis outlined budget action buttons for cleaner hierarchy

---

## 🔄 How It Works

### Category + Budget Loading
1. `CategoriesViewModel` exposes all categories and `budgetMonth`.
2. `switchMap` on `budgetMonth` loads `getBudgetsWithSpend(monthKey, startDay, endDay)`.
3. `CategoryBudgetAdapter` binds per-category progress.

### Add Category Flow
1. User taps categories FAB.
2. Dialog validates category name (non-empty + no case-insensitive duplicates).
3. `viewModel.addCategory(...)` inserts category with deterministic palette color.

### Month Summary
- Fragment computes:
  - total spent across budgets
  - total limit across budgets
  - over-budget category count
- Summary card progress/color updates with the same thresholds as category rows.

### Copy Previous Month
1. User taps **Copy Previous Month Budgets**.
2. `viewModel.copyBudgetsFromPreviousMonth()` is called.
3. Repository only copies if current month has no budgets.

---

## 📁 Files

| File | Purpose |
|------|---------|
| `categories/CategoriesFragment.java` | UI, month nav, summary card, copy action |
| `categories/CategoriesViewModel.java` | Month state + budget operations |
| `categories/CategoryBudgetAdapter.java` | Category list with progress bars |
| `categories/BudgetEditorDialog.java` | Set/edit budget dialog |
| `fragment_categories.xml` | Screen layout |
| `item_category_budget.xml` | Category row |
| `data/dao/BudgetDao.java` | Budget queries and month copy query |
| `data/pojo/BudgetWithSpend.java` | Budget aggregate model with `categoryId` |

---

## 🐛 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| FAB does nothing | Dialog flow not wired | Check `CategoriesFragment.setupFab()` |
| Cannot add a category | Empty/duplicate name validation | Provide unique non-empty category name |
| Summary card shows empty | No budgets for month | Add budget rows via Set Budget |
| Copy action seems no-op | Current month already has budgets | Clear current month budgets first or edit existing |
| Row progress on wrong category | Mapping mismatch | Ensure adapter maps by `categoryId` |
| Progress > 100 not obvious | Over budget state | Check red indicator and over-budget count text |

---

## 📅 Last Updated
2026-02-14 - v1.2.1 add-category FAB flow documented
