# Dashboard Module

> **Purpose**: Explains how the Dashboard screen works.

---

## 📌 Status: NOT YET IMPLEMENTED

This module will be implemented in Phase 3.

---

## 🎯 What This Screen Does

The Dashboard is the **home screen** of the app. It shows:
- **Monthly total** - How much you spent this month
- **Category pie chart** - Visual breakdown by category
- **Budget progress bars** - How much of each budget is used
- **Top 5 categories** - List of biggest spending areas
- **Month navigation** - Switch between months

---

## 🖼️ Screen Layout (Planned)

```
┌─────────────────────────────────┐
│  ← January 2026 →               │  ← Month navigation
├─────────────────────────────────┤
│  Total Spent                    │
│  ₹45,230                        │  ← Monthly total
├─────────────────────────────────┤
│      ┌───┐                      │
│     /     \   Food 40%          │
│    │       │  Transport 25%     │  ← Pie chart
│     \     /   Shopping 20%      │
│      └───┘    Other 15%         │
├─────────────────────────────────┤
│  Budget Progress                │
│  Food      ████████░░  80%      │  ← Bar chart
│  Transport ██████░░░░  60%      │
├─────────────────────────────────┤
│  Top Categories                 │
│  1. Food         ₹18,000        │  ← Top 5 list
│  2. Transport    ₹11,300        │
│  ...                            │
└─────────────────────────────────┘
```

---

## 🔄 How It Will Work

1. Screen loads → ViewModel fetches current month's data
2. ViewModel queries expenses grouped by category
3. Charts are populated with data
4. User taps ← or → → Month changes, data refreshes

---

## 📁 Files (After Implementation)

| File | Purpose |
|------|---------|
| `dashboard/DashboardFragment.java` | UI logic and chart setup |
| `dashboard/DashboardViewModel.java` | Data loading and month tracking |
| `res/layout/fragment_dashboard.xml` | Screen layout |

---

## 📅 Last Updated
2026-01-22 - Initial placeholder created
