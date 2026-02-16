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

- Dashboard parser-first `VoiceUnderstandingEngine` resolves speech into structured fields and opens editor with:
  - parsed amount
  - parsed merchant
  - parsed category hint
  - description fallback
- Fallback path is flavor-aware:
  - `standard`: on-device semantic fallback
  - `offlineLlm`: MediaPipe Gemma local extractor with semantic failover
- If amount is not confidently parsed, editor still opens with transcript + merchant hint so user can confirm quickly.
- If any field confidence is low, only trusted fields are prefilled and user completes the rest in modal.
- If voice parse confidence is high for mandatory fields, dashboard bypasses modal and saves directly.
- ViewModel resolves category name to ID when categories are available.

## Files

| File | Purpose |
|---|---|
| `common/ExpenseEditorDialogFragment.java` | dialog UI and user interactions |
| `common/ExpenseEditorViewModel.java` | form state + save/update/delete logic |
| `voice/VoiceExpenseParser.java` | spoken text parser |
| `voice/VoiceUnderstandingEngine.java` | parser-first and confidence merge logic |
| `gamification/GamificationEngine.java` | XP and achievement logic invoked on save |
| `res/layout/fragment_expense_editor_dialog.xml` | dialog layout |

## Common Issues

| Issue | Likely Cause | Check |
|---|---|---|
| edit save shows XP message | stale save-message path | `ExpenseEditorViewModel.saveExpense()` |
| voice category not preselected | no matching category name | `resolveCategory(...)` and incoming parsed text |
| voice amount missing in form | transcript had no parseable numeric amount | parser extraction patterns and dialog fallback flow |
| tag/account not persisted | selected IDs missing before save | ViewModel selected state and repository writes |

## Last Updated
2026-02-14 (offline LLM fallback flavor update)
