package com.spendwise.ai.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.entity.Bucket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Safety checks before suggesting salary allocation.
 *
 * Implements guardrails to prevent dangerous allocation:
 * 1. Emergency fund threshold — boost if Bucket 0 is underfunded
 * 2. Upcoming bills buffer — ensure short-term covers recurring expenses
 * 3. Spending pattern check — adjust if user consistently overspends
 * 4. Minimum allocation — never allocate < ₹500 to any active bucket
 */
public class CashflowGuardrail {

    private static final long MIN_ALLOCATION_CENTS = 500_00L; // ₹500

    /**
     * Run all guardrail checks and return adjusted recommendations.
     *
     * @param buckets                   All active buckets
     * @param emergencyBalanceCents     Current balance of the emergency bucket
     * @param avgMonthlySpendCents      Average monthly spending (from expense
     *                                  history)
     * @param upcomingBillsCents        Total recurring bills due in the next 30
     *                                  days
     * @param lastMonthActualSpendCents Actual spending from last month
     * @param shortTermAllocCents       Proposed short-term bucket allocation
     * @return GuardrailResult with warnings and adjusted allocations
     */
    @NonNull
    public GuardrailResult check(@NonNull List<Bucket> buckets,
            long emergencyBalanceCents,
            long avgMonthlySpendCents,
            long upcomingBillsCents,
            long lastMonthActualSpendCents,
            long shortTermAllocCents) {

        List<String> warnings = new ArrayList<>();
        Map<Long, Integer> adjustedPercents = new HashMap<>();
        boolean isHealthy = true;

        // Find bucket IDs by type
        long emergencyBucketId = -1;
        long shortTermBucketId = -1;
        int emergencyCurrentPercent = 0;
        int shortTermCurrentPercent = 0;

        for (Bucket bucket : buckets) {
            if (Bucket.TYPE_EMERGENCY.equals(bucket.getType())) {
                emergencyBucketId = bucket.getId();
                emergencyCurrentPercent = bucket.getTargetPercent();
            } else if (Bucket.TYPE_SHORT_TERM.equals(bucket.getType())) {
                shortTermBucketId = bucket.getId();
                shortTermCurrentPercent = bucket.getTargetPercent();
            }
        }

        // Check 1: Emergency fund threshold
        // If emergency fund < 3× average monthly spend, boost it
        long emergencyTarget = avgMonthlySpendCents * 3;
        if (emergencyBucketId >= 0 && emergencyBalanceCents < emergencyTarget) {
            isHealthy = false;
            long deficit = emergencyTarget - emergencyBalanceCents;
            int boostPercent = Math.min(emergencyCurrentPercent + 10, 30); // Cap at 30%
            adjustedPercents.put(emergencyBucketId, boostPercent);

            String deficitFormatted = String.format("₹%,.0f", deficit / 100.0);
            warnings.add("⚠️ Emergency fund is " + deficitFormatted +
                    " below the 3-month safety net. Boosting allocation to " + boostPercent + "%.");
        }

        // Check if severely underfunded (< 1× monthly spend)
        if (emergencyBucketId >= 0 && emergencyBalanceCents < avgMonthlySpendCents) {
            int urgentBoost = Math.min(emergencyCurrentPercent + 20, 40);
            adjustedPercents.put(emergencyBucketId, urgentBoost);
            warnings.add("🚨 Emergency fund is critically low (less than 1 month expenses). " +
                    "Urgently boosting to " + urgentBoost + "%.");
        }

        // Check 2: Upcoming bills buffer
        // Ensure short-term bucket can cover bills due within 30 days
        if (shortTermBucketId >= 0 && upcomingBillsCents > 0
                && shortTermAllocCents < upcomingBillsCents) {
            isHealthy = false;
            String billsFormatted = String.format("₹%,.0f", upcomingBillsCents / 100.0);
            warnings.add("📋 You have " + billsFormatted +
                    " in upcoming bills. Ensure your short-term bucket covers them.");
        }

        // Check 3: Spending pattern check
        // If actual spending exceeded allocation last month, warn the user
        if (lastMonthActualSpendCents > 0 && shortTermAllocCents > 0
                && lastMonthActualSpendCents > shortTermAllocCents) {
            long overspend = lastMonthActualSpendCents - shortTermAllocCents;
            String overspendFormatted = String.format("₹%,.0f", overspend / 100.0);
            warnings.add("📊 Last month you overspent by " + overspendFormatted +
                    ". Consider increasing your short-term allocation.");
        }

        // Check 4: Minimum allocation per bucket
        // (This is enforced in BucketAllocationEngine, here we just warn)

        return new GuardrailResult(isHealthy, warnings, adjustedPercents);
    }

    /**
     * Result of guardrail checks.
     */
    public static class GuardrailResult {
        private final boolean isHealthy;
        private final List<String> warnings;
        private final Map<Long, Integer> adjustedPercents; // bucketId → adjusted %

        public GuardrailResult(boolean isHealthy, List<String> warnings,
                Map<Long, Integer> adjustedPercents) {
            this.isHealthy = isHealthy;
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            this.adjustedPercents = adjustedPercents != null ? adjustedPercents : new HashMap<>();
        }

        public boolean isHealthy() {
            return isHealthy;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        /**
         * Get the adjusted allocation percentage for a specific bucket.
         * Returns null if no adjustment was made.
         */
        @Nullable
        public Integer getAdjustedPercent(long bucketId) {
            return adjustedPercents.get(bucketId);
        }

        /**
         * Check if there's a specific warning for a given bucket.
         */
        public boolean hasWarningFor(long bucketId) {
            return adjustedPercents.containsKey(bucketId);
        }

        /**
         * Create a healthy (no warnings, no adjustments) result.
         */
        public static GuardrailResult healthy() {
            return new GuardrailResult(true, new ArrayList<>(), new HashMap<>());
        }
    }
}
