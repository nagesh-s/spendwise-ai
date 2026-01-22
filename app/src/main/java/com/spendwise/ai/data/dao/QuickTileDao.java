package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.QuickTile;

import java.util.List;

/**
 * Data Access Object for QuickTile entity.
 * Provides CRUD operations for quick-add expense tiles.
 */
@Dao
public interface QuickTileDao {

    // ==================== CRUD Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(QuickTile quickTile);

    @Update
    void update(QuickTile quickTile);

    @Delete
    void delete(QuickTile quickTile);

    @Query("DELETE FROM quick_tiles WHERE id = :tileId")
    void deleteById(long tileId);

    // ==================== Query Operations ====================

    @Query("SELECT * FROM quick_tiles WHERE id = :id")
    QuickTile getById(long id);

    @Query("SELECT * FROM quick_tiles ORDER BY label ASC")
    LiveData<List<QuickTile>> getAllQuickTiles();

    @Query("SELECT * FROM quick_tiles ORDER BY label ASC")
    List<QuickTile> getAllQuickTilesSync();

    /**
     * Get quick tiles for a specific category.
     */
    @Query("SELECT * FROM quick_tiles WHERE categoryId = :categoryId ORDER BY label ASC")
    LiveData<List<QuickTile>> getQuickTilesByCategory(long categoryId);

    /**
     * Get count of all quick tiles.
     */
    @Query("SELECT COUNT(*) FROM quick_tiles")
    int getQuickTileCount();
}
