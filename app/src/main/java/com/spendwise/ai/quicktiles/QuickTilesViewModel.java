package com.spendwise.ai.quicktiles;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.QuickTileDao;
import com.spendwise.ai.data.entity.QuickTile;
import com.spendwise.ai.widget.QuickTilesWidgetProvider;

import java.util.List;

/**
 * ViewModel for QuickTiles management.
 */
public class QuickTilesViewModel extends AndroidViewModel {

    private final QuickTileDao quickTileDao;
    private final LiveData<List<QuickTile>> allQuickTiles;

    public QuickTilesViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        quickTileDao = db.quickTileDao();
        allQuickTiles = quickTileDao.getAllQuickTiles();
    }

    public LiveData<List<QuickTile>> getAllQuickTiles() {
        return allQuickTiles;
    }

    public void insertTile(QuickTile tile) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            quickTileDao.insert(tile);
            QuickTilesWidgetProvider.requestUpdate(getApplication());
        });
    }

    public void updateTile(QuickTile tile) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            quickTileDao.update(tile);
            QuickTilesWidgetProvider.requestUpdate(getApplication());
        });
    }

    public void deleteTile(QuickTile tile) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            quickTileDao.delete(tile);
            QuickTilesWidgetProvider.requestUpdate(getApplication());
        });
    }

    public QuickTile getTileById(long id) {
        return quickTileDao.getById(id);
    }
}
