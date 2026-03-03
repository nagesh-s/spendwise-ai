@Query("SELECT IFNULL(SUM(amountCents),0) FROM Expense WHERE dateEpochDay BETWEEN :start AND :end")
long totalForRange(long start, long end);

@Query("SELECT c.name, IFNULL(SUM(e.amountCents),0) AS total FROM Expense e JOIN Category c ON e.categoryId=c.id WHERE e.dateEpochDay BETWEEN :start AND :end GROUP BY c.id ORDER BY total DESC")
List<CategoryTotal> categoryTotals(long start, long end);

@Query("SELECT m.name, COUNT(e.id) AS txns, IFNULL(SUM(e.amountCents),0) AS total, AVG(e.amountCents) AS avgAmt FROM Expense e JOIN Merchant m ON e.merchantId=m.id WHERE e.dateEpochDay BETWEEN :start AND :end GROUP BY m.id ORDER BY total DESC LIMIT :limit")
List<MerchantAgg> topMerchants(long start, long end, int limit);

-- BucketDao key queries:
@Query("SELECT b.*, IFNULL(SUM(i.currentValueCents), 0) AS totalInvestedValue FROM buckets b LEFT JOIN investments i ON b.id = i.bucketId GROUP BY b.id ORDER BY b.sortOrder ASC")
LiveData<List<BucketWithTotal>> getAllBucketsWithTotals();

@Query("UPDATE buckets SET currentBalanceCents = :balanceCents WHERE id = :bucketId")
void updateBalance(long bucketId, long balanceCents);

-- InvestmentDao key queries:
@Query("SELECT * FROM investments WHERE bucketId = :bucketId ORDER BY currentValueCents DESC")
LiveData<List<Investment>> getInvestmentsForBucket(long bucketId);

@Query("SELECT IFNULL(SUM(currentValueCents), 0) FROM investments")
LiveData<Long> getTotalPortfolioValue();
