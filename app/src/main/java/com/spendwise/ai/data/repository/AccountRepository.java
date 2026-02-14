package com.spendwise.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.AccountDao;
import com.spendwise.ai.data.entity.Account;

import java.util.List;

/**
 * Repository for Account operations.
 */
public class AccountRepository {

    private final AccountDao accountDao;

    public AccountRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        accountDao = db.accountDao();
    }

    public void insert(Account account) {
        AppDatabase.databaseWriteExecutor.execute(() -> accountDao.insert(account));
    }

    public void update(Account account) {
        AppDatabase.databaseWriteExecutor.execute(() -> accountDao.update(account));
    }

    public void delete(Account account) {
        AppDatabase.databaseWriteExecutor.execute(() -> accountDao.delete(account));
    }

    public LiveData<List<Account>> getAllAccounts() {
        return accountDao.getAllAccounts();
    }

    public List<Account> getAllAccountsSync() {
        return accountDao.getAllAccountsSync();
    }

    public Account getDefaultAccount() {
        return accountDao.getDefaultAccount();
    }

    public void adjustBalance(long accountId, long amountCents) {
        AppDatabase.databaseWriteExecutor.execute(() -> accountDao.adjustBalance(accountId, amountCents));
    }
}
