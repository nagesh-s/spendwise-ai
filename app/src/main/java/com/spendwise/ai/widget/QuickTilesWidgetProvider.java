package com.spendwise.ai.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.widget.RemoteViews;

import com.spendwise.ai.MainActivity;
import com.spendwise.ai.R;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.QuickTile;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.LocalDate;

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
                context, appWidgetId, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.grid_quick_tiles, clickPendingIntent);

        // Set up the add button to open tile editor directly
        Intent addIntent = new Intent(context, MainActivity.class);
        addIntent.putExtra("navigate_to", "quick_tiles_add");
        addIntent.putExtra("open_quick_tile_editor", true);
        addIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent addPendingIntent = PendingIntent.getActivity(
                context, appWidgetId + 10_000, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_add_tile, addPendingIntent);

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(new int[] { appWidgetId }, R.id.grid_quick_tiles);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_TILE_CLICKED.equals(intent.getAction())) {
            long tileId = intent.getLongExtra(EXTRA_TILE_ID, -1);
            if (tileId != -1) {
                BroadcastReceiver.PendingResult pendingResult = goAsync();
                addExpenseFromWidgetTile(context, tileId, pendingResult);
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
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, QuickTilesWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.grid_quick_tiles);
    }

    private void addExpenseFromWidgetTile(
            Context context,
            long tileId,
            BroadcastReceiver.PendingResult pendingResult) {
        Context appContext = context.getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(appContext);
                QuickTile tile = db.quickTileDao().getById(tileId);
                if (tile == null) {
                    return;
                }

                Expense expense = new Expense(
                        LocalDate.now().toEpochDay(),
                        tile.getAmountCents(),
                        tile.getLabel(),
                        tile.getCategoryId(),
                        tile.getMerchantId());
                db.expenseDao().insert(expense);

                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(
                        appContext,
                        appContext.getString(
                                R.string.expense_added_from_tile,
                                tile.getLabel(),
                                DateUtils.formatAmountCents(tile.getAmountCents())),
                        Toast.LENGTH_SHORT).show());
            } finally {
                pendingResult.finish();
            }
        });
    }
}
