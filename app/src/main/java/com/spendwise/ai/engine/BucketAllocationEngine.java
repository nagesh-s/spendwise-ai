package com.spendwise.ai.engine;

import androidx.annotation.NonNull;

import com.spendwise.ai.data.entity.Bucket;

import java.util.ArrayList;
import java.util.List;

/**
 * Core logic for splitting salary into buckets using "Reverse Budgeting."
 *
 * Allocation priority order (pay future self first):
 * 1. Bucket 0 (Emergency) — if below threshold
 * 2. Bucket 4 (Retirement) + Bucket 3 (Long-term) — aggressive compounding
 * 3. Bucket 2 (Medium-term)
 * 4. Bucket 1 (Short-term) — remainder = guilt-free spending
 *
 * The key output is the "guilt-free amount" — the money the user can
 * spend to zero without anxiety, because all future buckets are already funded.
 */
public class BucketAllocationEngine {

    /**
     * Calculate bucket allocation for a given salary amount.
     *
     * @param salaryAmountCents Total salary amount in cents
     * @param buckets           List of all active buckets (with targetPercent set)
     * @param guardrailResult   Result from CashflowGuardrail checks (may adjust
     *                          allocations)
     * @return AllocationPlan containing per-bucket amounts and guilt-free total
     */
    @NonNull
    public AllocationPlan allocate(long salaryAmountCents, @NonNull List<Bucket> buckets,
            @NonNull CashflowGuardrail.GuardrailResult guardrailResult) {
        List<AllocationResult> allocations = new ArrayList<>();
        long remaining = salaryAmountCents;
        long guiltFreeAmountCents = 0;

        // Step 1: Validate total percentages
        int totalPercent = 0;
        for (Bucket bucket : buckets) {
            totalPercent += bucket.getTargetPercent();
        }

        // If percentages don't add to 100, normalize them
        double scaleFactor = totalPercent > 0 ? 100.0 / totalPercent : 1.0;

        // Step 2: Allocate in reverse budgeting order
        // Priority: EMERGENCY > RETIREMENT > LONG_TERM > MEDIUM_TERM > SHORT_TERM >
        // CUSTOM
        List<Bucket> sortedBuckets = sortByAllocationPriority(buckets);

        for (Bucket bucket : sortedBuckets) {
            int adjustedPercent = (int) Math.round(bucket.getTargetPercent() * scaleFactor);

            // Check if guardrail has overridden this bucket's allocation
            Integer guardrailOverride = guardrailResult.getAdjustedPercent(bucket.getId());
            if (guardrailOverride != null) {
                adjustedPercent = guardrailOverride;
            }

            long allocatedAmount = (long) (salaryAmountCents * adjustedPercent / 100.0);

            // Ensure we don't allocate more than remaining
            allocatedAmount = Math.min(allocatedAmount, remaining);

            // Ensure minimum threshold (₹500 = 50000 cents)
            if (allocatedAmount > 0 && allocatedAmount < 500_00L && remaining >= 500_00L) {
                allocatedAmount = 500_00L;
            }

            String reason = generateReason(bucket, adjustedPercent, allocatedAmount, guardrailResult);

            allocations.add(new AllocationResult(
                    bucket.getId(),
                    bucket.getName(),
                    bucket.getType(),
                    allocatedAmount,
                    adjustedPercent,
                    reason));

            remaining -= allocatedAmount;

            // Track guilt-free amount (short-term bucket)
            if (Bucket.TYPE_SHORT_TERM.equals(bucket.getType())) {
                guiltFreeAmountCents = allocatedAmount;
            }
        }

        // If there's any remainder due to rounding, add it to the short-term bucket
        if (remaining > 0) {
            for (AllocationResult result : allocations) {
                if (Bucket.TYPE_SHORT_TERM.equals(result.getBucketType())) {
                    result.addAmount(remaining);
                    guiltFreeAmountCents += remaining;
                    break;
                }
            }
        }

        return new AllocationPlan(
                salaryAmountCents,
                allocations,
                guiltFreeAmountCents,
                guardrailResult.getWarnings());
    }

    /**
     * Sort buckets by allocation priority (reverse budgeting order).
     * Emergency first, Short-term last.
     */
    private List<Bucket> sortByAllocationPriority(@NonNull List<Bucket> buckets) {
        List<Bucket> sorted = new ArrayList<>(buckets);
        sorted.sort((a, b) -> {
            int priorityA = getAllocationPriority(a.getType());
            int priorityB = getAllocationPriority(b.getType());
            return Integer.compare(priorityA, priorityB);
        });
        return sorted;
    }

    /**
     * Get allocation priority for a bucket type.
     * Lower number = higher priority = allocated first.
     */
    private int getAllocationPriority(String type) {
        switch (type) {
            case Bucket.TYPE_EMERGENCY:
                return 0; // First: secure the baseline
            case Bucket.TYPE_RETIREMENT:
                return 1; // Second: aggressive compounding
            case Bucket.TYPE_LONG_TERM:
                return 2; // Third: growth
            case Bucket.TYPE_MEDIUM_TERM:
                return 3; // Fourth: medium goals
            case Bucket.TYPE_SHORT_TERM:
                return 4; // Last: guilt-free remainder
            case Bucket.TYPE_CUSTOM:
                return 5; // User-created buckets
            default:
                return 6;
        }
    }

    /**
     * Generate a human-readable "why this split" explanation for a bucket.
     */
    private String generateReason(Bucket bucket, int percent, long amountCents,
            CashflowGuardrail.GuardrailResult guardrailResult) {
        String formattedAmount = String.format("₹%,.0f", amountCents / 100.0);

        switch (bucket.getType()) {
            case Bucket.TYPE_EMERGENCY:
                if (guardrailResult.hasWarningFor(bucket.getId())) {
                    return "Emergency fund below threshold — boosted to " + formattedAmount;
                }
                return formattedAmount + " to keep your safety net strong";
            case Bucket.TYPE_RETIREMENT:
                return formattedAmount + " for compounding magic — don't touch this!";
            case Bucket.TYPE_LONG_TERM:
                return formattedAmount + " growing toward your big life goals";
            case Bucket.TYPE_MEDIUM_TERM:
                return formattedAmount + " for your 2-5 year upgrades";
            case Bucket.TYPE_SHORT_TERM:
                return formattedAmount + " — spend this guilt-free, your future is funded ✓";
            default:
                return formattedAmount + " (" + percent + "% of salary)";
        }
    }

    // --- Result Classes ---

    /**
     * The complete allocation plan for a salary.
     */
    public static class AllocationPlan {
        private final long salaryAmountCents;
        private final List<AllocationResult> allocations;
        private final long guiltFreeAmountCents;
        private final List<String> warnings;

        public AllocationPlan(long salaryAmountCents, List<AllocationResult> allocations,
                long guiltFreeAmountCents, List<String> warnings) {
            this.salaryAmountCents = salaryAmountCents;
            this.allocations = allocations;
            this.guiltFreeAmountCents = guiltFreeAmountCents;
            this.warnings = warnings;
        }

        public long getSalaryAmountCents() {
            return salaryAmountCents;
        }

        public List<AllocationResult> getAllocations() {
            return allocations;
        }

        public long getGuiltFreeAmountCents() {
            return guiltFreeAmountCents;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public String getFormattedGuiltFree() {
            return String.format("₹%,.0f", guiltFreeAmountCents / 100.0);
        }
    }

    /**
     * Allocation result for a single bucket.
     */
    public static class AllocationResult {
        private final long bucketId;
        private final String bucketName;
        private final String bucketType;
        private long allocatedAmountCents;
        private final int allocatedPercent;
        private final String reason;

        public AllocationResult(long bucketId, String bucketName, String bucketType,
                long allocatedAmountCents, int allocatedPercent, String reason) {
            this.bucketId = bucketId;
            this.bucketName = bucketName;
            this.bucketType = bucketType;
            this.allocatedAmountCents = allocatedAmountCents;
            this.allocatedPercent = allocatedPercent;
            this.reason = reason;
        }

        public long getBucketId() {
            return bucketId;
        }

        public String getBucketName() {
            return bucketName;
        }

        public String getBucketType() {
            return bucketType;
        }

        public long getAllocatedAmountCents() {
            return allocatedAmountCents;
        }

        public int getAllocatedPercent() {
            return allocatedPercent;
        }

        public String getReason() {
            return reason;
        }

        void addAmount(long additionalCents) {
            this.allocatedAmountCents += additionalCents;
        }

        public String getFormattedAmount() {
            return String.format("₹%,.0f", allocatedAmountCents / 100.0);
        }
    }
}
