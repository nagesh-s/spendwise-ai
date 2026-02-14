Entities
- Expense(id, dateEpochDay, amountCents, description, categoryId, merchantId, photoPath, accountId)
- Category(id, name, colorArgb, iconKey, isDefault)
- Merchant(id, name)
- Budget(id, monthKeyYYYYMM, categoryId, limitCents)
- QuickTile(id, categoryId, merchantId, label, amountCents)
- RecurringExpense(id, amountCents, description, categoryId, merchantId, frequency, dayOfPeriod, monthOfYear, startDateEpochDay, endDateEpochDay, lastGeneratedEpochDay, isActive, reminderDaysBefore)
- Tag(id, name, colorArgb)
- ExpenseTag(expenseId, tagId)  -- many-to-many join table
- Account(id, name, type, balanceCents, colorArgb, iconKey, isDefault)  -- types: CASH, BANK, CREDIT_CARD, WALLET, OTHER
- FinancialGoal(id, name, type, targetAmountCents, currentAmountCents, startDateEpochDay, endDateEpochDay, status, colorArgb, iconKey, linkedCategoryId)  -- types: SAVINGS, LIMIT, DEBT

Database version: 4 (Migration 3→4 creates tags, expense_tags, accounts, financial_goals, adds accountId to expenses)
