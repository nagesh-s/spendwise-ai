# Expense Editor Module

> **Purpose**: Explains how adding/editing expenses works.

---

## 📌 Status: ✅ IMPLEMENTED (v1.0.0)

---

## 🎯 What This Dialog Does

The Expense Editor is a **dialog** for creating and editing expenses with:
- Amount input with ₹ prefix
- Description (optional) with smart category suggestions
- Category dropdown (required)
- Merchant autocomplete from history
- Date picker

---

## 🔄 How It Works

### Opening Dialog
1. User taps FAB on Calendar or "Add" button
2. `ExpenseEditorDialogFragment.newInstance(expenseId, dateEpochDay)` called
3. If `expenseId` is null → new expense mode
4. If `expenseId` provided → edit mode (loads existing expense)

### Smart Category Suggestions
1. User starts typing description
2. `TextWatcher` in `setupDescriptionSuggestions()` fires
3. Keyword matching checks for food/transport/shopping terms
4. Category dropdown auto-fills if match found

### Merchant Autocomplete
1. ViewModel loads `getAllMerchants()` from database
2. `AutoCompleteTextView` shows suggestions as user types
3. Threshold is 1 character

### Saving Expense
1. User taps Save button
2. `validate()` checks: amount > 0, category selected
3. If valid, `saveExpense()` creates/updates Expense entity
4. Merchant created if new (insertOrGetId)
5. Dialog dismisses on success

---

## 📁 Files

| File | Purpose |
|------|---------|
| `common/ExpenseEditorDialogFragment.java` | Dialog UI, form validation |
| `common/ExpenseEditorViewModel.java` | Form state, save logic |
| `fragment_expense_editor_dialog.xml` | Layout with input fields |
| `data/repository/ExpenseRepository.java` | Insert/update operations |
| `data/repository/MerchantRepository.java` | insertOrGetId for merchants |

---

## 🧠 Smart Category Keywords

| Category | Trigger Words |
|----------|---------------|
| Food & Dining | food, lunch, dinner, breakfast, restaurant, pizza, burger, coffee |
| Groceries | grocery, groceries, supermarket, vegetables, fruits |
| Transportation | uber, ola, taxi, bus, train, metro, petrol, fuel, gas |
| Utilities | electric, water, internet, wifi, phone, mobile, recharge |
| Entertainment | movie, netflix, spotify, concert, game, entertainment |
| Shopping | clothes, shoes, fashion, shopping, amazon, flipkart |
| Healthcare | doctor, medicine, hospital, pharmacy, health |
| Housing | rent, maintenance, repair, home |

Defined in `ExpenseEditorDialogFragment.suggestCategoryFromDescription()`.

---

## 🐛 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| "Please enter valid amount" | Empty or 0 amount | Amount must be > 0 |
| "Please select a category" | No category selected | User must pick from dropdown |
| Merchant not saving | Insert failed | Check MerchantRepository.insertOrGetId() |
| Date wrong | Epoch day conversion | Check DateUtils.epochDayToDate() |

---

## 📅 Last Updated
2026-01-23 - v1.0.0 with smart features
