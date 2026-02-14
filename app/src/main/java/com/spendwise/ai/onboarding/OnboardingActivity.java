package com.spendwise.ai.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.spendwise.ai.MainActivity;
import com.spendwise.ai.R;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnSkip;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        btnSkip = findViewById(R.id.btn_skip);
        btnNext = findViewById(R.id.btn_next);

        List<OnboardingItem> items = new ArrayList<>();
        items.add(new OnboardingItem(
                "Welcome to SpendWise",
                "Track your expenses effortlessly and gain financial clarity.",
                R.drawable.ic_launcher_foreground // Placeholder
        ));
        items.add(new OnboardingItem(
                "AI-Powered Insights",
                "Get smart insights on your spending habits and catch anomalies early.",
                R.drawable.ic_launcher_foreground // Placeholder
        ));
        items.add(new OnboardingItem(
                "Voice Input",
                "Simply speak to add expenses. 'Spent 200 on lunch at Cafe'.",
                R.drawable.ic_launcher_foreground // Placeholder
        ));

        OnboardingAdapter adapter = new OnboardingAdapter(items);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == items.size() - 1) {
                    btnNext.setText("Start");
                    btnSkip.setVisibility(View.GONE);
                } else {
                    btnNext.setText("Next");
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());
        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < items.size() - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                finishOnboarding();
            }
        });
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences("spendwise_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("first_run", false).apply();

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
