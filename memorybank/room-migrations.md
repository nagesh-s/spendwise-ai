## Room Migrations

Write explicit Migration objects for version increments and add regression tests that validate data retention across updates.

Document entity and index changes with clear rationale and upgrade steps per version.

### Migration History

| Migration | Tables Created | Columns Added | Notes |
|-----------|---------------|---------------|-------|
| 1→2 | `quick_tiles` | `photoPath` on `expenses` | Phase 1: QuickTiles + photo attachments |
| 2→3 | `recurring_expenses` | — | Phase 1: Recurring expenses system |
| 3→4 | `tags`, `expense_tags`, `accounts`, `financial_goals` | `accountId` on `expenses` | Phase 2: Tags, accounts, goals. Default Cash account seeded. |
| 4→5 | `buckets`, `bucket_categories`, `investments` | — | Bucket feature: 5 default time-horizon buckets seeded. |

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

### Migration 4→5 Details (Bucket Feature)

Located in `AppDatabase.java` as `MIGRATION_4_5`:

```sql
-- Buckets (5 time-horizon buckets)
CREATE TABLE buckets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL,
    targetPercent INTEGER NOT NULL, currentBalanceCents INTEGER NOT NULL, colorArgb INTEGER NOT NULL,
    iconKey TEXT, sortOrder INTEGER NOT NULL, isDefault INTEGER NOT NULL,
    description TEXT, timelineLabel TEXT, investmentAdvice TEXT)

-- Seed 5 default buckets: Emergency(10%), Short-term(30%), Medium-term(20%), Long-term(20%), Retirement(20%)
INSERT INTO buckets (...) VALUES ('Emergency Fund', 'EMERGENCY', 10, 0, ..., 0, 1, ...)
INSERT INTO buckets (...) VALUES ('Enjoy the Present', 'SHORT_TERM', 30, 0, ..., 1, 1, ...)
INSERT INTO buckets (...) VALUES ('Build the Next', 'MEDIUM_TERM', 20, 0, ..., 2, 1, ...)
INSERT INTO buckets (...) VALUES ('Secure the Future', 'LONG_TERM', 20, 0, ..., 3, 1, ...)
INSERT INTO buckets (...) VALUES ('Freedom Fund', 'RETIREMENT', 20, 0, ..., 4, 1, ...)

-- Bucket-Category link (many-to-many)
CREATE TABLE bucket_categories (bucketId INTEGER NOT NULL, categoryId INTEGER NOT NULL, PRIMARY KEY(bucketId, categoryId), ...)

-- Investments per bucket
CREATE TABLE investments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, bucketId INTEGER NOT NULL,
    name TEXT NOT NULL, jobTitle TEXT, platform TEXT, assetType TEXT NOT NULL,
    investedAmountCents INTEGER NOT NULL, currentValueCents INTEGER NOT NULL,
    lastUpdatedEpochDay INTEGER NOT NULL, notes TEXT, isSip INTEGER NOT NULL, ...)
```
