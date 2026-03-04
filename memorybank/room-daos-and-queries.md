ExpenseDao key queries
@Query("SELECT COALESCE(SUM(amountCents),0) FROM expenses WHERE dateEpochDay BETWEEN :start AND :end")
long totalForRange(long start, long end);

@Query("SELECT c.name, COALESCE(SUM(e.amountCents),0) AS total FROM expenses e JOIN categories c ON e.categoryId=c.id WHERE e.dateEpochDay BETWEEN :start AND :end GROUP BY c.id ORDER BY total DESC")
List<CategoryTotal> categoryTotals(long start, long end);

Merchant analytics
@Query("SELECT m.name, COUNT(e.id) AS txns, COALESCE(SUM(e.amountCents),0) AS total, AVG(e.amountCents) AS avgAmt FROM expenses e JOIN merchants m ON e.merchantId=m.id WHERE e.dateEpochDay BETWEEN :start AND :end GROUP BY m.id ORDER BY total DESC LIMIT :limit")
List<MerchantAgg> topMerchants(long start, long end, int limit);

BucketDao key queries
@Query("SELECT b.*, COALESCE(SUM(i.currentValueCents), 0) AS totalValueCents, COALESCE(SUM(i.investedAmountCents), 0) AS totalInvestedCents FROM buckets b LEFT JOIN investments i ON i.bucketId = b.id GROUP BY b.id ORDER BY b.sortOrder ASC")
LiveData<List<BucketWithTotal>> getAllBucketsWithTotals();

@Query("UPDATE buckets SET currentBalanceCents = :balanceCents WHERE id = :bucketId")
void updateBalance(long bucketId, long balanceCents);

InvestmentDao key queries
@Query("SELECT * FROM investments WHERE bucketId = :bucketId ORDER BY name ASC")
LiveData<List<Investment>> getInvestmentsForBucket(long bucketId);

@Query("SELECT COALESCE(SUM(currentValueCents), 0) FROM investments")
LiveData<Long> getTotalPortfolioValue();

InvestmentScheduleDao key queries
@Query("SELECT * FROM investment_schedules WHERE isActive = 1 ORDER BY id ASC")
List<InvestmentSchedule> getActiveSchedulesSync();

@Query("UPDATE investment_schedules SET isActive = :isActive WHERE id = :id")
void setActive(long id, boolean isActive);

InvestmentOccurrenceDao key queries
@Query("SELECT io.*, s.name AS scheduleName, s.assetType AS scheduleAssetType, s.platform AS schedulePlatform, s.bucketId AS scheduleBucketId FROM investment_occurrences io JOIN investment_schedules s ON s.id = io.scheduleId WHERE s.bucketId = :bucketId ORDER BY io.dueEpochDay DESC")
LiveData<List<InvestmentOccurrenceWithSchedule>> getOccurrencesForBucket(long bucketId);

@Query("UPDATE investment_occurrences SET status = :status, confirmedAmountCents = :amountCents, confirmedAtMillis = :confirmedAtMillis, linkedInvestmentId = :linkedInvestmentId, correlationKey = :correlationKey, sourceType = :sourceType WHERE id = :id")
void markResolved(long id, String status, long amountCents, long confirmedAtMillis, Long linkedInvestmentId, String correlationKey, String sourceType);

@Query("SELECT * FROM investment_occurrences WHERE status = :status AND dueEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dueEpochDay ASC")
List<InvestmentOccurrence> getByStatusInWindowSync(String status, long startEpochDay, long endEpochDay);

IngestionEventDao key queries
@Insert(onConflict = OnConflictStrategy.IGNORE)
long insertIgnore(IngestionEvent event);

@Query("SELECT * FROM ingestion_events WHERE sourceKey = :sourceKey LIMIT 1")
IngestionEvent getBySourceKey(String sourceKey);

@Query("UPDATE ingestion_events SET status = :status, processedAtMillis = :processedAtMillis WHERE id = :id")
void markProcessed(long id, String status, long processedAtMillis);

InstrumentBucketMappingDao key queries
@Query("SELECT * FROM instrument_bucket_mappings WHERE assetType = :assetType ORDER BY confidenceBoost DESC, updatedAtMillis DESC")
List<InstrumentBucketMapping> getByAssetTypeSync(String assetType);
