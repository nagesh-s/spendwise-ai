package com.spendwise.ai.review;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;
import com.spendwise.ai.data.pojo.ReviewCandidateWithEvent;

public class ReviewQueueFragment extends Fragment implements ReviewQueueAdapter.OnCandidateActionListener {

    private ReviewQueueViewModel viewModel;
    private ReviewQueueAdapter adapter;
    private TextView emptyStateText;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_review_queue, container, false);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        recyclerView = view.findViewById(R.id.recyclerView);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReviewQueueAdapter(this);
        recyclerView.setAdapter(adapter);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ReviewQueueViewModel.class);
        
        viewModel.getPendingCandidates().observe(getViewLifecycleOwner(), candidates -> {
            adapter.submitList(candidates);
            if (candidates == null || candidates.isEmpty()) {
                emptyStateText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyStateText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onConfirmSuggestion(ReviewCandidateWithEvent item) {
        viewModel.acceptSuggestion(item);
        Toast.makeText(getContext(), "Suggestion confirmed!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChangeRequested(ReviewCandidateWithEvent item) {
        // In a real app, this would open a BottomSheetDialog to select a holding.
        // For simplicity, we just show a toast right now.
        Toast.makeText(getContext(), "Opening manual selection...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onIgnore(ReviewCandidateWithEvent item) {
        viewModel.ignoreCandidate(item.candidate.getId());
        Toast.makeText(getContext(), "Ignored message", Toast.LENGTH_SHORT).show();
    }
}
