# Dashboard Module

> **Purpose**: Explains how the Dashboard screen works.

---

## 📌 Status: ✅ IMPLEMENTED + ENHANCED (v1.1.2)

---

## 🎯 What This Screen Does

Dashboard is the home analytics view. It shows:
- Monthly total spent
- Budget health summary (used vs limit, remaining/overshoot, over-budget count)
- Spending pace (average/day and projected month total)
- Category pie chart
- Budget progress bar chart
- Top spending categories list
- Month navigation

---

## 🔄 How It Works

### Data Loading
1. `DashboardViewModel` stores `currentMonth` (`YearMonth`).
2. Month changes trigger `switchMap` queries for:
   - monthly total
   - category totals
   - budgets with spend
3. Fragment observers update cards, charts, and lists reactively.

### Insight Cards
- Budget health is computed from `BudgetWithSpend` aggregates.
- Spending pace is computed from:
  - current month total
  - elapsed day count for current month
  - month length for projection

### Charts
- Pie chart: category distribution for selected month.
- Pie chart labels are intentionally de-crowded (legend-first) to avoid overlap on small screens.
- Horizontal bar chart: budget usage percentage with status colors.

---

## 📁 Files

| File | Purpose |
|------|---------|
| `dashboard/DashboardFragment.java` | Dashboard UI logic, observers, insight calculations |
| `dashboard/DashboardViewModel.java` | Month state + reactive data loading |
| `fragment_dashboard.xml` | Dashboard layout and cards |
| `dashboard/CategoryTotalAdapter.java` | Top category list |
| `data/dao/ExpenseDao.java` | Monthly total + category aggregate queries |
| `data/dao/BudgetDao.java` | Budget usage query |

---

## 🐛 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Projected total looks high/low | Elapsed day logic | Check `DashboardFragment.updateSpendingPace()` |
| Budget health empty | No budgets for selected month | Create budgets in Categories screen |
| Pie chart labels overlap | Dense category slices | Keep labels hidden and rely on legend readability |
| Chart not visible | Empty dataset | Verify month range and query result |
| Wrong budget mapping | Category linkage issue | Confirm `BudgetWithSpend` includes `categoryId` |

---

## 📅 Last Updated
2026-02-08 - v1.1.2 pie chart readability behavior documented
