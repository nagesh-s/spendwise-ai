package com.spendwise.ai.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.sms.InvestmentSignalKeyFactory;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Repairs duplicate investment holdings created from multiple messages that
 * described the same instrument.
 */
public class InvestmentDeduplicationEngine {

    private static final long FD_RD_AMOUNT_TOLERANCE_CENTS = 1_00L;
    private static final long FD_RD_DAY_WINDOW = 30L;

    public static class RepairResult {
        public int scanned;
        public int backfilledKeys;
        public int merged;
    }

    @NonNull
    public RepairResult repair(@NonNull AppDatabase db) {
        RepairResult result = new RepairResult();
        List<Holding> investments = db.investmentDao().getAllInvestmentsSync();
        if (investments == null || investments.isEmpty()) {
            return result;
        }
        result.scanned = investments.size();

        List<Holding> current = new ArrayList<>(investments);
        for (Holding investment : current) {
            String canonicalKey = buildCanonicalKey(investment);
            if (isBlank(investment.getCanonicalInstrumentKey())) {
                investment.setCanonicalInstrumentKey(canonicalKey);
                db.investmentDao().update(investment);
                result.backfilledKeys++;
            }
        }

        Map<String, Holding> keeperByStrictKey = new HashMap<>();
        for (Holding investment : current) {
            Holding fresh = db.investmentDao().getById(investment.getId());
            if (fresh == null) {
                continue;
            }
            String strictKey = strictDuplicateKey(fresh);
            Holding keeper = keeperByStrictKey.get(strictKey);
            if (keeper == null) {
                keeperByStrictKey.put(strictKey, fresh);
                continue;
            }
            if (shouldMerge(keeper, fresh)) {
                mergeInto(db, keeper, fresh);
                result.merged++;
            }
        }
        return result;
    }

    @NonNull
    public String buildCanonicalKey(@NonNull Holding investment) {
        long timestampMillis = LocalDate.ofEpochDay(investment.getLastUpdatedEpochDay())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        return InvestmentSignalKeyFactory.buildCanonicalInstrumentKey(
                investment.getAssetType(),
                investment.getPlatform(),
                investment.getName(),
                investment.getInvestedAmountCents(),
                timestampMillis,
                investment.getNotes(),
                investment.getPrimaryCorrelationKey());
    }

    private void mergeInto(
            @NonNull AppDatabase db,
            @NonNull Holding keeper,
            @NonNull Holding duplicate) {
        keeper.setInvestedAmountCents(Math.max(keeper.getInvestedAmountCents(), duplicate.getInvestedAmountCents()));
        keeper.setCurrentValueCents(Math.max(keeper.getCurrentValueCents(), duplicate.getCurrentValueCents()));
        keeper.setLastUpdatedEpochDay(Math.max(keeper.getLastUpdatedEpochDay(), duplicate.getLastUpdatedEpochDay()));

        if (isBlank(keeper.getPlatform()) && !isBlank(duplicate.getPlatform())) {
            keeper.setPlatform(duplicate.getPlatform());
        }
        if (isWeakName(keeper.getName()) && !isWeakName(duplicate.getName())) {
            keeper.setName(duplicate.getName());
        }
        if (isBlank(keeper.getPrimaryCorrelationKey()) && !isBlank(duplicate.getPrimaryCorrelationKey())) {
            keeper.setPrimaryCorrelationKey(duplicate.getPrimaryCorrelationKey());
        }
        if (!isBlank(duplicate.getLastSourceKey())) {
            keeper.setLastSourceKey(duplicate.getLastSourceKey());
        }
        if (!isBlank(duplicate.getLastSourceType())) {
            keeper.setLastSourceType(duplicate.getLastSourceType());
        }
        keeper.setNotes(mergeNotes(keeper.getNotes(), duplicate.getNotes()));
        keeper.setCanonicalInstrumentKey(buildCanonicalKey(keeper));

        db.investmentDao().update(keeper);
        db.investmentDao().deleteById(duplicate.getId());
    }

    private boolean shouldMerge(@NonNull Holding keeper, @NonNull Holding candidate) {
        if (!normalize(keeper.getAssetType()).equals(normalize(candidate.getAssetType()))) {
            return false;
        }
        boolean fdOrRd = Holding.ASSET_FD.equals(keeper.getAssetType())
                || Holding.ASSET_RD.equals(keeper.getAssetType());
        if (!fdOrRd) {
            return !isBlank(keeper.getCanonicalInstrumentKey())
                    && keeper.getCanonicalInstrumentKey().equals(candidate.getCanonicalInstrumentKey());
        }
        if (!normalize(keeper.getPlatform()).equals(normalize(candidate.getPlatform()))) {
            return false;
        }
        long amountDelta = Math.abs(keeper.getInvestedAmountCents() - candidate.getInvestedAmountCents());
        long dayDelta = Math.abs(keeper.getLastUpdatedEpochDay() - candidate.getLastUpdatedEpochDay());
        return amountDelta <= FD_RD_AMOUNT_TOLERANCE_CENTS && dayDelta <= FD_RD_DAY_WINDOW;
    }

    @NonNull
    private String strictDuplicateKey(@NonNull Holding investment) {
        return String.format(Locale.US, "%s|%s|%d",
                normalize(investment.getAssetType()),
                normalize(investment.getPlatform()),
                amountBucket(investment));
    }

    private long amountBucket(@NonNull Holding investment) {
        if (Holding.ASSET_FD.equals(investment.getAssetType())
                || Holding.ASSET_RD.equals(investment.getAssetType())) {
            return Math.round(investment.getInvestedAmountCents() / 100.0d);
        }
        return Math.round(investment.getInvestedAmountCents() / 1000.0d);
    }

    @NonNull
    private String mergeNotes(@Nullable String first, @Nullable String second) {
        if (isBlank(first)) {
            return second == null ? "" : second.trim();
        }
        if (isBlank(second) || first.contains(second.trim())) {
            return first.trim();
        }
        return first.trim() + " " + second.trim();
    }

    private boolean isWeakName(@Nullable String value) {
        String normalized = normalize(value);
        return normalized.isEmpty()
                || normalized.equals("investment")
                || normalized.equals("investment allocation")
                || normalized.equals("fixed deposit")
                || normalized.equals("recurring deposit")
                || normalized.endsWith(" fixed deposit")
                || normalized.endsWith(" recurring deposit");
    }

    private boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    @NonNull
    private String normalize(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
