Entities
- Expense(id, dateEpochDay, amountCents, description, categoryId, merchantId)
- Category(id, name, colorArgb, iconKey, isDefault)
- Merchant(id, name)
- Budget(id, monthKeyYYYYMM, categoryId, limitCents)
- QuickTile(id, categoryId, merchantId, label, amountCents)
- RecurringExpense(id, amountCents, description, categoryId, merchantId, frequency, dayOfPeriod, monthOfYear, startDateEpochDay, endDateEpochDay, lastGeneratedEpochDay, isActive, reminderDaysBefore)

