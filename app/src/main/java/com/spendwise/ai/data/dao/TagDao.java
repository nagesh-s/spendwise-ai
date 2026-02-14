package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.ExpenseTag;
import com.spendwise.ai.data.entity.Tag;

import java.util.List;

/**
 * Data Access Object for Tag and ExpenseTag entities.
 */
@Dao
public interface TagDao {

    // ==================== Tag CRUD ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Tag tag);

    @Update
    void update(Tag tag);

    @Delete
    void delete(Tag tag);

    @Query("SELECT * FROM tags ORDER BY name ASC")
    LiveData<List<Tag>> getAllTags();

    @Query("SELECT * FROM tags ORDER BY name ASC")
    List<Tag> getAllTagsSync();

    @Query("SELECT * FROM tags WHERE id = :id")
    Tag getById(long id);

    @Query("SELECT COUNT(*) FROM tags")
    int getTagCount();

    // ==================== ExpenseTag Operations ====================

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertExpenseTag(ExpenseTag expenseTag);

    @Query("DELETE FROM expense_tags WHERE expenseId = :expenseId AND tagId = :tagId")
    void removeTagFromExpense(long expenseId, long tagId);

    @Query("DELETE FROM expense_tags WHERE expenseId = :expenseId")
    void removeAllTagsFromExpense(long expenseId);

    /**
     * Get all tags for a specific expense.
     */
    @Query("SELECT t.* FROM tags t " +
            "INNER JOIN expense_tags et ON t.id = et.tagId " +
            "WHERE et.expenseId = :expenseId " +
            "ORDER BY t.name ASC")
    LiveData<List<Tag>> getTagsForExpense(long expenseId);

    @Query("SELECT t.* FROM tags t " +
            "INNER JOIN expense_tags et ON t.id = et.tagId " +
            "WHERE et.expenseId = :expenseId " +
            "ORDER BY t.name ASC")
    List<Tag> getTagsForExpenseSync(long expenseId);

    /**
     * Get expenses that have a specific tag.
     */
    @Query("SELECT expenseId FROM expense_tags WHERE tagId = :tagId")
    LiveData<List<Long>> getExpenseIdsForTag(long tagId);
}
