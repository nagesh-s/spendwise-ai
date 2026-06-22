package com.spendwise.ai.buckets;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.entity.BucketCategory;
import com.spendwise.ai.data.entity.InvestmentOccurrence;
import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.data.pojo.InvestmentOccurrenceWithSchedule;
import com.spendwise.ai.data.repository.BucketRepository;
import com.spendwise.ai.data.repository.InvestmentOccurrenceRepository;
import com.spendwise.ai.data.repository.InvestmentRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.lifecycle.Transformations;
import com.spendwise.ai.data.pojo.HoldingWithProvider;
import com.spendwise.ai.data.entity.Provider;

/**
 * ViewModel for the Bucket Detail drill-down screen.
 */
public class BucketDetailViewModel extends AndroidViewModel {

    private final BucketRepository bucketRepository;
    private final InvestmentRepository investmentRepository;
    private final InvestmentOccurrenceRepository investmentOccurrenceRepository;

    public BucketDetailViewModel(@NonNull Application application) {
        super(application);
        bucketRepository = new BucketRepository(application);
        investmentRepository = new InvestmentRepository(application);
        investmentOccurrenceRepository = new InvestmentOccurrenceRepository(application);
    }

    public LiveData<List<Holding>> getInvestmentsForBucket(long bucketId) {
        return investmentRepository.getInvestmentsForBucket(bucketId);
    }

    public static class ProviderGroup {
        public final Provider provider;
        public final String headerTitle;
        public final long totalValueCents;
        public final List<com.spendwise.ai.data.pojo.HoldingWithTransactionsAndProvider> holdings;

        public ProviderGroup(Provider provider, String headerTitle, long totalValueCents, List<com.spendwise.ai.data.pojo.HoldingWithTransactionsAndProvider> holdings) {
            this.provider = provider;
            this.headerTitle = headerTitle;
            this.totalValueCents = totalValueCents;
            this.holdings = holdings;
        }
    }

    public LiveData<List<ProviderGroup>> getGroupedInvestmentsForBucket(long bucketId) {
        return Transformations.map(investmentRepository.getHoldingsWithTransactionsAndProviderForBucket(bucketId), list -> {
            Map<Long, List<com.spendwise.ai.data.pojo.HoldingWithTransactionsAndProvider>> grouped = new LinkedHashMap<>();
            List<com.spendwise.ai.data.pojo.HoldingWithTransactionsAndProvider> unlinked = new ArrayList<>();

            for (com.spendwise.ai.data.pojo.HoldingWithTransactionsAndProvider item : list) {
                String notes = item.holding.getNotes();
                if (notes != null && notes.contains("[auto-map-pending:true]")) {
                    continue; // Skip pending investments
                }

                if (item.provider != null) {
                    long pid = item.provider.getId();
                    if (!grouped.containsKey(pid)) {
                        grouped.put(pid, new ArrayList<>());
                    }
                    grouped.get(pid).add(item);
                } else {
                    unlinked.add(item);
                }
            }

            List<ProviderGroup> result = new ArrayList<>();
            for (Map.Entry<Long, List<com.spendwise.ai.data.pojo.HoldingWithTransactionsAndProvider>> entry : grouped.entrySet()) {
                List<com.spendwise.ai.data.pojo.HoldingWithTransactionsAndProvider> group = entry.getValue();
                if (!group.isEmpty()) {
                    long totalValue = 0;
                    for (com.spendwise.ai.data.pojo.HoldingWithTransactionsAndProvider h : group) {
                        totalValue += h.holding.getCurrentValueCents();
                    }
                    result.add(new ProviderGroup(group.get(0).provider, group.get(0).provider.getName(), totalValue, group));
                }
            }

            if (!unlinked.isEmpty()) {
                long totalValue = 0;
                for (com.spendwise.ai.data.pojo.HoldingWithTransactionsAndProvider h : unlinked) {
                    totalValue += h.holding.getCurrentValueCents();
                }
                result.add(new ProviderGroup(null, "Unlinked / Other", totalValue, unlinked));
            }

            return result;
        });
    }

    public LiveData<List<Holding>> getPendingInvestmentsForBucket(long bucketId) {
        return Transformations.map(investmentRepository.getInvestmentsForBucket(bucketId), list -> {
            List<Holding> pending = new ArrayList<>();
            for (Holding inv : list) {
                String notes = inv.getNotes();
                if (notes != null && notes.contains("[auto-map-pending:true]")) {
                    pending.add(inv);
                }
            }
            return pending;
        });
    }

    public LiveData<List<BucketCategory>> getCategoriesForBucket(long bucketId) {
        return bucketRepository.getCategoriesForBucket(bucketId);
    }

    public LiveData<List<InvestmentOccurrenceWithSchedule>> getExpectedOccurrencesForBucket(long bucketId) {
        return investmentOccurrenceRepository.getOccurrencesForBucket(bucketId);
    }

    public void addInvestment(Holding investment) {
        investmentRepository.insert(investment);
    }

    public void updateInvestment(Holding investment) {
        investmentRepository.update(investment);
    }

    public void deleteInvestment(Holding investment) {
        investmentRepository.delete(investment);
    }

    public void addCategoryToBucket(long bucketId, long categoryId) {
        bucketRepository.addCategoryToBucket(bucketId, categoryId);
    }

    public void removeCategoryFromBucket(long bucketId, long categoryId) {
        bucketRepository.removeCategoryFromBucket(bucketId, categoryId);
    }

    public void resolveOccurrence(
            long occurrenceId,
            String status,
            long amountCents,
            long confirmedAtMillis,
            Long linkedInvestmentId,
            String correlationKey,
            String sourceType) {
        investmentOccurrenceRepository.resolveOccurrence(
                occurrenceId,
                status,
                amountCents,
                confirmedAtMillis,
                linkedInvestmentId,
                correlationKey,
                sourceType);
    }
}
