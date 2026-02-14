package com.spendwise.ai.goals;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.entity.FinancialGoal;
import com.spendwise.ai.data.repository.FinancialGoalRepository;

import java.util.List;

/**
 * ViewModel for the Goals screen.
 */
public class GoalsViewModel extends AndroidViewModel {

    private final FinancialGoalRepository repository;
    private final LiveData<List<FinancialGoal>> allGoals;
    private final LiveData<List<FinancialGoal>> activeGoals;

    public GoalsViewModel(@NonNull Application application) {
        super(application);
        repository = new FinancialGoalRepository(application);
        allGoals = repository.getAllGoals();
        activeGoals = repository.getActiveGoals();
    }

    public LiveData<List<FinancialGoal>> getAllGoals() {
        return allGoals;
    }

    public LiveData<List<FinancialGoal>> getActiveGoals() {
        return activeGoals;
    }

    public void addGoal(FinancialGoal goal) {
        repository.insert(goal);
    }

    public void updateGoal(FinancialGoal goal) {
        repository.update(goal);
    }

    public void deleteGoal(FinancialGoal goal) {
        repository.delete(goal);
    }

    public void updateProgress(long goalId, long amountCents) {
        repository.updateProgress(goalId, amountCents);
    }

    public void markComplete(long goalId) {
        repository.updateStatus(goalId, FinancialGoal.STATUS_COMPLETED);
    }

    public void pauseGoal(long goalId) {
        repository.updateStatus(goalId, FinancialGoal.STATUS_PAUSED);
    }

    public void resumeGoal(long goalId) {
        repository.updateStatus(goalId, FinancialGoal.STATUS_ACTIVE);
    }
}
