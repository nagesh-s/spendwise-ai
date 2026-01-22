package com.spendwise.ai;

import android.app.Application;
import com.jakewharton.threetenabp.AndroidThreeTen;

public class SpendWiseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
    }
}
