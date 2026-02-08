package com.spendwise.ai.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.spendwise.ai.R;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.QuickTile;
import com.spendwise.ai.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory that creates RemoteViews for each QuickTile item in the widget grid.
 * 
 * Loads tile data from the database and formats them for display.
 */
public class QuickTileRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context context;
    private List<QuickTile> tiles = new ArrayList<>();

    public QuickTileRemoteViewsFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        // Initial data load happens in onDataSetChanged
    }

    @Override
    public void onDataSetChanged() {
        // Load tiles from database synchronously (this runs in a binder thread)
        AppDatabase db = AppDatabase.getInstance(context);
        tiles = db.quickTileDao().getAllQuickTilesSync();
    }

    @Override
    public void onDestroy() {
        tiles.clear();
    }

    @Override
    public int getCount() {
        return tiles.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position < 0 || position >= tiles.size()) {
            return null;
        }

        QuickTile tile = tiles.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quicktile_item);

        // Set tile label
        views.setTextViewText(R.id.text_tile_label, tile.getLabel());

        // Set formatted amount
        String formattedAmount = DateUtils.formatAmountCents(tile.getAmountCents());
        views.setTextViewText(R.id.text_tile_amount, formattedAmount);

        // Set up fill-in intent for click handling
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(QuickTilesWidgetProvider.EXTRA_TILE_ID, tile.getId());
        views.setOnClickFillInIntent(R.id.text_tile_label, fillInIntent);

        // Set click on the entire item
        views.setOnClickFillInIntent(android.R.id.background, fillInIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        // Return null to use the default loading view
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= tiles.size()) {
            return -1;
        }
        return tiles.get(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
