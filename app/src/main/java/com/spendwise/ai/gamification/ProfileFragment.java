package com.spendwise.ai.gamification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;
import com.spendwise.ai.data.repository.ExpenseRepository;

public class ProfileFragment extends Fragment {

    private GamificationEngine gamificationEngine;
    private ExpenseRepository expenseRepository;

    private TextView textLevelTitle;
    private TextView textLevelNumber;
    private ProgressBar progressXp;
    private TextView textXp;
    private TextView textStreak;
    private TextView textTotalXp;
    private RecyclerView recyclerAchievements;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gamificationEngine = new GamificationEngine(requireContext());
        expenseRepository = new ExpenseRepository(requireActivity().getApplication());

        textLevelTitle = view.findViewById(R.id.text_level_title);
        textLevelNumber = view.findViewById(R.id.text_level_number);
        progressXp = view.findViewById(R.id.progress_xp);
        textXp = view.findViewById(R.id.text_xp);
        textStreak = view.findViewById(R.id.text_streak);
        textTotalXp = view.findViewById(R.id.text_total_xp);
        recyclerAchievements = view.findViewById(R.id.recycler_achievements);

        setupAchievements();
        updateStats();
        observeStreak();
    }

    private void setupAchievements() {
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        recyclerAchievements.setLayoutManager(layoutManager);
        recyclerAchievements.setAdapter(new AchievementAdapter(gamificationEngine.getAllAchievements()));
    }

    private void updateStats() {
        int level = gamificationEngine.getLevel();
        long currentXp = gamificationEngine.getXp();
        long xpProgress = gamificationEngine.getXpProgress();
        long xpToNext = gamificationEngine.getXpToNextLevel();

        textLevelTitle.setText(gamificationEngine.getLevelTitle());
        textLevelNumber.setText("Level " + level);
        textTotalXp.setText(String.valueOf(currentXp));

        progressXp.setMax((int) xpToNext);
        progressXp.setProgress((int) xpProgress);
        textXp.setText(xpProgress + " / " + xpToNext + " XP");
    }

    private void observeStreak() {
        expenseRepository.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                int streak = gamificationEngine.calculateCurrentStreak(expenses);
                textStreak.setText(streak + " Days");
            }
        });
    }
}
