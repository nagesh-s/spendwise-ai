# DAO query notes

Representative queries only. The source DAOs are authoritative for annotations, names, transactions, and complete signatures. `InvestmentDao` is a legacy API name for active `Holding` persistence; new code may use a clearer naming convention when refactoring with tests.

## Expenses and merchant analytics

```java
@Query("SELECT COALESCE(SUM(amountCents),0) FROM expenses WHERE dateEpochDay BETWEEN :start AND :end")
long totalForRange(long start, long end);

@Query("SELECT COALESCE(SUM(amountCents),0) FROM expenses WHERE isInvestmentTransfer = 0 AND dateEpochDay BETWEEN :start AND :end")
long consumptionTotalForRange(long start, long end);
```

Consumption-only aggregates exclude investment transfers. History/merchant views may include them to preserve cash-flow visibility. Consult the exact method signature in `ExpenseDao` before using category/merchant result POJOs.

## Holdings (via `InvestmentDao`)

```java
@Query("SELECT * FROM holdings WHERE bucketId = :bucketId ORDER BY name ASC")
LiveData<List<Holding>> getInvestmentsForBucket(long bucketId);

@Query("SELECT * FROM holdings WHERE canonicalInstrumentKey = :canonicalInstrumentKey LIMIT 1")
Holding getByCanonicalInstrumentKey(String canonicalInstrumentKey);

@Query("SELECT COALESCE(SUM(currentValueCents), 0) FROM holdings")
LiveData<Long> getTotalPortfolioValue();
```

## Logical investment transactions

```java
@Query("SELECT * FROM investment_transactions WHERE transactionKey = :key LIMIT 1")
InvestmentTransaction getByTransactionKey(String key);

@Query("SELECT * FROM investment_transactions WHERE holdingId = :holdingId ORDER BY eventTimestampMillis DESC, id DESC")
List<InvestmentTransaction> getForHoldingSync(long holdingId);
```

`getConfirmedTotals(holdingId)` provides logical contribution/withdrawal/income/fee/tax aggregates for the domain service. Do not infer a transaction ledger by counting source messages.

## Source evidence and bucket assignments

```java
@Insert(onConflict = OnConflictStrategy.IGNORE)
long insertIgnore(IngestionEvent event);

@Query("SELECT * FROM ingestion_events WHERE sourceKey = :sourceKey LIMIT 1")
IngestionEvent getBySourceKey(String sourceKey);

@Query("SELECT * FROM instrument_bucket_mappings WHERE assetType = :assetType ORDER BY confidenceBoost DESC, updatedAtMillis DESC")
List<InstrumentBucketMapping> getByAssetTypeSync(String assetType);
```

## Schedule occurrences

```java
@Query("SELECT * FROM investment_occurrences WHERE status = :status AND dueEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dueEpochDay ASC")
List<InvestmentOccurrence> getByStatusInWindowSync(String status, long startEpochDay, long endEpochDay);
```

Occurrence linkage now includes the active holding reference (`linkedInvestmentId`, a historical column name) and logical `transactionId`; see `InvestmentOccurrenceDao` and `InvestmentOccurrence` for current fields and Room foreign keys.
