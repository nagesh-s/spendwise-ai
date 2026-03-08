package com.spendwise.ai.buckets;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.spendwise.ai.R;
import com.spendwise.ai.engine.SalaryAllocationOrchestrator;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.YearMonth;

import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.pojo.BucketWithTotal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Main Buckets dashboard fragment.
 */
public class BucketsFragment extends Fragment implements BucketsAdapter.OnBucketClickListener {

    private static final int[] CUSTOM_BUCKET_COLORS = new int[] {
            Color.parseColor("#3F8AE0"),
            Color.parseColor("#00A38C"),
            Color.parseColor("#F57C00"),
            Color.parseColor("#7B61FF"),
            Color.parseColor("#D6458A"),
            Color.parseColor("#5C6BC0")
    };

    private BucketsViewModel viewModel;
    private BucketsAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyStateView;
    private LinearLayout allocationBarContainer;
    private TextView textBucketsOverview;
    private TextView textMonthStatus;
    private TextView textNetWorth;
    private ImageButton buttonToggleNetWorth;
    private MaterialButtonToggleGroup ledgerToggle;
    private boolean showNetWorth;
    private long latestNetWorthCents;
    private List<BucketWithTotal> latestBuckets = new ArrayList<>();

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
        emptyStateView = view.findViewById(R.id.layout_empty_buckets);
        allocationBarContainer = view.findViewById(R.id.layout_allocation_bar);
        textBucketsOverview = view.findViewById(R.id.text_buckets_overview);
        textMonthStatus = view.findViewById(R.id.text_month_status);
        textNetWorth = view.findViewById(R.id.text_net_worth);
        buttonToggleNetWorth = view.findViewById(R.id.btn_toggle_net_worth);
        ledgerToggle = view.findViewById(R.id.toggle_ledger_mode);

        adapter = new BucketsAdapter(this);
        int spanCount = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2;
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        recyclerView.setAdapter(adapter);

        ledgerToggle.check(R.id.btn_ledger_actual);
        adapter.setLedgerMode(BucketsAdapter.LedgerMode.ACTUAL);
        ledgerToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btn_ledger_planned) {
                adapter.setLedgerMode(BucketsAdapter.LedgerMode.PLANNED);
            } else {
                adapter.setLedgerMode(BucketsAdapter.LedgerMode.ACTUAL);
            }
        });

        showNetWorth = BucketSalaryPrefs.isShowNetWorth(requireContext());
        updateNetWorthText();
        updateMonthStatusBanner();

        buttonToggleNetWorth.setOnClickListener(v -> {
            showNetWorth = !showNetWorth;
            BucketSalaryPrefs.setShowNetWorth(requireContext(), showNetWorth);
            updateNetWorthText();
        });

        view.findViewById(R.id.btn_allocate_salary).setOnClickListener(v -> showRunAllocationDialog());

        View.OnClickListener addBucketListener = v -> showAddBucketDialog();
        view.findViewById(R.id.fab_add_bucket).setOnClickListener(addBucketListener);
        view.findViewById(R.id.btn_add_bucket_empty).setOnClickListener(addBucketListener);

        viewModel.getBucketsWithTotals().observe(getViewLifecycleOwner(), buckets -> {
            latestBuckets = buckets == null ? new ArrayList<>() : new ArrayList<>(buckets);
            adapter.submitList(latestBuckets);
            updateBucketOverview(latestBuckets);
            updateAllocationBar(latestBuckets);
            updateMonthStatusBanner();

            if (latestBuckets.isEmpty()) {
                emptyStateView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyStateView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getTotalPortfolioValue().observe(getViewLifecycleOwner(), total -> {
            latestNetWorthCents = total == null ? 0L : total;
            updateNetWorthText();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMonthStatusBanner();
    }

    @Override
    public void onBucketClick(BucketWithTotal bucketWithTotal) {
        Bundle args = new Bundle();
        args.putLong("bucketId", bucketWithTotal.bucket.getId());
        args.putString("bucketName", bucketWithTotal.bucket.getName());
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.navigation_bucket_detail, args);
    }

    private void updateAllocationBar(List<BucketWithTotal> buckets) {
        allocationBarContainer.removeAllViews();
        if (buckets.isEmpty()) {
            return;
        }

        int totalTargetPercent = 0;
        for (BucketWithTotal bwt : buckets) {
            totalTargetPercent += bwt.bucket.getTargetPercent();
        }
        boolean useEqualWeights = totalTargetPercent <= 0;

        for (BucketWithTotal bwt : buckets) {
            View segment = new View(requireContext());
            float weight = useEqualWeights ? 1f : Math.max(0f, bwt.bucket.getTargetPercent());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, weight);
            params.setMarginEnd(2);
            segment.setLayoutParams(params);
            segment.setBackgroundColor(bwt.bucket.getColorArgb());
            allocationBarContainer.addView(segment);
        }
    }

    private void showRunAllocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.buckets_run_allocation_title);

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_salary_input, null, false);
        TextInputLayout salaryInputLayout = dialogView.findViewById(R.id.input_salary_layout);
        TextInputEditText salaryInput = dialogView.findViewById(R.id.input_salary);
        long existingExpected = BucketSalaryPrefs.getExpectedSalaryCents(requireContext());
        if (existingExpected > 0) {
            salaryInput.setText(String.format(Locale.getDefault(), "%.2f", existingExpected / 100.0));
        }
        builder.setView(dialogView);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.buckets_run_allocation_cta, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Long amountCents = parseAmountToCents(salaryInput.getText().toString().trim());
            if (amountCents == null || amountCents <= 0) {
                salaryInputLayout.setError(getString(R.string.buckets_salary_invalid));
                return;
            }
            salaryInputLayout.setError(null);

            BucketSalaryPrefs.setExpectedSalaryCents(requireContext(), amountCents);
            dialog.dismiss();
            final android.content.Context appContext = requireContext().getApplicationContext();
            com.spendwise.ai.data.AppDatabase.databaseWriteExecutor.execute(() -> {
                SalaryAllocationOrchestrator.AllocationResult result = new SalaryAllocationOrchestrator(appContext)
                        .runManualAllocation(amountCents, false);
                androidx.fragment.app.FragmentActivity activity = getActivity();
                if (activity == null) {
                    return;
                }
                activity.runOnUiThread(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (result.skipped) {
                        Toast.makeText(requireContext(), R.string.buckets_allocation_already_run, Toast.LENGTH_SHORT)
                                .show();
                    } else if (result.allocated) {
                        Toast.makeText(requireContext(), R.string.buckets_allocation_ran, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), R.string.buckets_allocation_failed, Toast.LENGTH_SHORT).show();
                    }
                    updateMonthStatusBanner();
                });
            });
        }));
        dialog.show();
    }

    private void showAddBucketDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.buckets_add_dialog_title);

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_bucket_editor, null, false);
        TextInputLayout nameInputLayout = dialogView.findViewById(R.id.input_bucket_name_layout);
        TextInputEditText nameInput = dialogView.findViewById(R.id.input_bucket_name);
        TextInputEditText timelineInput = dialogView.findViewById(R.id.input_bucket_timeline);
        TextInputLayout targetInputLayout = dialogView.findViewById(R.id.input_bucket_target_layout);
        TextInputEditText targetInput = dialogView.findViewById(R.id.input_bucket_target);

        builder.setView(dialogView);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.buckets_add_dialog_save, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInputLayout.setError(getString(R.string.buckets_add_error_name));
                return;
            }
            nameInputLayout.setError(null);

            Integer targetPercent = parseTargetPercent(targetInput.getText().toString().trim());
            if (targetPercent == null) {
                targetInputLayout.setError(getString(R.string.buckets_add_error_target));
                return;
            }
            targetInputLayout.setError(null);

            int nextSortOrder = latestBuckets.size();
            int existingPercentTotal = 0;
            for (BucketWithTotal bwt : latestBuckets) {
                existingPercentTotal += bwt.bucket.getTargetPercent();
                nextSortOrder = Math.max(nextSortOrder, bwt.bucket.getSortOrder() + 1);
            }

            Bucket bucket = new Bucket(
                    name,
                    Bucket.TYPE_CUSTOM,
                    targetPercent,
                    nextColor(nextSortOrder),
                    nextSortOrder,
                    false);
            String timeline = timelineInput.getText().toString().trim();
            bucket.setTimelineLabel(timeline.isEmpty()
                    ? getString(R.string.bucket_custom_timeline_default)
                    : timeline);
            bucket.setInvestmentAdvice(getString(R.string.bucket_custom_advice_default));

            viewModel.addBucket(bucket);
            Toast.makeText(requireContext(), R.string.buckets_add_success, Toast.LENGTH_SHORT).show();

            int updatedPercent = existingPercentTotal + targetPercent;
            if (updatedPercent > 100) {
                Toast.makeText(requireContext(),
                        getString(R.string.buckets_add_total_warning, updatedPercent),
                        Toast.LENGTH_LONG).show();
            }
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void updateBucketOverview(List<BucketWithTotal> buckets) {
        int totalPercent = 0;
        for (BucketWithTotal bwt : buckets) {
            totalPercent += bwt.bucket.getTargetPercent();
        }
        textBucketsOverview.setText(getString(
                R.string.buckets_overview_format,
                buckets.size(),
                totalPercent,
                formatTargetMixHealth(totalPercent)));
    }

    private void updateMonthStatusBanner() {
        String monthKey = DateUtils.getMonthKey(YearMonth.now());
        String status = BucketSalaryPrefs.getMonthStatus(requireContext(), monthKey);
        int bgColor;
        int textColor;
        int messageId;
        if (BucketSalaryPrefs.STATUS_AUTO_ALLOCATED.equals(status)) {
            bgColor = R.color.md_theme_secondaryContainer;
            textColor = R.color.md_theme_onSecondaryContainer;
            messageId = R.string.buckets_month_status_auto;
        } else if (BucketSalaryPrefs.STATUS_FALLBACK_ALLOCATED.equals(status)) {
            bgColor = R.color.md_theme_tertiaryContainer;
            textColor = R.color.md_theme_onTertiaryContainer;
            messageId = R.string.buckets_month_status_fallback;
        } else if (BucketSalaryPrefs.STATUS_MANUAL_ALLOCATED.equals(status)) {
            bgColor = R.color.md_theme_primaryContainer;
            textColor = R.color.md_theme_onPrimaryContainer;
            messageId = R.string.buckets_month_status_manual;
        } else if (BucketSalaryPrefs.STATUS_SALARY_DETECTED.equals(status)) {
            bgColor = R.color.md_theme_secondaryContainer;
            textColor = R.color.md_theme_onSecondaryContainer;
            messageId = R.string.buckets_month_status_detected;
        } else {
            bgColor = R.color.md_theme_surfaceContainerHigh;
            textColor = R.color.md_theme_onSurfaceVariant;
            messageId = R.string.buckets_month_status_pending;
        }
        textMonthStatus.setText(getString(messageId));
        textMonthStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), bgColor));
        textMonthStatus.setTextColor(ContextCompat.getColor(requireContext(), textColor));
    }

    private void updateNetWorthText() {
        if (showNetWorth) {
            textNetWorth.setVisibility(View.VISIBLE);
            textNetWorth.setText(getString(
                    R.string.buckets_net_worth_format,
                    formatCurrency(latestNetWorthCents)));
            buttonToggleNetWorth.setImageResource(R.drawable.ic_eye);
            buttonToggleNetWorth.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_primary));
            buttonToggleNetWorth.setContentDescription(getString(R.string.buckets_hide_net_worth));
        } else {
            textNetWorth.setVisibility(View.GONE);
            buttonToggleNetWorth.setImageResource(R.drawable.ic_eye_off);
            buttonToggleNetWorth
                    .setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
            buttonToggleNetWorth.setContentDescription(getString(R.string.buckets_show_net_worth));
        }
    }

    private String formatCurrency(long amountCents) {
        return String.format(Locale.getDefault(), "₹%,.0f", amountCents / 100.0);
    }

    private Integer parseTargetPercent(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0 || parsed > 100) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseAmountToCents(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return (long) (Double.parseDouble(value) * 100);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int nextColor(int index) {
        return CUSTOM_BUCKET_COLORS[index % CUSTOM_BUCKET_COLORS.length];
    }

    private String formatTargetMixHealth(int totalPercent) {
        if (totalPercent == 100) {
            return getString(R.string.buckets_mix_balanced_short);
        }
        if (totalPercent < 100) {
            return getString(R.string.buckets_mix_under_allocated_short, 100 - totalPercent);
        }
        return getString(R.string.buckets_mix_over_allocated_short, totalPercent - 100);
    }
}
