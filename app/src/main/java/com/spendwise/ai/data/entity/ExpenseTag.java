package com.spendwise.ai.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

/**
 * Join table linking expenses to tags (many-to-many relationship).
 * An expense can have multiple tags, and a tag can be applied to multiple
 * expenses.
 */
@Entity(tableName = "expense_tags", primaryKeys = { "expenseId", "tagId" }, foreignKeys = {
        @ForeignKey(entity = Expense.class, parentColumns = "id", childColumns = "expenseId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Tag.class, parentColumns = "id", childColumns = "tagId", onDelete = ForeignKey.CASCADE)
}, indices = {
        @Index(value = "expenseId"),
        @Index(value = "tagId")
})
public class ExpenseTag {

    private long expenseId;
    private long tagId;

    public ExpenseTag(long expenseId, long tagId) {
        this.expenseId = expenseId;
        this.tagId = tagId;
    }

    public long getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(long expenseId) {
        this.expenseId = expenseId;
    }

    public long getTagId() {
        return tagId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }
}
