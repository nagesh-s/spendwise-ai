# Expense Editor Module

> Purpose: explain add/edit flows, voice-prefill behavior, and save-side effects.

---

## Status: Implemented + Expanded (v1.6.0)

## What This Dialog Does

- Add and edit expenses with:
  - amount
  - date
  - category
  - merchant
  - account
  - tags
  - optional description
- Supports voice-prefilled launch from Dashboard.
- Supports delete when editing existing expense.
- Triggers gamification side effects for new expense saves.

## Save Flow

1. Validate amount/category/date.
2. Resolve merchant ID (create/find if needed).
3. Insert or update expense in background executor.
4. Persist selected tags.
5. If new expense:
  - award XP
  - evaluate achievement unlocks (first expense, streak, early bird, level)
6. Post save result message and completion state to UI.

## Voice Prefill

- Dashboard parses speech text into structured fields and opens editor with:
  - parsed amount
  - parsed merchant
  - parsed category hint
  - description fallback
- ViewModel resolves category name to ID when categories are available.

## Files

| File | Purpose |
|---|---|
| `common/ExpenseEditorDialogFragment.java` | dialog UI and user interactions |
| `common/ExpenseEditorViewModel.java` | form state + save/update/delete logic |
| `voice/VoiceExpenseParser.java` | spoken text parser |
| `gamification/GamificationEngine.java` | XP and achievement logic invoked on save |
| `res/layout/fragment_expense_editor_dialog.xml` | dialog layout |

## Common Issues

| Issue | Likely Cause | Check |
|---|---|---|
| edit save shows XP message | stale save-message path | `ExpenseEditorViewModel.saveExpense()` |
| voice category not preselected | no matching category name | `resolveCategory(...)` and incoming parsed text |
| tag/account not persisted | selected IDs missing before save | ViewModel selected state and repository writes |

## Last Updated
2026-02-14
