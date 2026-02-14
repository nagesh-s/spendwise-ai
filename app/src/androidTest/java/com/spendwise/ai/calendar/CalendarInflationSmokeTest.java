package com.spendwise.ai.calendar;

import android.content.Context;

import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.spendwise.ai.MainActivity;
import com.spendwise.ai.R;
import com.spendwise.ai.security.BiometricHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CalendarInflationSmokeTest {

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
    public void navigateToCalendar_inflatesMaterialCalendarView() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                NavHostFragment navHostFragment = (NavHostFragment) activity
                        .getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHostFragment);
                navHostFragment.getNavController().navigate(R.id.navigation_calendar);
            });

            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            scenario.onActivity(activity -> {
                NavHostFragment navHostFragment = (NavHostFragment) activity
                        .getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHostFragment);

                androidx.fragment.app.Fragment current = navHostFragment
                        .getChildFragmentManager()
                        .getPrimaryNavigationFragment();
                assertNotNull(current);
                assertTrue(current instanceof CalendarFragment);
                assertNotNull(current.requireView().findViewById(R.id.calendar_view));
            });
        }
    }
}
