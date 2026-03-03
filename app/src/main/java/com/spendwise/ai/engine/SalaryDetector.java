package com.spendwise.ai.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.sms.ParsedTransaction;

import java.util.regex.Pattern;

/**
 * Identifies salary credits from parsed SMS transactions.
 *
 * Uses keyword matching, amount heuristics, and recurrence patterns
 * to distinguish salary from other credit transactions (refunds, cashback,
 * P2P).
 *
 * This feeds into the BucketAllocationEngine — once a salary is detected,
 * the allocation suggestion dialog is triggered.
 */
public class SalaryDetector {

    // Salary-specific keywords in SMS body
    private static final Pattern SALARY_KEYWORDS = Pattern.compile(
            "(?:salary|sal\\s*cr|neft\\s*cr|payroll|monthly\\s*pay|employer|" +
                    "wages|stipend|income\\s*credit|sal\\s*credit)",
            Pattern.CASE_INSENSITIVE);

    // Non-salary credit keywords (to reject false positives)
    private static final Pattern NON_SALARY_KEYWORDS = Pattern.compile(
            "(?:refund|cashback|reversal|interest|dividend|reward|" +
                    "bonus|prize|won|gift|lottery)",
            Pattern.CASE_INSENSITIVE);

    // Default minimum threshold for salary detection (₹10,000 = 1,000,000 cents)
    private static final long DEFAULT_MIN_SALARY_CENTS = 10_000_00L;

    private final long minSalaryCents;

    public SalaryDetector() {
        this(DEFAULT_MIN_SALARY_CENTS);
    }

    public SalaryDetector(long minSalaryCents) {
        this.minSalaryCents = minSalaryCents;
    }

    /**
     * Analyze a parsed transaction to determine if it's a salary credit.
     *
     * @param transaction The parsed SMS transaction
     * @return SalaryDetectionResult if it looks like salary, null otherwise
     */
    @Nullable
    public SalaryDetectionResult detect(@NonNull ParsedTransaction transaction) {
        // Must be a credit transaction
        if (transaction.getType() != ParsedTransaction.TransactionType.CREDIT) {
            return null;
        }

        long amountCents = transaction.getAmountCents();
        String body = transaction.getOriginalMessage();

        // Check against non-salary keywords first (fast rejection)
        if (body != null && NON_SALARY_KEYWORDS.matcher(body).find()) {
            return null;
        }

        // Calculate confidence based on multiple signals
        float confidence = 0.0f;
        StringBuilder reason = new StringBuilder();

        // Signal 1: Salary keywords in body
        if (body != null && SALARY_KEYWORDS.matcher(body).find()) {
            confidence += 0.50f;
            reason.append("Salary keyword detected. ");
        }

        // Signal 2: Amount above threshold
        if (amountCents >= minSalaryCents) {
            confidence += 0.25f;
            reason.append("Amount above ₹").append(minSalaryCents / 100).append(" threshold. ");
        }

        // Signal 3: NEFT/IMPS transfer (common salary channels)
        if (body != null && Pattern.compile("(?:neft|imps|rtgs)", Pattern.CASE_INSENSITIVE)
                .matcher(body).find()) {
            confidence += 0.15f;
            reason.append("NEFT/IMPS/RTGS transfer. ");
        }

        // Signal 4: Round number (salaries tend to be round amounts)
        if (amountCents % 100_00L == 0) { // Divisible by ₹100
            confidence += 0.10f;
            reason.append("Round amount. ");
        }

        // Only consider it a salary if confidence >= 0.40 (at least amount threshold +
        // one signal)
        if (confidence < 0.40f) {
            return null;
        }

        return new SalaryDetectionResult(
                amountCents,
                Math.min(confidence, 1.0f),
                transaction.getAccountInfo(),
                transaction.getTimestampMillis(),
                reason.toString().trim());
    }

    /**
     * Check if a given amount could be a salary based on a previously known salary
     * amount.
     * Uses a tolerance of ±10% to account for variable components (bonuses,
     * deductions).
     *
     * @param amountCents      The detected credit amount
     * @param knownSalaryCents Previously known salary amount
     * @return true if it's within tolerance
     */
    public static boolean isWithinSalaryTolerance(long amountCents, long knownSalaryCents) {
        if (knownSalaryCents <= 0)
            return false;
        double ratio = (double) amountCents / knownSalaryCents;
        return ratio >= 0.90 && ratio <= 1.10;
    }

    /**
     * Result of salary detection analysis.
     */
    public static class SalaryDetectionResult {
        private final long amountCents;
        private final float confidence; // 0.0 to 1.0
        @Nullable
        private final String accountInfo;
        private final long timestampMillis;
        private final String reason;

        public SalaryDetectionResult(long amountCents, float confidence,
                @Nullable String accountInfo, long timestampMillis,
                String reason) {
            this.amountCents = amountCents;
            this.confidence = confidence;
            this.accountInfo = accountInfo;
            this.timestampMillis = timestampMillis;
            this.reason = reason;
        }

        public long getAmountCents() {
            return amountCents;
        }

        public float getConfidence() {
            return confidence;
        }

        @Nullable
        public String getAccountInfo() {
            return accountInfo;
        }

        public long getTimestampMillis() {
            return timestampMillis;
        }

        public String getReason() {
            return reason;
        }

        public String getFormattedAmount() {
            return String.format("₹%,.2f", amountCents / 100.0);
        }
    }
}
