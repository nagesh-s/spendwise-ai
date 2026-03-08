package com.spendwise.ai.buckets;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.engine.BucketAllocationEngine;
import com.spendwise.ai.engine.CashflowGuardrail;

import org.threeten.bp.YearMonth;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;

/**
 * Dialog that shows the salary allocation suggestion.
 *
 * This is the core UX of "Reverse Budgeting" — showing the user
 * how their salary is split, with the guilt-free amount emphasized.
 *
 * Triggered by:
 * 1. Salary SMS detected → SalaryDetector → this dialog
 * 2. Manual "I Got Paid" tap on BucketsFragment
 * 3. Smart fallback: expected payday reached with no SMS detected
 */
public class AllocationSuggestionDialog {

    /**
     * Show allocation suggestion for a detected salary amount.
     *
     * @param context           Activity or Fragment context
     * @param salaryAmountCents The salary amount in cents
     * @param onConfirm         Callback when user confirms allocation
     */
    public static void show(Context context, long salaryAmountCents,
            OnAllocationConfirmedListener onConfirm) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<Bucket> buckets = db.bucketDao().getAllBucketsSync();

            if (buckets.isEmpty())
                return;

            // Run guardrail checks
            long emergencyBalance = 0;
            long avgMonthlySpend = 0;
            long upcomingBills = 0;
            long lastMonthSpend = 0;
            long shortTermAlloc = 0;

            // Find emergency bucket balance
            for (Bucket b : buckets) {
                if (Bucket.TYPE_EMERGENCY.equals(b.getType())) {
                    emergencyBalance = b.getCurrentBalanceCents();
                }
                if (Bucket.TYPE_SHORT_TERM.equals(b.getType())) {
                    shortTermAlloc = (long) (salaryAmountCents * b.getTargetPercent() / 100.0);
                }
            }

            // Get average monthly spend from expenses
            long startOfMonth = org.threeten.bp.LocalDate.now()
                    .withDayOfMonth(1).toEpochDay();
            long endOfMonth = org.threeten.bp.LocalDate.now()
                    .withDayOfMonth(org.threeten.bp.LocalDate.now().lengthOfMonth()).toEpochDay();
            long thisMonthSpend = db.expenseDao().getConsumptionTotalForRange(startOfMonth, endOfMonth);
            avgMonthlySpend = thisMonthSpend > 0 ? thisMonthSpend : shortTermAlloc;

            CashflowGuardrail guardrail = new CashflowGuardrail();
            CashflowGuardrail.GuardrailResult guardrailResult = guardrail.check(
                    buckets, emergencyBalance, avgMonthlySpend,
                    upcomingBills, lastMonthSpend, shortTermAlloc);

            // Run allocation engine
            BucketAllocationEngine engine = new BucketAllocationEngine();
            BucketAllocationEngine.AllocationPlan plan = engine.allocate(
                    salaryAmountCents, buckets, guardrailResult);

            // Show dialog on main thread
            android.os.Handler mainHandler = new android.os.Handler(
                    android.os.Looper.getMainLooper());
            mainHandler.post(() -> showDialog(context, plan, onConfirm));
        });
    }

    private static void showDialog(Context context,
            BucketAllocationEngine.AllocationPlan plan,
            OnAllocationConfirmedListener onConfirm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Allocate Salary");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        // Salary header
        TextView headerText = new TextView(context);
        headerText.setText(String.format("Salary: ₹%,.0f", plan.getSalaryAmountCents() / 100.0));
        headerText.setTextSize(18);
        headerText.setPadding(0, 0, 0, 24);
        layout.addView(headerText);

        // Warnings (if any)
        for (String warning : plan.getWarnings()) {
            TextView warningText = new TextView(context);
            warningText.setText(warning);
            warningText.setTextColor(0xFFFF6F00);
            warningText.setPadding(0, 0, 0, 8);
            layout.addView(warningText);
        }

        // Allocation breakdown
        for (BucketAllocationEngine.AllocationResult alloc : plan.getAllocations()) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 12, 0, 12);

            TextView nameText = new TextView(context);
            nameText.setText(String.format("%s — %s (%d%%)",
                    alloc.getBucketName(),
                    alloc.getFormattedAmount(),
                    alloc.getAllocatedPercent()));
            nameText.setTextSize(15);
            row.addView(nameText);

            TextView reasonText = new TextView(context);
            reasonText.setText(alloc.getReason());
            reasonText.setTextSize(12);
            reasonText.setTextColor(0xFF757575);
            row.addView(reasonText);

            layout.addView(row);
        }

        // Guilt-free highlight
        TextView guiltFreeText = new TextView(context);
        guiltFreeText.setText(String.format(
                "\n✅ Your guilt-free spending: %s\nAll future buckets are funded — enjoy this!",
                plan.getFormattedGuiltFree()));
        guiltFreeText.setTextColor(0xFF43A047);
        guiltFreeText.setTextSize(14);
        guiltFreeText.setPadding(0, 16, 0, 0);
        layout.addView(guiltFreeText);

        builder.setView(layout);

        builder.setPositiveButton("Confirm Allocation", (dialog, which) -> {
            if (onConfirm != null) {
                String currentMonth = YearMonth.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMM"));
                BucketSalaryPrefs.setLastAllocatedMonth(context, currentMonth);
                onConfirm.onConfirmed(plan);
            }
        });

        builder.setNeutralButton("Edit Splits", (dialog, which) -> {
            // TODO: Open split editor for manual adjustment
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    public interface OnAllocationConfirmedListener {
        void onConfirmed(BucketAllocationEngine.AllocationPlan plan);
    }
}
