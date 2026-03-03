package com.spendwise.ai.engine;

import com.spendwise.ai.data.entity.Bucket;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for CashflowGuardrail.
 *
 * Tests safety checks:
 * - Emergency fund threshold (3× monthly spend)
 * - Critical emergency (< 1× monthly spend)
 * - Upcoming bills buffer
 * - Overspend pattern detection
 * - Healthy result
 */
public class CashflowGuardrailTest {

    private CashflowGuardrail guardrail;
    private List<Bucket> defaultBuckets;

    private static final long ONE_LAKH = 100_000_00L;

    @Before
    public void setUp() {
        guardrail = new CashflowGuardrail();
        defaultBuckets = createDefaultBuckets();
    }

    private List<Bucket> createDefaultBuckets() {
        List<Bucket> buckets = new ArrayList<>();

        Bucket emergency = new Bucket("Emergency Fund", Bucket.TYPE_EMERGENCY, 10, 0xFFE53935, 0, true);
        emergency.setId(1);

        Bucket shortTerm = new Bucket("Enjoy the Present", Bucket.TYPE_SHORT_TERM, 30, 0xFF43A047, 1, true);
        shortTerm.setId(2);

        Bucket mediumTerm = new Bucket("Build the Next", Bucket.TYPE_MEDIUM_TERM, 20, 0xFF1E88E5, 2, true);
        mediumTerm.setId(3);

        Bucket longTerm = new Bucket("Secure the Future", Bucket.TYPE_LONG_TERM, 20, 0xFF8E24AA, 3, true);
        longTerm.setId(4);

        Bucket retirement = new Bucket("Freedom Fund", Bucket.TYPE_RETIREMENT, 20, 0xFFFF8F00, 4, true);
        retirement.setId(5);

        buckets.add(emergency);
        buckets.add(shortTerm);
        buckets.add(mediumTerm);
        buckets.add(longTerm);
        buckets.add(retirement);
        return buckets;
    }

    // --- Healthy State Tests ---

    @Test
    public void check_healthyFinances_returnsHealthyResult() {
        // Emergency has 3× monthly spend, no bills, no overspend
        CashflowGuardrail.GuardrailResult result = guardrail.check(
                defaultBuckets,
                3 * ONE_LAKH, // emergency balance = 3× monthly spend
                ONE_LAKH, // avg monthly spend
                0, // no upcoming bills
                ONE_LAKH * 30 / 100, // last month spend = short-term alloc
                ONE_LAKH * 30 / 100); // short-term alloc

        assertTrue("Should be healthy with sufficient emergency fund", result.isHealthy());
        assertTrue("No warnings expected", result.getWarnings().isEmpty());
    }

    @Test
    public void healthyFactory_createsHealthyResult() {
        CashflowGuardrail.GuardrailResult result = CashflowGuardrail.GuardrailResult.healthy();

        assertTrue(result.isHealthy());
        assertTrue(result.getWarnings().isEmpty());
        assertNull(result.getAdjustedPercent(1L));
    }

    // --- Emergency Fund Threshold Tests ---

    @Test
    public void check_emergencyBelowThreshold_warnsAndAdjusts() {
        // Emergency balance = 1.5× monthly spend (below 3× target)
        CashflowGuardrail.GuardrailResult result = guardrail.check(
                defaultBuckets,
                (long) (ONE_LAKH * 1.5), // below 3× threshold
                ONE_LAKH,
                0, 0, 0);

        assertFalse("Should not be healthy", result.isHealthy());
        assertFalse("Should have warnings", result.getWarnings().isEmpty());
        assertTrue("Should have adjusted emergency percent", result.hasWarningFor(1L));

        Integer adjusted = result.getAdjustedPercent(1L);
        assertNotNull("Should have an adjusted percent", adjusted);
        assertTrue("Adjusted percent should be higher than default 10%", adjusted > 10);
    }

    @Test
    public void check_emergencyCriticallyLow_urgentWarning() {
        // Emergency balance = 0.5× monthly spend (critically low)
        CashflowGuardrail.GuardrailResult result = guardrail.check(
                defaultBuckets,
                ONE_LAKH / 2, // only half a month's expenses
                ONE_LAKH,
                0, 0, 0);

        assertFalse(result.isHealthy());

        // Should have at least one urgent warning
        boolean hasUrgentWarning = false;
        for (String warning : result.getWarnings()) {
            if (warning.contains("critically") || warning.contains("🚨")) {
                hasUrgentWarning = true;
                break;
            }
        }
        assertTrue("Should have an urgent warning", hasUrgentWarning);

        // Should boost emergency allocation more aggressively
        Integer adjusted = result.getAdjustedPercent(1L);
        assertNotNull(adjusted);
        assertTrue("Critical emergency should boost to at least 20%", adjusted >= 20);
    }

    @Test
    public void check_emergencyAtExactThreshold_noEmergencyWarning() {
        // Emergency balance = exactly 3× monthly spend
        CashflowGuardrail.GuardrailResult result = guardrail.check(
                defaultBuckets,
                3 * ONE_LAKH,
                ONE_LAKH,
                0, 0, 0);

        assertTrue("Exact threshold should be healthy", result.isHealthy());
        assertNull("No adjusted percent needed", result.getAdjustedPercent(1L));
    }

    // --- Upcoming Bills Buffer Tests ---

    @Test
    public void check_billsExceedShortTermAlloc_warns() {
        CashflowGuardrail.GuardrailResult result = guardrail.check(
                defaultBuckets,
                3 * ONE_LAKH, // emergency is fine
                ONE_LAKH, // avg spend
                50_000_00L, // upcoming bills = ₹50k
                0,
                30_000_00L); // short-term alloc = ₹30k (less than bills)

        assertFalse("Should warn about insufficient bill coverage", result.isHealthy());

        boolean hasBillWarning = false;
        for (String warning : result.getWarnings()) {
            if (warning.contains("bills") || warning.contains("📋")) {
                hasBillWarning = true;
                break;
            }
        }
        assertTrue("Should have a bill warning", hasBillWarning);
    }

    @Test
    public void check_noBills_noWarning() {
        CashflowGuardrail.GuardrailResult result = guardrail.check(
                defaultBuckets,
                3 * ONE_LAKH,
                ONE_LAKH,
                0, // no upcoming bills
                0,
                30_000_00L);

        assertTrue("No bills should not trigger warning", result.isHealthy());
    }

    // --- Overspend Pattern Tests ---

    @Test
    public void check_lastMonthOverspent_warns() {
        CashflowGuardrail.GuardrailResult result = guardrail.check(
                defaultBuckets,
                3 * ONE_LAKH,
                ONE_LAKH,
                0,
                50_000_00L, // last month actual spend = ₹50k
                30_000_00L); // short-term alloc was only ₹30k

        boolean hasOverspendWarning = false;
        for (String warning : result.getWarnings()) {
            if (warning.contains("overspent") || warning.contains("📊")) {
                hasOverspendWarning = true;
                break;
            }
        }
        assertTrue("Should warn about past overspending", hasOverspendWarning);
    }

    @Test
    public void check_lastMonthUnderspent_noOverspendWarning() {
        CashflowGuardrail.GuardrailResult result = guardrail.check(
                defaultBuckets,
                3 * ONE_LAKH,
                ONE_LAKH,
                0,
                20_000_00L, // spent less than allocated
                30_000_00L);

        boolean hasOverspendWarning = false;
        for (String warning : result.getWarnings()) {
            if (warning.contains("overspent") || warning.contains("📊")) {
                hasOverspendWarning = true;
                break;
            }
        }
        assertFalse("Underspending should not trigger overspend warning", hasOverspendWarning);
    }

    // --- Edge Cases ---

    @Test
    public void check_emptyBucketList_noExceptions() {
        CashflowGuardrail.GuardrailResult result = guardrail.check(
                new ArrayList<>(), 0, 0, 0, 0, 0);

        assertNotNull(result);
        // With no buckets, guardrail checks on emergency/short-term are skipped
    }

    @Test
    public void check_zeroMonthlySpend_noEmergencyWarning() {
        CashflowGuardrail.GuardrailResult result = guardrail.check(
                defaultBuckets,
                0, // zero emergency balance
                0, // zero monthly spend → target is 0
                0, 0, 0);

        // With zero monthly spend, emergency target = 0 and balance = 0 → healthy
        assertTrue("Zero monthly spend → emergency target is 0 → healthy", result.isHealthy());
    }
}
