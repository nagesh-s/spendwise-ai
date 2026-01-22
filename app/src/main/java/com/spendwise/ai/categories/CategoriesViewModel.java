package com.spendwise.ai.categories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.pojo.BudgetWithSpend;
import com.spendwise.ai.data.repository.BudgetRepository;
import com.spendwise.ai.data.repository.CategoryRepository;
import com.spendwise.ai.util.DateUtils;

import java.time.YearMonth;
import java.util.List;

/**
 * ViewModel for Categories & Budgets screen.
 * Manages categories list and budget progress for selected month.
 */
public class CategoriesViewModel extends AndroidViewModel {

    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;

    // All categories
    private final LiveData<List<Category>> allCategories;

    // Current month for budgets
    private final MutableLiveData<YearMonth> budgetMonth = new MutableLiveData<>();

    // Budget progress for current month
    private final LiveData<List<BudgetWithSpend>> budgetsWithSpend;

    public CategoriesViewModel(@NonNull Application application) {
        super(application);
        categoryRepository = new CategoryRepository(application);
        budgetRepository = new BudgetRepository(application);

        // Load all categories
        allCategories = categoryRepository.getAllCategories();

        // Initialize with current month
        budgetMonth.setValue(YearMonth.now());

        // Budget progress updates when month changes
        budgetsWithSpend = Transformations.switchMap(budgetMonth, month -> {
            String monthKey = DateUtils.getMonthKey(month);
            long startDay = DateUtils.getMonthStartEpochDay(month);
            long endDay = DateUtils.getMonthEndEpochDay(month);
            return budgetRepository.getBudgetsWithSpend(monthKey, startDay, endDay);
        });
    }

    // ==================== Categories ====================

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public void addCategory(String name, int colorArgb, String iconKey) {
        Category category = new Category(name, colorArgb, iconKey, false);
        categoryRepository.insert(category);
    }

    public void updateCategory(Category category) {
        categoryRepository.update(category);
    }

    public void deleteCategory(long categoryId) {
        categoryRepository.deleteIfNotDefault(categoryId);
    }

    // ==================== Budget Month Navigation ====================

    public LiveData<YearMonth> getBudgetMonth() {
        return budgetMonth;
    }

    public void goToPreviousMonth() {
        YearMonth current = budgetMonth.getValue();
        if (current != null) {
            budgetMonth.setValue(current.minusMonths(1));
        }
    }

    public void goToNextMonth() {
        YearMonth current = budgetMonth.getValue();
        if (current != null) {
            budgetMonth.setValue(current.plusMonths(1));
        }
    }

    public String getCurrentMonthDisplay() {
        YearMonth month = budgetMonth.getValue();
        return month != null ? DateUtils.getDisplayMonth(month) : "";
    }

    // ==================== Budgets ====================

    public LiveData<List<BudgetWithSpend>> getBudgetsWithSpend() {
        return budgetsWithSpend;
    }

    public void setBudget(long categoryId, long limitCents) {
        YearMonth month = budgetMonth.getValue();
        if (month != null) {
            String monthKey = DateUtils.getMonthKey(month);
            budgetRepository.upsert(categoryId, monthKey, limitCents);
        }
    }

    public void copyBudgetsFromPreviousMonth() {
        YearMonth current = budgetMonth.getValue();
        if (current != null) {
            String currentMonthKey = DateUtils.getMonthKey(current);
            String previousMonthKey = DateUtils.getMonthKey(current.minusMonths(1));
            budgetRepository.copyBudgetsToNewMonth(previousMonthKey, currentMonthKey);
        }
    }
}
