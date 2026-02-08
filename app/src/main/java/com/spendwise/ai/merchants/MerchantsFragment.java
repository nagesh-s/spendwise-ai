package com.spendwise.ai.merchants;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.spendwise.ai.R;
import com.spendwise.ai.data.pojo.MerchantAgg;
import com.spendwise.ai.databinding.FragmentMerchantsBinding;
import com.spendwise.ai.util.DateUtils;

import java.util.List;

/**
 * Fragment for viewing merchant analytics with top merchants list.
 */
public class MerchantsFragment extends Fragment {

    private FragmentMerchantsBinding binding;
    private MerchantsViewModel viewModel;
    private MerchantAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMerchantsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MerchantsViewModel.class);

        setupRecyclerView();
        setupMonthNavigation();
        observeData();
    }

    private void setupRecyclerView() {
        adapter = new MerchantAdapter();
        binding.recyclerMerchants.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerMerchants.setAdapter(adapter);
    }

    private void setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener(v -> viewModel.goToPreviousMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.goToNextMonth());
    }

    private void observeData() {
        // Observe current month
        viewModel.getAnalyticsMonth().observe(getViewLifecycleOwner(), month -> {
            binding.textAnalyticsMonth.setText(DateUtils.getDisplayMonth(month));
        });

        // Observe top merchants
        viewModel.getTopMerchants().observe(getViewLifecycleOwner(), merchants -> {
            updateSummary(merchants);
            if (merchants == null || merchants.isEmpty()) {
                adapter.submitList(java.util.Collections.emptyList());
                binding.recyclerMerchants.setVisibility(View.GONE);
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
            } else {
                binding.recyclerMerchants.setVisibility(View.VISIBLE);
                binding.layoutEmptyState.setVisibility(View.GONE);
                adapter.submitList(merchants);
            }
        });
    }

    private void updateSummary(List<MerchantAgg> merchants) {
        if (merchants == null || merchants.isEmpty()) {
            binding.textMerchantSummaryTotal.setText(getString(R.string.no_merchant_data_summary));
            binding.textMerchantSummaryTop.setText(getString(R.string.add_expense_with_merchant_hint));
            return;
        }

        long total = 0L;
        for (MerchantAgg merchant : merchants) {
            total += merchant.getTotalCents();
        }
        MerchantAgg topMerchant = merchants.get(0);

        binding.textMerchantSummaryTotal.setText(
                getString(R.string.merchant_total_summary_format, DateUtils.formatAmountCents(total)));
        binding.textMerchantSummaryTop.setText(
                getString(
                        R.string.top_merchant_summary_format,
                        topMerchant.getName(),
                        DateUtils.formatAmountCents(topMerchant.getTotalCents())));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
