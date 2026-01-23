# Calendar Module

> **Purpose**: Explains how the Calendar screen works.

---

## 📌 Status: ✅ IMPLEMENTED (v1.0.0)

---

## 🎯 What This Screen Does

The Calendar shows a **month view** where users can:
- See which days have expenses (color-coded dots)
- Tap a day to view/add expenses for that date
- Navigate between months

---

## 🔄 How It Works

### Viewing Calendar
1. User opens Calendar tab
2. `CalendarFragment` initializes `MaterialCalendarView`
3. ViewModel loads `dailyTotals` for displayed month
4. `SingleDayDecorator` adds colored dots (green/orange/red based on spending)

### Selecting a Date
1. User taps a calendar day
2. `onDateChangedListener` fires
3. ViewModel sets `selectedDate`
4. `selectedDateExpenses` LiveData updates
5. RecyclerView shows expenses for that day

### Adding Expense
1. User taps FAB (+ button)
2. Opens `ExpenseEditorDialogFragment` with selected date pre-filled
3. After save, expense appears in list

---

## 📁 Files

| File | Purpose |
|------|---------|
| `calendar/CalendarFragment.java` | UI logic, calendar setup, observers |
| `calendar/CalendarViewModel.java` | Selected date, daily totals, expenses |
| `calendar/SingleDayDecorator.java` | Colored dots for spending levels |
| `calendar/SpendingDayDecorator.java` | Base decorator class |
| `fragment_calendar.xml` | Layout with MaterialCalendarView |
| `item_expense.xml` | Expense list item layout |
| `common/ExpenseAdapter.java` | RecyclerView adapter |

---

## 🎨 Spending Dot Colors

| Color | Spending Level | Threshold |
|-------|----------------|-----------|
| 🟢 Green | Low | < ₹500 |
| 🟠 Orange | Medium | ₹500 - ₹2000 |
| 🔴 Red | High | > ₹2000 |

Thresholds defined in `CalendarFragment.updateCalendarDecorators()`.

---

## 🐛 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Dates not visible in dark mode | Wrong text color | Check `CalendarDateTextAppearance` in themes.xml |
| Dots not showing | No expenses / decorators not attached | Check `getDailyTotals()` returns data |
| Wrong month expenses showing | Epoch day calculation | Check `getMonthStartEpochDay/EndEpochDay` |

---

## 📅 Last Updated
2026-01-23 - v1.0.0 Complete with decorators
