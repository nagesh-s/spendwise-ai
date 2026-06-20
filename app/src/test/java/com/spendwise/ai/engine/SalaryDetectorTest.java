package com.spendwise.ai.engine;

import com.spendwise.ai.sms.ParsedTransaction;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for SalaryDetector.
 *
 * Tests salary detection from SMS transactions using:
 * - Keyword matching (salary, payroll, NEFT)
 * - Amount thresholds
 * - Non-salary rejection (refunds, cashback)
 * - Confidence scoring
 * - Salary tolerance check
 */
public class SalaryDetectorTest {

    private SalaryDetector detector;

    @Before
    public void setUp() {
        detector = new SalaryDetector();
    }

    // --- Helper ---

    private ParsedTransaction createCredit(long amountCents, String message) {
        return new ParsedTransaction(
                ParsedTransaction.TransactionType.CREDIT,
                "Bank",
                amountCents,
                null, // merchant
                "XX1234", // accountInfo
                message, // originalMessage
                System.currentTimeMillis());
    }

    private ParsedTransaction createDebit(long amountCents, String message) {
        return new ParsedTransaction(
                ParsedTransaction.TransactionType.DEBIT,
                "Bank",
                amountCents,
                null,
                "XX1234",
                message,
                System.currentTimeMillis());
    }

    // --- Positive Detection Tests ---

    @Test
    public void detect_salaryKeywordAndHighAmount_detectsSalary() {
        ParsedTransaction txn = createCredit(
                50_000_00L,
                "Salary credited to your account XX1234 via NEFT. Amount Rs.50,000.00");

        SalaryDetector.SalaryDetectionResult result = detector.detect(txn);

        assertNotNull("Should detect salary", result);
        assertEquals(50_000_00L, result.getAmountCents());
        assertTrue("Confidence should be high", result.getConfidence() >= 0.75f);
        assertNotNull(result.getReason());
    }

    @Test
    public void detect_neftCreditAboveThreshold_detectsSalary() {
        ParsedTransaction txn = createCredit(
                75_000_00L,
                "Rs.75,000.00 credited to your account XX5678 via NEFT ref 999888777");

        SalaryDetector.SalaryDetectionResult result = detector.detect(txn);

        assertNotNull("NEFT credit above threshold should be detected", result);
        assertTrue("Confidence should be >= 0.40", result.getConfidence() >= 0.40f);
    }

    @Test
    public void detect_payrollKeyword_detectsSalary() {
        ParsedTransaction txn = createCredit(
                45_000_00L,
                "Payroll credit of Rs 45000 to account XX1234");

        SalaryDetector.SalaryDetectionResult result = detector.detect(txn);

        assertNotNull("Payroll keyword should detect salary", result);
        assertTrue(result.getReason().toLowerCase().contains("salary keyword"));
    }

    @Test
    public void detect_roundAmountAboveThreshold_detectsSalary() {
        ParsedTransaction txn = createCredit(
                30_000_00L,
                "Rs 30000 credited to account XX1234 via IMPS transfer");

        SalaryDetector.SalaryDetectionResult result = detector.detect(txn);

        assertNotNull("Round amount via IMPS above threshold should detect", result);
    }

    // --- Negative Detection Tests ---

    @Test
    public void detect_debitTransaction_returnsNull() {
        ParsedTransaction txn = createDebit(
                50_000_00L,
                "Rs.50,000.00 debited from your account for salary processing");

        SalaryDetector.SalaryDetectionResult result = detector.detect(txn);

        assertNull("Debit transactions should never detect as salary", result);
    }

    @Test
    public void detect_refundCredit_returnsNull() {
        ParsedTransaction txn = createCredit(
                15_000_00L,
                "Refund of Rs.15,000.00 credited to account XX1234");

        SalaryDetector.SalaryDetectionResult result = detector.detect(txn);

        assertNull("Refund credits should be rejected", result);
    }

    @Test
    public void detect_cashbackCredit_returnsNull() {
        ParsedTransaction txn = createCredit(
                500_00L,
                "Cashback of Rs.500 credited to your account");

        SalaryDetector.SalaryDetectionResult result = detector.detect(txn);

        assertNull("Cashback should be rejected", result);
    }

    @Test
    public void detect_smallAmountNoKeywords_returnsNull() {
        ParsedTransaction txn = createCredit(
                2_000_00L,
                "Rs 2000 credited to your account XX1234");

        SalaryDetector.SalaryDetectionResult result = detector.detect(txn);

        assertNull("Small amount without keywords should not detect", result);
    }

    @Test
    public void detect_interestCredit_returnsNull() {
        ParsedTransaction txn = createCredit(
                1_200_00L,
                "Interest of Rs.1,200 credited to your savings account XX1234");

        SalaryDetector.SalaryDetectionResult result = detector.detect(txn);

        assertNull("Interest credits should be rejected", result);
    }

    // --- Salary Tolerance Tests ---

    @Test
    public void isWithinSalaryTolerance_exactMatch_returnsTrue() {
        assertTrue(SalaryDetector.isWithinSalaryTolerance(50_000_00L, 50_000_00L));
    }

    @Test
    public void isWithinSalaryTolerance_within10Percent_returnsTrue() {
        // 5% above
        assertTrue(SalaryDetector.isWithinSalaryTolerance(52_500_00L, 50_000_00L));
        // 10% below
        assertTrue(SalaryDetector.isWithinSalaryTolerance(45_000_00L, 50_000_00L));
    }

    @Test
    public void isWithinSalaryTolerance_beyond10Percent_returnsFalse() {
        // 15% above
        assertFalse(SalaryDetector.isWithinSalaryTolerance(57_500_00L, 50_000_00L));
        // 15% below
        assertFalse(SalaryDetector.isWithinSalaryTolerance(42_000_00L, 50_000_00L));
    }

    @Test
    public void isWithinSalaryTolerance_zeroKnownSalary_returnsFalse() {
        assertFalse(SalaryDetector.isWithinSalaryTolerance(50_000_00L, 0L));
    }

    // --- Custom Threshold Tests ---

    @Test
    public void detect_customLowThreshold_detectsLowerAmounts() {
        SalaryDetector lowThresholdDetector = new SalaryDetector(5_000_00L); // ₹5,000

        ParsedTransaction txn = createCredit(
                8_000_00L,
                "Salary of Rs.8,000 credited via NEFT");

        SalaryDetector.SalaryDetectionResult result = lowThresholdDetector.detect(txn);

        assertNotNull("Lower threshold should detect lower salaries", result);
    }

    // --- Result Object Tests ---

    @Test
    public void detectionResult_formattedAmount_isCorrect() {
        ParsedTransaction txn = createCredit(
                50_000_00L,
                "Salary credited to your account XX1234 via NEFT");

        SalaryDetector.SalaryDetectionResult result = detector.detect(txn);

        assertNotNull(result);
        assertTrue(result.getFormattedAmount().contains("50,000"));
        assertEquals("XX1234", result.getAccountInfo());
    }
}
