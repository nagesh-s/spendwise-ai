package com.spendwise.ai.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.spendwise.ai.MainActivity;
import com.spendwise.ai.R;

/**
 * AppWidgetProvider for the QuickTiles home screen widget.
 * 
 * Displays a grid of QuickTile items that allow one-tap expense creation.
 * Tapping a tile opens the expense editor with prefilled values.
 */
public class QuickTilesWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_TILE_CLICKED = "com.spendwise.ai.ACTION_TILE_CLICKED";
    public static final String EXTRA_TILE_ID = "tile_id";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Create RemoteViews for the widget layout
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quicktiles);

        // Set up the intent for the RemoteViewsService that will provide the grid data
        Intent serviceIntent = new Intent(context, QuickTileWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.grid_quick_tiles, serviceIntent);

        // Set the empty view
        views.setEmptyView(R.id.grid_quick_tiles, R.id.text_empty);

        // Set up the pending intent template for tile clicks
        Intent clickIntent = new Intent(context, QuickTilesWidgetProvider.class);
        clickIntent.setAction(ACTION_TILE_CLICKED);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(
                context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.grid_quick_tiles, clickPendingIntent);

        // Set up the add button to open the app
        Intent addIntent = new Intent(context, MainActivity.class);
        addIntent.putExtra("navigate_to", "quick_tiles_manager");
        addIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent addPendingIntent = PendingIntent.getActivity(
                context, 1, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_add_tile, addPendingIntent);

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.grid_quick_tiles);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_TILE_CLICKED.equals(intent.getAction())) {
            long tileId = intent.getLongExtra(EXTRA_TILE_ID, -1);
            if (tileId != -1) {
                // Open MainActivity with the tile ID to create expense
                Intent launchIntent = new Intent(context, MainActivity.class);
                launchIntent.putExtra("action", "create_expense_from_tile");
                launchIntent.putExtra("tile_id", tileId);
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(launchIntent);
            }
        }
    }

    /**
     * Called when the first widget is added during a session.
     */
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    /**
     * Called when the last widget is removed.
     */
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    /**
     * Request widget update from anywhere in the app.
     */
    public static void requestUpdate(Context context) {
        Intent intent = new Intent(context, QuickTilesWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        context.sendBroadcast(intent);
    }
}
