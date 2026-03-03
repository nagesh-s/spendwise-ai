package com.spendwise.ai.buckets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;
import com.spendwise.ai.data.pojo.BucketWithTotal;

/**
 * Main Buckets dashboard fragment.
 *
 * Shows allocation bar, bucket cards (each with its own value — no combined
 * total),
 * guilt-free banner, and allocate salary button.
 * This is the "anti-Upstox" screen.
 */
public class BucketsFragment extends Fragment implements BucketsAdapter.OnBucketClickListener {

    private BucketsViewModel viewModel;
    private BucketsAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private LinearLayout allocationBarContainer;
    private TextView textNetWorth;
    private boolean showNetWorth = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buckets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(BucketsViewModel.class);

        recyclerView = view.findViewById(R.id.recycler_buckets);
        emptyText = view.findViewById(R.id.text_empty_buckets);
        allocationBarContainer = view.findViewById(R.id.layout_allocation_bar);
        textNetWorth = view.findViewById(R.id.text_net_worth);

        adapter = new BucketsAdapter(this);
        recyclerView.setAdapter(adapter);

        // "Show Net Worth" toggle (hidden by default — anti-Upstox UX)
        View btnToggleNetWorth = view.findViewById(R.id.btn_toggle_net_worth);
        btnToggleNetWorth.setOnClickListener(v -> {
            showNetWorth = !showNetWorth;
            textNetWorth.setVisibility(showNetWorth ? View.VISIBLE : View.GONE);
        });

        // Allocate Salary button
        view.findViewById(R.id.btn_allocate_salary).setOnClickListener(v -> {
            long expectedSalary = BucketSalaryPrefs.getExpectedSalaryCents(requireContext());
            if (expectedSalary > 0) {
                // Use saved expected salary
                AllocationSuggestionDialog.show(requireContext(), expectedSalary, plan -> {
                    // Apply allocations to bucket balances
                    for (com.spendwise.ai.engine.BucketAllocationEngine.AllocationResult alloc : plan
                            .getAllocations()) {
                        viewModel.updateBalance(alloc.getBucketId(), alloc.getAllocatedAmountCents());
                    }
                });
            } else {
                // First time — ask user for salary via simple dialog
                showManualSalaryDialog();
            }
        });

        // FAB → add custom bucket
        view.findViewById(R.id.fab_add_bucket).setOnClickListener(v -> {
            // TODO: Open BucketEditorDialog
        });

        // Observe buckets
        viewModel.getBucketsWithTotals().observe(getViewLifecycleOwner(), buckets -> {
            adapter.submitList(buckets);

            if (buckets == null || buckets.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                allocationBarContainer.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                allocationBarContainer.setVisibility(View.VISIBLE);
                updateAllocationBar(buckets);
            }
        });

        // Observe total portfolio value for the hidden net worth display
        viewModel.getTotalPortfolioValue().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                textNetWorth.setText(String.format("Total Net Worth: ₹%,.0f", total / 100.0));
            }
        });
    }

    @Override
    public void onBucketClick(BucketWithTotal bucketWithTotal) {
        Bundle args = new Bundle();
        args.putLong("bucketId", bucketWithTotal.bucket.getId());
        args.putString("bucketName", bucketWithTotal.bucket.getName());
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.navigation_bucket_detail, args);
    }

    /**
     * Build the 5-segment allocation bar from bucket data.
     * Each segment width is proportional to the bucket's targetPercent.
     */
    private void updateAllocationBar(java.util.List<BucketWithTotal> buckets) {
        allocationBarContainer.removeAllViews();
        int totalWidth = allocationBarContainer.getWidth();
        if (totalWidth <= 0) {
            // Post for after layout
            allocationBarContainer.post(() -> updateAllocationBar(buckets));
            return;
        }

        for (BucketWithTotal bwt : buckets) {
            View segment = new View(requireContext());
            int percent = bwt.bucket.getTargetPercent();
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, percent);
            params.setMarginEnd(2); // Small gap between segments
            segment.setLayoutParams(params);
            segment.setBackgroundColor(bwt.bucket.getColorArgb());
            allocationBarContainer.addView(segment);
        }
    }

    /**
     * Show a simple input dialog to set the expected salary (first-time or manual
     * entry).
     * Salary is saved and reused — user just confirms/edits each month.
     */
    private void showManualSalaryDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Set Your Monthly Salary");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 0);

        android.widget.TextView hint = new android.widget.TextView(requireContext());
        hint.setText("Enter your monthly salary. This is saved and reused — you'll just confirm each month.");
        hint.setTextSize(13);
        hint.setPadding(0, 0, 0, 16);
        layout.addView(hint);

        android.widget.EditText salaryInput = new android.widget.EditText(requireContext());
        salaryInput.setHint("Monthly salary (₹)");
        salaryInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(salaryInput);

        builder.setView(layout);

        builder.setPositiveButton("Save & Allocate", (dialog, which) -> {
            String amountStr = salaryInput.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                try {
                    long amountCents = (long) (Double.parseDouble(amountStr) * 100);
                    BucketSalaryPrefs.setExpectedSalaryCents(requireContext(), amountCents);
                    // Now trigger allocation
                    AllocationSuggestionDialog.show(requireContext(), amountCents, plan -> {
                        for (com.spendwise.ai.engine.BucketAllocationEngine.AllocationResult alloc : plan
                                .getAllocations()) {
                            viewModel.updateBalance(alloc.getBucketId(), alloc.getAllocatedAmountCents());
                        }
                    });
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
