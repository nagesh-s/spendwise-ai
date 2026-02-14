## Room Migrations

Write explicit Migration objects for version increments and add regression tests that validate data retention across updates.

Document entity and index changes with clear rationale and upgrade steps per version.

### Migration History

| Migration | Tables Created | Columns Added | Notes |
|-----------|---------------|---------------|-------|
| 1→2 | `quick_tiles` | `photoPath` on `expenses` | Phase 1: QuickTiles + photo attachments |
| 2→3 | `recurring_expenses` | — | Phase 1: Recurring expenses system |
| 3→4 | `tags`, `expense_tags`, `accounts`, `financial_goals` | `accountId` on `expenses` | Phase 2: Tags, accounts, goals. Default Cash account seeded. |

### Migration 3→4 Details (Phase 2)

Located in `AppDatabase.java` as `MIGRATION_3_4`:

```sql
-- Tags
CREATE TABLE tags (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, colorArgb INTEGER NOT NULL)

-- Expense-Tag join (many-to-many)
CREATE TABLE expense_tags (expenseId INTEGER NOT NULL, tagId INTEGER NOT NULL, PRIMARY KEY(expenseId, tagId), ...)
CREATE INDEX index_expense_tags_expenseId ON expense_tags (expenseId)
CREATE INDEX index_expense_tags_tagId ON expense_tags (tagId)

-- Accounts
CREATE TABLE accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, ...)
INSERT INTO accounts (...) VALUES ('Cash', 'CASH', 0, <green>, 'fa_wallet', 1)  -- default seed

-- Link expenses to accounts
ALTER TABLE expenses ADD COLUMN accountId INTEGER

-- Financial Goals
CREATE TABLE financial_goals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, ...)
```
