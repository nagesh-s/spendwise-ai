# JSON import/export

`DataBackupManager` uses Android's Storage Access Framework and runs file/database work on a single-thread executor. Its current format version is **3**, separate from the Room schema version.

Top-level fields and representative values:

```json
{
  "backupVersion": 3,
  "timestamp": 1770000000000,
  "categories": [],
  "merchants": [],
  "expenses": [],
  "budgets": [],
  "buckets": [],
  "investments": [],
  "bucketMappings": []
}
```

`timestamp` is epoch milliseconds. Monetary values are integer cents. `monthKeyYYYYMM` uses `YYYYMM`, and epoch-day fields are integer days. Version 3 added bucket mappings; imports reject newer versions.

The `investments` field is a serialized `Holding` subset retained under the historical field name. Export includes expense/category/merchant/budget/bucket/holding and bucket-mapping subsets. It does **not** include the complete investment transaction ledger, event links, providers, valuations, raw source events/messages, reviews, correction memory, all preferences, receipt images, or all expense metadata.

Import merges rather than replacing the entire database. Therefore JSON export/import must not be described as a complete restore or a migration mechanism for a schema-version jump. Do not add sensitive fields to public sample fixtures; use synthetic data.

CSV/PDF reports are user-selected exports and are not interchangeable with the JSON import format.
