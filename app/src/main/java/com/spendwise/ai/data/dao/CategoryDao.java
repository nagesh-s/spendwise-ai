package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.Category;

import java.util.List;

/**
 * Data Access Object for Category entity.
 * Provides CRUD operations for expense categories.
 */
@Dao
public interface CategoryDao {

    // ==================== CRUD Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Category category);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Category> categories);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("DELETE FROM categories WHERE id = :categoryId AND isDefault = 0")
    int deleteByIdIfNotDefault(long categoryId);

    // ==================== Query Operations ====================

    @Query("SELECT * FROM categories WHERE id = :id")
    Category getById(long id);

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    List<Category> getAllCategoriesSync();

    @Query("SELECT * FROM categories WHERE isDefault = 1 ORDER BY name ASC")
    List<Category> getDefaultCategories();

    @Query("SELECT * FROM categories WHERE isDefault = 0 ORDER BY name ASC")
    LiveData<List<Category>> getCustomCategories();

    /**
     * Search categories by name (for autocomplete).
     */
    @Query("SELECT * FROM categories WHERE name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT :limit")
    List<Category> searchByName(String query, int limit);

    /**
     * Check if a category name already exists.
     */
    @Query("SELECT COUNT(*) FROM categories WHERE LOWER(name) = LOWER(:name)")
    int countByName(String name);

    /**
     * Get the count of all categories.
     */
    @Query("SELECT COUNT(*) FROM categories")
    int getCategoryCount();
}
