package com.spendwise.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.CategoryDao;
import com.spendwise.ai.data.entity.Category;

import java.util.List;

/**
 * Repository for Category data access.
 * Provides a clean API for UI to interact with category data.
 */
public class CategoryRepository {

    private final CategoryDao categoryDao;

    public CategoryRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        categoryDao = db.categoryDao();
    }

    // ==================== Insert/Update/Delete ====================

    public void insert(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.insert(category));
    }

    public void update(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.update(category));
    }

    /**
     * Delete a category if it's not a default category.
     * Returns number of rows deleted (0 if default category).
     */
    public void deleteIfNotDefault(long categoryId) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.deleteByIdIfNotDefault(categoryId));
    }

    // ==================== Queries ====================

    public LiveData<List<Expense>> getAllExpenses() {
        return expenseDao.getAllExpenses();
    }

    public List<Expense> getAllExpensesSync() {
        return expenseDao.getAllExpensesSync();
    }

    public LiveData<List<Category>> getAllCategories() {
        return categoryDao.getAllCategories();
    }

    public LiveData<List<Category>> getCustomCategories() {
        return categoryDao.getCustomCategories();
    }

    public List<Category> getAllCategoriesSync() {
        return categoryDao.getAllCategoriesSync();
    }

    /**
     * Get category by ID (synchronous, call from background thread).
     */
    public Category getByIdSync(long id) {
        return categoryDao.getById(id);
    }

    /**
     * Search categories by name for autocomplete.
     */
    public List<Category> searchByName(String query, int limit) {
        return categoryDao.searchByName(query, limit);
    }
}
