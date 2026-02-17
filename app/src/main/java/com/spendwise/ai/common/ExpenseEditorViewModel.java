package com.spendwise.ai.common;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Account;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.Merchant;
import com.spendwise.ai.data.entity.Tag;
import com.spendwise.ai.data.repository.AccountRepository;
import com.spendwise.ai.data.repository.CategoryRepository;
import com.spendwise.ai.data.repository.ExpenseRepository;
import com.spendwise.ai.data.repository.MerchantRepository;
import com.spendwise.ai.data.repository.TagRepository;
import com.spendwise.ai.gamification.GamificationEngine;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ViewModel for ExpenseEditorDialog.
 * Manages expense creation/editing form state and provides category/merchant
 * suggestions.
 */
public class ExpenseEditorViewModel extends AndroidViewModel {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final MerchantRepository merchantRepository;
    private final TagRepository tagRepository;
    private final AccountRepository accountRepository;
    private final GamificationEngine gamificationEngine;

    // All categories for dropdown
    private final LiveData<List<Category>> allCategories;

    // All merchants for autocomplete
    private final LiveData<List<Merchant>> allMerchants;

    // All tags
    private final LiveData<List<Tag>> allTags;

    // All accounts
    private final LiveData<List<Account>> allAccounts;

    // Form state
    private final MutableLiveData<Long> expenseId = new MutableLiveData<>();
    private final MutableLiveData<Long> amountCents = new MutableLiveData<>();
    private final MutableLiveData<String> description = new MutableLiveData<>();
    private final MutableLiveData<Long> selectedCategoryId = new MutableLiveData<>();
    private final MutableLiveData<String> merchantName = new MutableLiveData<>();
    private final MutableLiveData<LocalDate> selectedDate = new MutableLiveData<>();
    private final MutableLiveData<Long> selectedAccountId = new MutableLiveData<>();

    // Selected tags
    private final Set<Long> selectedTagIds = new HashSet<>();

    // Validation errors
    private final MutableLiveData<String> amountError = new MutableLiveData<>();
    private final MutableLiveData<String> categoryError = new MutableLiveData<>();

    // Save result
    private final MutableLiveData<Boolean> saveComplete = new MutableLiveData<>();
    private final MutableLiveData<String> saveMessage = new MutableLiveData<>();

    // Loaded expense when editing
    private final MutableLiveData<Expense> editingExpense = new MutableLiveData<>();

    public ExpenseEditorViewModel(@NonNull Application application) {
        super(application);
        expenseRepository = new ExpenseRepository(application);
        categoryRepository = new CategoryRepository(application);
        merchantRepository = new MerchantRepository(application);
        tagRepository = new TagRepository(application);
        accountRepository = new AccountRepository(application);
        gamificationEngine = new GamificationEngine(application);

        allCategories = categoryRepository.getAllCategories();
        allMerchants = merchantRepository.getAllMerchants();
        allTags = tagRepository.getAllTags();
        allAccounts = accountRepository.getAllAccounts();

        // Initialize with today's date
        selectedDate.setValue(LocalDate.now());
    }

    // ==================== Form Initialization ====================

    /**
     * Initialize for creating a new expense on a specific date.
     */
    public void initForNewExpense(long dateEpochDay) {
        expenseId.setValue(null);
        selectedDate.setValue(LocalDate.ofEpochDay(dateEpochDay));
        // Reset fields
        amountCents.setValue(null);
        description.setValue(null);
        merchantName.setValue(null);
        selectedCategoryId.setValue(null);
        selectedAccountId.setValue(null);
        editingExpense.setValue(null);
        clearErrors();
    }

    /**
     * Initialize for creating a new expense from voice input.
     */
    public void initForVoiceExpense(long dateEpochDay, long amount, String merchant, String category, String desc) {
        initForNewExpense(dateEpochDay);
        if (amount > 0)
            amountCents.setValue(amount);
        if (merchant != null)
            merchantName.setValue(merchant);
        if (desc != null)
            description.setValue(desc);

        if (category != null) {
            // Try to resolve category immediately if list is loaded, or wait
            List<Category> currentCats = allCategories.getValue();
            if (currentCats != null) {
                resolveCategory(currentCats, category);
            } else {
                // Will resolve when observed
                pendingVoiceCategory = category;
            }
        }
    }

    private String pendingVoiceCategory;

    public void onCategoriesLoaded(List<Category> categories) {
        if (pendingVoiceCategory != null) {
            resolveCategory(categories, pendingVoiceCategory);
            pendingVoiceCategory = null;
        }
    }

    private void resolveCategory(List<Category> categories, String name) {
        for (Category cat : categories) {
            if (cat.getName().equalsIgnoreCase(name)) {
                selectedCategoryId.setValue(cat.getId());
                return;
            }
        }
    }

    /**
     * Initialize for editing an existing expense.
     */
    public void initForEdit(long existingExpenseId) {
        expenseId.setValue(existingExpenseId);
        clearErrors();
        loadExpense(existingExpenseId);
    }

    private void loadExpense(long existingExpenseId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Expense existingExpense = expenseRepository.getExpenseByIdSync(existingExpenseId);
            if (existingExpense == null) {
                return;
            }

            editingExpense.postValue(existingExpense);
            amountCents.postValue(existingExpense.getAmountCents());
            description.postValue(existingExpense.getDescription() != null ? existingExpense.getDescription() : "");
            selectedCategoryId.postValue(existingExpense.getCategoryId());
            selectedDate.postValue(DateUtils.epochDayToDate(existingExpense.getDateEpochDay()));
            selectedAccountId.postValue(existingExpense.getAccountId());
        });
    }

    // ==================== Form Getters/Setters ====================

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public LiveData<List<Merchant>> getAllMerchants() {
        return allMerchants;
    }

    public MutableLiveData<Long> getAmountCents() {
        return amountCents;
    }

    public LiveData<Expense> getEditingExpense() {
        return editingExpense;
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

    // ==================== Account Operations ====================

    public LiveData<List<Account>> getAllAccounts() {
        return allAccounts;
    }

    public MutableLiveData<Long> getSelectedAccountId() {
        return selectedAccountId;
    }

    public void setSelectedAccountId(Long accountId) {
        selectedAccountId.setValue(accountId);
    }

    // ==================== Tag Operations ====================

    public LiveData<List<Tag>> getAllTags() {
        return allTags;
    }

    public Set<Long> getSelectedTagIds() {
        return selectedTagIds;
    }

    public void toggleTag(long tagId) {
        if (selectedTagIds.contains(tagId)) {
            selectedTagIds.remove(tagId);
        } else {
            selectedTagIds.add(tagId);
        }
    }

    public void loadTagsForExpense(long expId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Tag> tags = tagRepository.getTagsForExpenseSync(expId);
            if (tags != null) {
                selectedTagIds.clear();
                for (Tag tag : tags) {
                    selectedTagIds.add(tag.getId());
                }
            }
        });
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

    public LiveData<String> getSaveMessage() {
        return saveMessage;
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

        // Capture tag IDs before moving to background thread
        final List<Long> tagIds = new ArrayList<>(selectedTagIds);
        final Long accountIdVal = selectedAccountId.getValue();
        final Long existingId = expenseId.getValue();
        final boolean isNewExpense = existingId == null || existingId <= 0;

        // Run all DB operations on background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Handle merchant (create if new)
            Long merchantId = null;
            if (merchant != null && !merchant.trim().isEmpty()) {
                merchantId = merchantRepository.insertOrGetId(merchant.trim());
                if (merchantId == -1) {
                    merchantId = null;
                }
            }

            // Create expense
            Expense expense = new Expense(
                    date.toEpochDay(),
                    amount,
                    desc,
                    categoryId,
                    merchantId);

            // Set account if selected
            if (accountIdVal != null && accountIdVal > 0) {
                expense.setAccountId(accountIdVal);
            }

            String msg;
            if (!isNewExpense) {
                expense.setId(existingId);
                expenseRepository.update(expense);
                // Update tags
                tagRepository.setTagsForExpense(existingId, new ArrayList<>(tagIds));
                msg = "Expense updated.";
            } else {
                long newId = expenseRepository.insertSync(expense);
                // Save tags for new expense
                if (!tagIds.isEmpty()) {
                    tagRepository.setTagsForExpense(newId, new ArrayList<>(tagIds));
                }

                // Award XP for new expense
                gamificationEngine.addXp(10);

                // Check achievements for new expense events only
                List<Expense> allExpenses = expenseRepository.getAllExpensesSync();
                boolean earlyBirdEvent = LocalTime.now().getHour() < 8;
                List<String> unlocked = gamificationEngine.checkAchievements(allExpenses, earlyBirdEvent);

                msg = "Expense saved! +10 XP";
                if (!unlocked.isEmpty()) {
                    msg += "\nUnlocks: " + unlocked.size() + " Badge(s)!";
                }
            }
            saveMessage.postValue(msg);

            // Post result back to main thread
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> saveComplete.setValue(true));
        });

    }

    public void resetSaveComplete() {
        saveComplete.setValue(false);
    }

    /**
     * Delete the current expense being edited.
     */
    public void deleteExpense() {
        Long existingId = expenseId.getValue();
        if (existingId != null && existingId > 0) {
            expenseRepository.deleteById(existingId);
        }
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
