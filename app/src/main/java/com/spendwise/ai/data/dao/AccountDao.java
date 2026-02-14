package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.Account;

import java.util.List;

/**
 * Data Access Object for Account entity.
 */
@Dao
public interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Account account);

    @Update
    void update(Account account);

    @Delete
    void delete(Account account);

    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, name ASC")
    LiveData<List<Account>> getAllAccounts();

    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, name ASC")
    List<Account> getAllAccountsSync();

    @Query("SELECT * FROM accounts WHERE id = :id")
    Account getById(long id);

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    Account getDefaultAccount();

    @Query("SELECT COUNT(*) FROM accounts")
    int getAccountCount();

    /**
     * Update account balance by adding/subtracting an amount.
     */
    @Query("UPDATE accounts SET balanceCents = balanceCents + :amountCents WHERE id = :accountId")
    void adjustBalance(long accountId, long amountCents);
}
