package com.spendwise.ai.common;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.Merchant;
import com.spendwise.ai.data.repository.CategoryRepository;
import com.spendwise.ai.data.repository.ExpenseRepository;
import com.spendwise.ai.data.repository.MerchantRepository;
import com.spendwise.ai.util.DateUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * ViewModel for ExpenseEditorDialog.
 * Manages expense creation/editing form state and provides category/merchant suggestions.
 */
public class ExpenseEditorViewModel extends AndroidViewModel {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final MerchantRepository merchantRepository;

    // All categories for dropdown
    private final LiveData<List<Category>> allCategories;

    // Form state
    private final MutableLiveData<Long> expenseId = new MutableLiveData<>();
    private final MutableLiveData<Long> amountCents = new MutableLiveData<>();
    private final MutableLiveData<String> description = new MutableLiveData<>();
    private final MutableLiveData<Long> selectedCategoryId = new MutableLiveData<>();
    private final MutableLiveData<String> merchantName = new MutableLiveData<>();
    private final MutableLiveData<LocalDate> selectedDate = new MutableLiveData<>();

    // Validation errors
    private final MutableLiveData<String> amountError = new MutableLiveData<>();
    private final MutableLiveData<String> categoryError = new MutableLiveData<>();

    // Save result
    private final MutableLiveData<Boolean> saveComplete = new MutableLiveData<>();

    public ExpenseEditorViewModel(@NonNull Application application) {
        super(application);
        expenseRepository = new ExpenseRepository(application);
        categoryRepository = new CategoryRepository(application);
        merchantRepository = new MerchantRepository(application);

        allCategories = categoryRepository.getAllCategories();

        // Initialize with today's date
        selectedDate.setValue(LocalDate.now());
    }

    // ==================== Form Initialization ====================

    /**
     * Initialize for creating a new expense on a specific date.
     */
    public void initForNewExpense(long dateEpochDay) {
        expenseId.setValue(null);
        amountCents.setValue(0L);
        description.setValue("");
        selectedCategoryId.setValue(null);
        merchantName.setValue("");
        selectedDate.setValue(DateUtils.epochDayToDate(dateEpochDay));
        clearErrors();
    }

    /**
     * Initialize for editing an existing expense.
     */
    public void initForEdit(long existingExpenseId) {
        // Load expense in background and populate form
        // This would need to be done via a callback or by observing
        expenseId.setValue(existingExpenseId);
        // Note: Actual loading would happen when the fragment observes and calls loadExpense()
    }

    // ==================== Form Getters/Setters ====================

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public MutableLiveData<Long> getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long cents) {
        amountCents.setValue(cents);
        if (cents > 0) {
            amountError.setValue(null);
        }
    }

    public void setAmountFromString(String amountStr) {
        long cents = DateUtils.parseAmountToCents(amountStr);
        setAmountCents(cents);
    }

    public MutableLiveData<String> getDescription() {
        return description;
    }

    public void setDescription(String desc) {
        description.setValue(desc);
    }

    public MutableLiveData<Long> getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(long categoryId) {
        selectedCategoryId.setValue(categoryId);
        categoryError.setValue(null);
    }

    public MutableLiveData<String> getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String name) {
        merchantName.setValue(name);
    }

    public MutableLiveData<LocalDate> getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate date) {
        selectedDate.setValue(date);
    }

    // ==================== Validation ====================

    public LiveData<String> getAmountError() {
        return amountError;
    }

    public LiveData<String> getCategoryError() {
        return categoryError;
    }

    private void clearErrors() {
        amountError.setValue(null);
        categoryError.setValue(null);
    }

    private boolean validate() {
        boolean valid = true;

        Long amount = amountCents.getValue();
        if (amount == null || amount <= 0) {
            amountError.setValue("Please enter a valid amount");
            valid = false;
        }

        Long categoryId = selectedCategoryId.getValue();
        if (categoryId == null || categoryId <= 0) {
            categoryError.setValue("Please select a category");
            valid = false;
        }

        return valid;
    }

    // ==================== Save Expense ====================

    public LiveData<Boolean> getSaveComplete() {
        return saveComplete;
    }

    public void saveExpense() {
        if (!validate()) {
            return;
        }

        Long amount = amountCents.getValue();
        String desc = description.getValue();
        Long categoryId = selectedCategoryId.getValue();
        String merchant = merchantName.getValue();
        LocalDate date = selectedDate.getValue();

        if (amount == null || categoryId == null || date == null) {
            return;
        }

        // Handle merchant (create if new)
        Long merchantId = null;
        if (merchant != null && !merchant.trim().isEmpty()) {
            merchantId = merchantRepository.insertOrGetId(merchant.trim());
            if (merchantId == -1) {
                merchantId = null; // Failed to get/create merchant
            }
        }

        // Create expense
        Expense expense = new Expense(
                date.toEpochDay(),
                amount,
                desc,
                categoryId,
                merchantId
        );

        Long existingId = expenseId.getValue();
        if (existingId != null && existingId > 0) {
            expense.setId(existingId);
            expenseRepository.update(expense);
        } else {
            expenseRepository.insert(expense);
        }

        saveComplete.setValue(true);
    }

    public void resetSaveComplete() {
        saveComplete.setValue(false);
    }

    // ==================== Merchant Suggestions ====================

    /**
     * Search merchants for autocomplete suggestions.
     * Should be called from background thread.
     */
    public List<Merchant> searchMerchants(String query) {
        return merchantRepository.searchByName(query, 5);
    }
}
