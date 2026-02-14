package com.spendwise.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.MerchantDao;
import com.spendwise.ai.data.entity.Merchant;
import com.spendwise.ai.data.pojo.MerchantAgg;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Repository for Merchant data access.
 * Provides a clean API for UI to interact with merchant data.
 */
public class MerchantRepository {

    private final MerchantDao merchantDao;

    public MerchantRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        merchantDao = db.merchantDao();
    }

    // ==================== Insert/Update/Delete ====================

    public void insert(Merchant merchant) {
        AppDatabase.databaseWriteExecutor.execute(() -> merchantDao.insert(merchant));
    }

    /**
     * Insert merchant if it doesn't exist, return the ID.
     * Used when creating expenses with new merchants.
     */
    public long insertOrGetId(String merchantName) {
        Future<Long> future = AppDatabase.databaseWriteExecutor.submit(() -> {
            Merchant existing = merchantDao.findByName(merchantName);
            if (existing != null) {
                return existing.getId();
            }
            Merchant newMerchant = new Merchant(merchantName);
            return merchantDao.insert(newMerchant);
        });
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            return -1;
        }
    }

    public void update(Merchant merchant) {
        AppDatabase.databaseWriteExecutor.execute(() -> merchantDao.update(merchant));
    }

    public void delete(Merchant merchant) {
        AppDatabase.databaseWriteExecutor.execute(() -> merchantDao.delete(merchant));
    }

    // ==================== Queries ====================

    public LiveData<List<Merchant>> getAllMerchants() {
        return merchantDao.getAllMerchants();
    }

    public List<Merchant> getAllMerchantsSync() {
        return merchantDao.getAllMerchantsSync();
    }

    /**
     * Search merchants by name for autocomplete.
     */
    public List<Merchant> searchByName(String query, int limit) {
        return merchantDao.searchByName(query, limit);
    }

    public Merchant findByName(String name) {
        return merchantDao.findByName(name);
    }

    // ==================== Analytics ====================

    public LiveData<List<MerchantAgg>> getTopMerchants(long startDay, long endDay, int limit) {
        return merchantDao.getTopMerchants(startDay, endDay, limit);
    }
}
