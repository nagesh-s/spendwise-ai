package com.spendwise.ai.buckets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.Investment;

/**
 * Drill-down fragment for a single bucket.
 * Shows: bucket header (name, timeline, advice), investments list,
 * linked categories, and per-bucket spending analysis.
 */
public class BucketDetailFragment extends Fragment implements InvestmentAdapter.OnInvestmentClickListener {

    private BucketDetailViewModel viewModel;
    private InvestmentAdapter adapter;
    private long bucketId = -1;
    private String bucketName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bucket_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(BucketDetailViewModel.class);

        // Get bucket info from arguments
        if (getArguments() != null) {
            bucketId = getArguments().getLong("bucketId", -1);
            bucketName = getArguments().getString("bucketName", "Bucket");
        }

        RecyclerView recyclerInvestments = view.findViewById(R.id.recycler_investments);
        TextView textBucketName = view.findViewById(R.id.text_detail_bucket_name);
        TextView emptyText = view.findViewById(R.id.text_empty_investments);

        textBucketName.setText(bucketName);

        adapter = new InvestmentAdapter(this);
        recyclerInvestments.setAdapter(adapter);

        // FAB → add investment
        view.findViewById(R.id.fab_add_investment).setOnClickListener(v -> {
            // TODO: Open InvestmentEditorDialog
        });

        // Observe investments for this bucket
        if (bucketId >= 0) {
            viewModel.getInvestmentsForBucket(bucketId).observe(getViewLifecycleOwner(), investments -> {
                adapter.submitList(investments);
                if (investments == null || investments.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    recyclerInvestments.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    recyclerInvestments.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public void onInvestmentClick(Investment investment) {
        // TODO: Open InvestmentEditorDialog for editing
    }
}
