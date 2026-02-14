package com.spendwise.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.TagDao;
import com.spendwise.ai.data.entity.ExpenseTag;
import com.spendwise.ai.data.entity.Tag;

import java.util.List;

/**
 * Repository for Tag operations.
 * Mediates between the ViewModel and the TagDao.
 */
public class TagRepository {

    private final TagDao tagDao;

    public TagRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        tagDao = db.tagDao();
    }

    // ==================== Tag CRUD ====================

    public void insert(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> tagDao.insert(tag));
    }

    public long insertSync(Tag tag) {
        return tagDao.insert(tag);
    }

    public void update(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> tagDao.update(tag));
    }

    public void delete(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> tagDao.delete(tag));
    }

    public LiveData<List<Tag>> getAllTags() {
        return tagDao.getAllTags();
    }

    // ==================== ExpenseTag Operations ====================

    public void addTagToExpense(long expenseId, long tagId) {
        AppDatabase.databaseWriteExecutor.execute(() -> tagDao.insertExpenseTag(new ExpenseTag(expenseId, tagId)));
    }

    public void removeTagFromExpense(long expenseId, long tagId) {
        AppDatabase.databaseWriteExecutor.execute(() -> tagDao.removeTagFromExpense(expenseId, tagId));
    }

    public void removeAllTagsFromExpense(long expenseId) {
        AppDatabase.databaseWriteExecutor.execute(() -> tagDao.removeAllTagsFromExpense(expenseId));
    }

    /**
     * Set the tags for an expense (replaces all existing tags).
     */
    public void setTagsForExpense(long expenseId, List<Long> tagIds) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            tagDao.removeAllTagsFromExpense(expenseId);
            for (Long tagId : tagIds) {
                tagDao.insertExpenseTag(new ExpenseTag(expenseId, tagId));
            }
        });
    }

    public LiveData<List<Tag>> getTagsForExpense(long expenseId) {
        return tagDao.getTagsForExpense(expenseId);
    }

    public List<Tag> getTagsForExpenseSync(long expenseId) {
        return tagDao.getTagsForExpenseSync(expenseId);
    }
}
