package com.spendwise.ai.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Service that provides the RemoteViewsFactory for the QuickTiles widget grid.
 */
public class QuickTileWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new QuickTileRemoteViewsFactory(getApplicationContext());
    }
}
