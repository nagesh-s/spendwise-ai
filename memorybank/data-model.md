Entities
- Expense(id, dateEpochDay, amountCents, description, categoryId, merchantId, photoPath, accountId, isInvestmentTransfer, linkedInvestmentId, recoveryStatus, canonicalSourceKey)
- Category(id, name, colorArgb, iconKey, isDefault)
- Merchant(id, name)
- Budget(id, monthKeyYYYYMM, categoryId, limitCents)
- QuickTile(id, categoryId, merchantId, label, amountCents)
- RecurringExpense(id, amountCents, description, categoryId, merchantId, frequency, dayOfPeriod, monthOfYear, startDateEpochDay, endDateEpochDay, lastGeneratedEpochDay, isActive, reminderDaysBefore)
- Tag(id, name, colorArgb)
- ExpenseTag(expenseId, tagId) -- many-to-many join table
- Account(id, name, type, balanceCents, colorArgb, iconKey, isDefault) -- types: CASH, BANK, CREDIT_CARD, WALLET, OTHER
- FinancialGoal(id, name, type, targetAmountCents, currentAmountCents, startDateEpochDay, endDateEpochDay, status, colorArgb, iconKey, linkedCategoryId) -- types: SAVINGS, LIMIT, DEBT
- Bucket(id, name, type, targetPercent, currentBalanceCents, colorArgb, iconKey, sortOrder, isDefault, description, timelineLabel, investmentAdvice) -- types: EMERGENCY, SHORT_TERM, MEDIUM_TERM, LONG_TERM, RETIREMENT, CUSTOM
- BucketCategory(id, bucketId, categoryId) -- links buckets to expense categories
- Investment(id, bucketId, name, jobTitle, platform, assetType, investedAmountCents, currentValueCents, returnPercent, maturityDateEpochDay, interestRate, lastUpdatedEpochDay, notes, isAutoFetched, isSip, sipAmountCents, primaryCorrelationKey, lastSourceType, lastSourceKey)
- InvestmentSchedule(id, name, bucketId, assetType, platform, amountCents, frequency, dayOfPeriod, startEpochDay, endEpochDay, isActive, source, notes)
- InvestmentOccurrence(id, scheduleId, dueEpochDay, expectedAmountCents, status, confirmedAmountCents, confirmedAtMillis, linkedInvestmentId, correlationKey, sourceType) -- status: EXPECTED, CONFIRMED, ADJUSTED, SKIPPED
- IngestionEvent(id, domain, sourceType, sourceKey, correlationKey, eventType, amountCents, eventTimestampMillis, payloadHash, processedAtMillis, status, sourceSender, sourceBody) -- domain: SALARY, INVESTMENT, EXPENSE; sourceType: SMS, NOTIFICATION, MANUAL, SCHEDULE, EXPENSE_RECOVERY
- InstrumentBucketMapping(id, instrumentPattern, platformPattern, assetType, bucketId, confidenceBoost, updatedAtMillis, source) -- source: USER, AI, RULE

Core relationships
- Bucket 1..n Investment
- Bucket 1..n InvestmentSchedule
- InvestmentSchedule 1..n InvestmentOccurrence
- InvestmentOccurrence n..1 Investment (optional reconciliation link)
- IngestionEvent provides cross-source idempotency registry keyed by unique sourceKey
- InstrumentBucketMapping stores learn-once bucket assignment memory for future auto-mapping

Ledger model
- Planned ledger: Bucket.currentBalanceCents (monthly salary allocation plan)
- Actual ledger: Investment aggregates (confirmed holdings/net worth)
- Net worth and portfolio totals use Investment only (expected occurrences never inflate net worth)
- Investment transfers recovered from `Expense` stay visible in history/merchant views but are excluded from spend analytics and budget usage

Database version
- Version: 7
- Migration 5→6 introduces schedule/occurrence/event/mapping tables and reconciliation metadata columns on investments
- Migration 6→7 adds expense-level investment transfer metadata so old expense imports can be recovered into holdings without corrupting spend analytics
