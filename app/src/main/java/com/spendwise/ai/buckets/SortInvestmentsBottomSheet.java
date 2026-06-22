package com.spendwise.ai.buckets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import com.spendwise.ai.R;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.engine.BucketAssignmentEngine;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SortInvestmentsBottomSheet extends BottomSheetDialogFragment {

    private BucketsViewModel viewModel;
    private TextView textSortHeader;
    private TextView textSortCount;
    private MaterialCardView cardInvestment;
    private TextView textSortAmount;
    private TextView textSortName;
    private TextView textSortPlatform;
    private RecyclerView recyclerSortBuckets;
    private View btnSkip;

    private List<Holding> unassignedHoldings = new ArrayList<>();
    private List<Bucket> allBuckets = new ArrayList<>();
    private int currentIndex = 0;
    private SortBucketsAdapter bucketsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_sort_investments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(BucketsViewModel.class);

        textSortHeader = view.findViewById(R.id.text_sort_header);
        textSortCount = view.findViewById(R.id.text_sort_count);
        cardInvestment = view.findViewById(R.id.card_investment_to_sort);
        textSortAmount = view.findViewById(R.id.text_sort_amount);
        textSortName = view.findViewById(R.id.text_sort_name);
        textSortPlatform = view.findViewById(R.id.text_sort_platform);
        recyclerSortBuckets = view.findViewById(R.id.recycler_sort_buckets);
        btnSkip = view.findViewById(R.id.btn_skip_sort);

        int spanCount = getResources().getConfiguration().screenWidthDp >= 600 ? 4 : 3;
        recyclerSortBuckets.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        bucketsAdapter = new SortBucketsAdapter();
        recyclerSortBuckets.setAdapter(bucketsAdapter);

        // Load buckets
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<Bucket> buckets = db.bucketDao().getAllBucketsSync();
            // Filter out UNASSIGNED bucket from the list of choices
            List<Bucket> validChoices = new ArrayList<>();
            for (Bucket b : buckets) {
                if (!Bucket.TYPE_UNASSIGNED.equals(b.getType())) {
                    validChoices.add(b);
                }
            }
            requireActivity().runOnUiThread(() -> {
                allBuckets = validChoices;
                bucketsAdapter.notifyDataSetChanged();
            });
        }).start();

        viewModel.getUnassignedHoldings().observe(getViewLifecycleOwner(), holdings -> {
            if (holdings != null) {
                unassignedHoldings = holdings;
                if (currentIndex >= unassignedHoldings.size()) {
                    // We finished sorting!
                    dismiss();
                } else {
                    displayCurrentHolding();
                }
            }
        });

        btnSkip.setOnClickListener(v -> {
            animateCardOut(() -> {
                currentIndex++;
                if (currentIndex >= unassignedHoldings.size()) {
                    dismiss();
                } else {
                    displayCurrentHolding();
                }
            });
        });
    }

    private void displayCurrentHolding() {
        if (currentIndex >= unassignedHoldings.size()) return;
        
        Holding holding = unassignedHoldings.get(currentIndex);
        
        // Format amount
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        format.setMaximumFractionDigits(0);
        double amountInRupees = holding.getInvestedAmountCents() / 100.0;
        textSortAmount.setText(format.format(amountInRupees));
        
        textSortName.setText(holding.getName());
        
        String platform = holding.getPlatform() != null ? holding.getPlatform() : "Unknown Platform";
        String assetType = holding.getAssetType() != null ? holding.getAssetType() : "Investment";
        textSortPlatform.setText("via " + platform + " • " + assetType);
        
        textSortCount.setText((currentIndex + 1) + " of " + unassignedHoldings.size());
        
        // Reset card state if it was animated out
        cardInvestment.setTranslationX(0f);
        cardInvestment.setAlpha(1f);
    }

    private void animateCardOut(Runnable onAnimationEnd) {
        cardInvestment.animate()
                .translationX(cardInvestment.getWidth())
                .alpha(0f)
                .setDuration(250)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        onAnimationEnd.run();
                    }
                })
                .start();
    }

    private void assignHoldingToBucket(Holding holding, Bucket bucket) {
        android.content.Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            holding.setBucketId(bucket.getId());
            db.investmentDao().update(holding);
            
            // Learn the mapping so the ML Engine auto-assigns it next time!
            new BucketAssignmentEngine(appContext)
                    .learnFromHolding(db, holding, bucket.getId(), "USER_SORT");
                    
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                Toast.makeText(appContext, "Assigned to " + bucket.getName(), Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private class SortBucketsAdapter extends RecyclerView.Adapter<SortBucketsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sort_bucket_button, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Bucket bucket = allBuckets.get(position);
            holder.textName.setText(bucket.getName());
            
            // Just use a generic icon for now, the real app uses the bucket.getIconName() mapper
            holder.icon.setImageResource(R.drawable.ic_launcher_foreground);

            holder.itemView.setOnClickListener(v -> {
                if (currentIndex >= unassignedHoldings.size()) return;
                Holding currentHolding = unassignedHoldings.get(currentIndex);
                
                // Disable clicks temporarily
                recyclerSortBuckets.setEnabled(false);
                
                assignHoldingToBucket(currentHolding, bucket);
                
                animateCardOut(() -> {
                    recyclerSortBuckets.setEnabled(true);
                    currentIndex++;
                    if (currentIndex >= unassignedHoldings.size()) {
                        dismiss();
                    } else {
                        displayCurrentHolding();
                    }
                });
            });
        }

        @Override
        public int getItemCount() {
            return allBuckets.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView textName;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.icon_bucket);
                textName = itemView.findViewById(R.id.text_bucket_name);
            }
        }
    }
}
