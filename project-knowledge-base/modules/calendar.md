# Calendar Module

> **Purpose**: Explains how the Calendar screen works.

---

## 📌 Status: ✅ IMPLEMENTED + ENHANCED (v1.1.1)

---

## 🎯 What This Screen Does

Calendar screen provides:
- Month view with day decorators based on spending tiers
- Selected date header with daily total
- Expense list for selected day
- One-tap **Today** action to jump back to current date/month
- Add/edit flow through expense editor dialog

---

## 🔄 How It Works

### Viewing Calendar
1. `CalendarFragment` sets today as selected date.
2. ViewModel loads `dailyTotals` for displayed month.
3. Decorators are rebuilt from totals and applied to calendar.

### Selecting / Jumping Date
1. User taps day → `setSelectedDate()` in ViewModel.
2. Or user taps **Today** → calendar selects current day and month.
3. `selectedDateExpenses` LiveData updates list for date.

### Managing Expenses
1. FAB or empty-state button opens `ExpenseEditorDialogFragment`.
2. Save in dialog updates DB.
3. List and decorators refresh via LiveData observers.

---

## 📁 Files

| File | Purpose |
|------|---------|
| `calendar/CalendarFragment.java` | UI logic, observers, Today action |
| `calendar/CalendarViewModel.java` | Selected date, displayed month, totals |
| `calendar/SingleDayDecorator.java` | Dot color assignment per day |
| `calendar/SpendingDayDecorator.java` | Base decorator logic |
| `fragment_calendar.xml` | Calendar + selected day UI |
| `common/ExpenseAdapter.java` | Day expense list adapter |

---

## 🎨 Spending Dot Colors

| Color | Spending Level | Threshold |
|-------|----------------|-----------|
| 🟢 Green | Low | < ₹500 |
| 🟠 Orange | Medium | ₹500 - ₹2000 |
| 🔴 Red | High | > ₹2000 |

Thresholds are defined in `CalendarFragment.updateCalendarDecorators()`.

---

## 🐛 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Today button not jumping | Date/month sync issue | Check `jumpToToday()` implementation |
| Dots not showing | No data or decorators removed | Verify `dailyTotals` query output |
| Date list stale | Selected date not propagated | Check `setSelectedDate()` observer chain |

---

## 📅 Last Updated
2026-02-08 - v1.1.1 added Today quick-jump action
