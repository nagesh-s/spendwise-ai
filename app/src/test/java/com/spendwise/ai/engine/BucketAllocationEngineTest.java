package com.spendwise.ai.engine;

import com.spendwise.ai.data.entity.Bucket;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for BucketAllocationEngine.
 *
 * Tests the Reverse Budgeting logic:
 * - Priority ordering (Emergency → Retirement → Long → Medium → Short)
 * - Guilt-free amount calculation
 * - Guardrail overrides
 * - Remainder rounding
 * - Edge cases
 */
public class BucketAllocationEngineTest {

    private BucketAllocationEngine engine;
    private List<Bucket> defaultBuckets;

    // ₹1,00,000 salary = 10,000,000 cents
    private static final long SALARY_1_LAKH = 100_000_00L;

    @Before
    public void setUp() {
        engine = new BucketAllocationEngine();
        defaultBuckets = createDefaultBuckets();
    }

    /**
     * Create the 5 default buckets matching the seeded data in MIGRATION_4_5.
     */
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

    // --- Core Allocation Tests ---

    @Test
    public void allocate_withDefaultBuckets_allocatesAllFunds() {
        CashflowGuardrail.GuardrailResult guardrail = CashflowGuardrail.GuardrailResult.healthy();
        BucketAllocationEngine.AllocationPlan plan = engine.allocate(SALARY_1_LAKH, defaultBuckets, guardrail);

        assertNotNull(plan);
        assertEquals(SALARY_1_LAKH, plan.getSalaryAmountCents());

        // Total allocated should equal salary
        long totalAllocated = 0;
        for (BucketAllocationEngine.AllocationResult alloc : plan.getAllocations()) {
            totalAllocated += alloc.getAllocatedAmountCents();
        }
        assertEquals("Total allocation must equal salary", SALARY_1_LAKH, totalAllocated);
    }

    @Test
    public void allocate_withDefaultBuckets_hasGuiltFreeAmount() {
        CashflowGuardrail.GuardrailResult guardrail = CashflowGuardrail.GuardrailResult.healthy();
        BucketAllocationEngine.AllocationPlan plan = engine.allocate(SALARY_1_LAKH, defaultBuckets, guardrail);

        // Guilt-free amount should be > 0 (short-term bucket gets 30%)
        assertTrue("Guilt-free amount must be positive",
                plan.getGuiltFreeAmountCents() > 0);
    }

    @Test
    public void allocate_withDefaultBuckets_allocatesFiveBuckets() {
        CashflowGuardrail.GuardrailResult guardrail = CashflowGuardrail.GuardrailResult.healthy();
        BucketAllocationEngine.AllocationPlan plan = engine.allocate(SALARY_1_LAKH, defaultBuckets, guardrail);

        assertEquals("Should allocate to all 5 buckets", 5, plan.getAllocations().size());
    }

    @Test
    public void allocate_reverseBudgetingOrder_emergencyFirst_shortTermLast() {
        CashflowGuardrail.GuardrailResult guardrail = CashflowGuardrail.GuardrailResult.healthy();
        BucketAllocationEngine.AllocationPlan plan = engine.allocate(SALARY_1_LAKH, defaultBuckets, guardrail);

        List<BucketAllocationEngine.AllocationResult> allocations = plan.getAllocations();

        // Emergency should be first
        assertEquals("Emergency should be allocated first",
                Bucket.TYPE_EMERGENCY, allocations.get(0).getBucketType());

        // Short-term should be last (or second to last if custom exists)
        String lastType = allocations.get(allocations.size() - 1).getBucketType();
        assertTrue("Short-term or Custom should be allocated last",
                Bucket.TYPE_SHORT_TERM.equals(lastType) || Bucket.TYPE_CUSTOM.equals(lastType));
    }

    @Test
    public void allocate_producesNonNullReasons() {
        CashflowGuardrail.GuardrailResult guardrail = CashflowGuardrail.GuardrailResult.healthy();
        BucketAllocationEngine.AllocationPlan plan = engine.allocate(SALARY_1_LAKH, defaultBuckets, guardrail);

        for (BucketAllocationEngine.AllocationResult alloc : plan.getAllocations()) {
            assertNotNull("Reason should not be null for " + alloc.getBucketName(),
                    alloc.getReason());
            assertFalse("Reason should not be empty for " + alloc.getBucketName(),
                    alloc.getReason().isEmpty());
        }
    }

    // --- Guardrail Override Tests ---

    @Test
    public void allocate_withGuardrailOverride_usesAdjustedPercents() {
        // Simulate guardrail boosting emergency to 25%
        java.util.Map<Long, Integer> adjustedPercents = new java.util.HashMap<>();
        adjustedPercents.put(1L, 25); // emergencyBucketId = 1
        List<String> warnings = new ArrayList<>();
        warnings.add("Emergency fund below threshold");
        CashflowGuardrail.GuardrailResult guardrail = new CashflowGuardrail.GuardrailResult(false, warnings,
                adjustedPercents);

        BucketAllocationEngine.AllocationPlan plan = engine.allocate(SALARY_1_LAKH, defaultBuckets, guardrail);

        // Find the emergency allocation
        for (BucketAllocationEngine.AllocationResult alloc : plan.getAllocations()) {
            if (Bucket.TYPE_EMERGENCY.equals(alloc.getBucketType())) {
                assertEquals("Emergency should use guardrail-adjusted 25%",
                        25, alloc.getAllocatedPercent());
                break;
            }
        }

        // Warnings should be passed through
        assertFalse(plan.getWarnings().isEmpty());
    }

    // --- Edge Cases ---

    @Test
    public void allocate_zeroSalary_returnsEmptyAllocations() {
        CashflowGuardrail.GuardrailResult guardrail = CashflowGuardrail.GuardrailResult.healthy();
        BucketAllocationEngine.AllocationPlan plan = engine.allocate(0, defaultBuckets, guardrail);

        // All allocations should be 0
        for (BucketAllocationEngine.AllocationResult alloc : plan.getAllocations()) {
            assertEquals("Zero salary → zero allocation",
                    0L, alloc.getAllocatedAmountCents());
        }
        assertEquals("Zero salary → zero guilt-free", 0L, plan.getGuiltFreeAmountCents());
    }

    @Test
    public void allocate_singleBucket_getsFullSalary() {
        List<Bucket> singleBucket = new ArrayList<>();
        Bucket only = new Bucket("Only", Bucket.TYPE_SHORT_TERM, 100, 0xFF000000, 0, true);
        only.setId(1);
        singleBucket.add(only);

        CashflowGuardrail.GuardrailResult guardrail = CashflowGuardrail.GuardrailResult.healthy();
        BucketAllocationEngine.AllocationPlan plan = engine.allocate(SALARY_1_LAKH, singleBucket, guardrail);

        assertEquals(1, plan.getAllocations().size());
        assertEquals(SALARY_1_LAKH, plan.getAllocations().get(0).getAllocatedAmountCents());
    }

    @Test
    public void allocate_percentsDontSumTo100_normalizesCorrectly() {
        // Create buckets that only add up to 50%
        List<Bucket> underBuckets = new ArrayList<>();
        Bucket b1 = new Bucket("A", Bucket.TYPE_EMERGENCY, 10, 0xFF000000, 0, true);
        b1.setId(1);
        Bucket b2 = new Bucket("B", Bucket.TYPE_SHORT_TERM, 40, 0xFF000000, 1, true);
        b2.setId(2);
        underBuckets.add(b1);
        underBuckets.add(b2);

        CashflowGuardrail.GuardrailResult guardrail = CashflowGuardrail.GuardrailResult.healthy();
        BucketAllocationEngine.AllocationPlan plan = engine.allocate(SALARY_1_LAKH, underBuckets, guardrail);

        // Total should still equal salary (normalization + remainder handling)
        long totalAllocated = 0;
        for (BucketAllocationEngine.AllocationResult alloc : plan.getAllocations()) {
            totalAllocated += alloc.getAllocatedAmountCents();
        }
        assertEquals("Normalization: total must equal salary", SALARY_1_LAKH, totalAllocated);
    }

    @Test
    public void allocate_healthyGuardrail_noWarnings() {
        CashflowGuardrail.GuardrailResult guardrail = CashflowGuardrail.GuardrailResult.healthy();
        BucketAllocationEngine.AllocationPlan plan = engine.allocate(SALARY_1_LAKH, defaultBuckets, guardrail);

        assertTrue("Healthy guardrail should produce no warnings",
                plan.getWarnings().isEmpty());
    }

    @Test
    public void allocate_formattedGuiltFree_notEmpty() {
        CashflowGuardrail.GuardrailResult guardrail = CashflowGuardrail.GuardrailResult.healthy();
        BucketAllocationEngine.AllocationPlan plan = engine.allocate(SALARY_1_LAKH, defaultBuckets, guardrail);

        String formatted = plan.getFormattedGuiltFree();
        assertNotNull(formatted);
        assertTrue("Formatted guilt-free should contain ₹", formatted.contains("₹"));
    }
}
