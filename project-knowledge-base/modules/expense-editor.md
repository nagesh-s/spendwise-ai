# Expense Editor Module

> **Purpose**: Explains how adding/editing expenses works.

---

## 📌 Status: ✅ IMPLEMENTED + FIXED (v1.1.1)

---

## 🎯 What This Dialog Does

Expense Editor supports:
- Add new expense for selected date
- Edit existing expense (with reliable prefill)
- Amount, description, category, merchant, and date fields
- Smart category suggestion from description keywords
- Merchant autocomplete from local history

---

## 🔄 How It Works

### Opening Dialog
1. Caller provides `(expenseId?, dateEpochDay)` via `newInstance(...)`.
2. No `expenseId` → new expense mode.
3. With `expenseId` → edit mode.

### Edit Prefill (v1.1.1 Fix)
1. ViewModel loads existing expense synchronously on background executor.
2. Fragment observes `editingExpense`.
3. UI fields prefill amount/category/date/description/merchant before user saves.

### Smart Category Suggestion
- Description keywords map to seeded categories:
  - Food/grocery terms → `Food & Dining`
  - Utility/home terms → `Bills & Utilities`
  - Health terms → `Health & Fitness`
  - Transport/shopping/entertainment map directly

### Save
1. Validate amount and category.
2. Resolve merchant ID (create/find).
3. Insert or update `Expense`.
4. Signal `saveComplete` and dismiss dialog.

---

## 📁 Files

| File | Purpose |
|------|---------|
| `common/ExpenseEditorDialogFragment.java` | Dialog UI + observers + keyword suggestion |
| `common/ExpenseEditorViewModel.java` | Form state, validation, load/save/delete |
| `fragment_expense_editor_dialog.xml` | Dialog layout |
| `data/repository/ExpenseRepository.java` | Insert/update/delete + sync lookup |
| `data/repository/MerchantRepository.java` | Merchant lookup/create |

---

## 🐛 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Edit dialog shows blank fields | Edit prefill not observed | Verify `getEditingExpense()` observer |
| Merchant not prefilled | Merchant map not loaded yet | Check pending merchant selection logic |
| Wrong suggestion category | Keyword mapping mismatch | Verify seeded category name constants |
| Save fails validation | Missing amount/category | Check `validate()` errors in ViewModel |

---

## 📅 Last Updated
2026-02-08 - v1.1.1 fixed edit prefill and category mapping alignment
