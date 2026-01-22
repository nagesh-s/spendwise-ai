package com.spendwise.ai.merchants;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.Merchant;
import com.spendwise.ai.data.pojo.MerchantAgg;
import com.spendwise.ai.data.repository.ExpenseRepository;
import com.spendwise.ai.data.repository.MerchantRepository;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.YearMonth;
import java.util.List;

/**
 * ViewModel for Merchants Analytics screen.
 * Manages top merchants list and selected merchant details.
 */
public class MerchantsViewModel extends AndroidViewModel {

    private final MerchantRepository merchantRepository;
    private final ExpenseRepository expenseRepository;

    // Current month for analytics
    private final MutableLiveData<YearMonth> analyticsMonth = new MutableLiveData<>();

    // Top merchants for current month
    private final LiveData<List<MerchantAgg>> topMerchants;

    // All merchants
    private final LiveData<List<Merchant>> allMerchants;

    // Selected merchant for details view
    private final MutableLiveData<Long> selectedMerchantId = new MutableLiveData<>();

    // Recent expenses for selected merchant
    private final LiveData<List<Expense>> selectedMerchantExpenses;

    private static final int TOP_MERCHANTS_LIMIT = 10;
    private static final int RECENT_EXPENSES_LIMIT = 20;

    public MerchantsViewModel(@NonNull Application application) {
        super(application);
        merchantRepository = new MerchantRepository(application);
        expenseRepository = new ExpenseRepository(application);

        // Initialize with current month
        analyticsMonth.setValue(YearMonth.now());

        // Load all merchants
        allMerchants = merchantRepository.getAllMerchants();

        // Top merchants update when month changes
        topMerchants = Transformations.switchMap(analyticsMonth, month -> {
            long startDay = DateUtils.getMonthStartEpochDay(month);
            long endDay = DateUtils.getMonthEndEpochDay(month);
            return merchantRepository.getTopMerchants(startDay, endDay, TOP_MERCHANTS_LIMIT);
        });

        // Selected merchant expenses
        selectedMerchantExpenses = Transformations.switchMap(selectedMerchantId, merchantId -> {
            if (merchantId == null || merchantId <= 0) {
                return new MutableLiveData<>();
            }
            return expenseRepository.getRecentExpensesByMerchant(merchantId, RECENT_EXPENSES_LIMIT);
        });
    }

    // ==================== Month Navigation ====================

    public LiveData<YearMonth> getAnalyticsMonth() {
        return analyticsMonth;
    }

    public void goToPreviousMonth() {
        YearMonth current = analyticsMonth.getValue();
        if (current != null) {
            analyticsMonth.setValue(current.minusMonths(1));
        }
    }

    public void goToNextMonth() {
        YearMonth current = analyticsMonth.getValue();
        if (current != null) {
            analyticsMonth.setValue(current.plusMonths(1));
        }
    }

    public String getCurrentMonthDisplay() {
        YearMonth month = analyticsMonth.getValue();
        return month != null ? DateUtils.getDisplayMonth(month) : "";
    }

    // ==================== Data Accessors ====================

    public LiveData<List<MerchantAgg>> getTopMerchants() {
        return topMerchants;
    }

    public LiveData<List<Merchant>> getAllMerchants() {
        return allMerchants;
    }

    // ==================== Merchant Selection ====================

    public void selectMerchant(long merchantId) {
        selectedMerchantId.setValue(merchantId);
    }

    public LiveData<Long> getSelectedMerchantId() {
        return selectedMerchantId;
    }

    public LiveData<List<Expense>> getSelectedMerchantExpenses() {
        return selectedMerchantExpenses;
    }
}
