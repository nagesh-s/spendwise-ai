package com.spendwise.ai;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.spendwise.ai.security.BiometricHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MainActivityThemeSmokeTest {

    @Before
    public void prepareLaunchState() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("spendwise_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("first_run", false)
                .apply();
        BiometricHelper.setBiometricEnabled(context, false);
    }

    @Test
    public void mainActivity_materialThemeResolvesBackgroundAndSurface() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                TypedValue background = new TypedValue();
                try (TypedArray attributes = activity.getTheme().obtainStyledAttributes(
                        new int[]{android.R.attr.background})) {
                    assertNotNull(attributes.getDrawable(0));
                }

                TypedValue surface = new TypedValue();
                assertTrue(activity.getTheme().resolveAttribute(
                        com.google.android.material.R.attr.colorSurface, surface, true));
            });
        }
    }
}
