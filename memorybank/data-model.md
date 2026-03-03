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
- Bucket(id, name, type, targetPercent, currentBalanceCents, colorArgb, iconKey, sortOrder, isDefault, description, timelineLabel, investmentAdvice)  -- types: EMERGENCY, SHORT_TERM, MEDIUM_TERM, LONG_TERM, RETIREMENT, CUSTOM
- BucketCategory(bucketId, categoryId)  -- links buckets to expense categories
- Investment(id, bucketId, name, jobTitle, platform, assetType, investedAmountCents, currentValueCents, lastUpdatedEpochDay, notes, isSip)  -- per-bucket investment tracking

Database version: 5 (Migration 4→5 creates buckets, bucket_categories, investments; seeds 5 default buckets)
