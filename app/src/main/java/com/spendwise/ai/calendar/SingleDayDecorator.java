package com.spendwise.ai.calendar;

import android.graphics.Color;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

/**
 * Decorator that adds a colored dot to a specific day.
 * Used to indicate spending levels for each day.
 */
public class SingleDayDecorator implements DayViewDecorator {

    private final CalendarDay day;
    private final int color;

    // Spending tier colors
    public static final int COLOR_LOW = Color.parseColor("#4CAF50"); // Green - low spending
    public static final int COLOR_MEDIUM = Color.parseColor("#FF9800"); // Orange - medium spending
    public static final int COLOR_HIGH = Color.parseColor("#F44336"); // Red - high spending

    /**
     * @param day   The calendar day to decorate
     * @param color The dot color (use COLOR_LOW, COLOR_MEDIUM, COLOR_HIGH)
     */
    public SingleDayDecorator(CalendarDay day, int color) {
        this.day = day;
        this.color = color;
    }

    /**
     * Create decorator based on spending amount.
     * 
     * @param day           The calendar day
     * @param amountCents   Spending amount in cents
     * @param lowThreshold  Threshold for low spending (green if below)
     * @param highThreshold Threshold for high spending (red if above)
     */
    public static SingleDayDecorator forAmount(CalendarDay day, long amountCents,
            long lowThreshold, long highThreshold) {
        int color;
        if (amountCents >= highThreshold) {
            color = COLOR_HIGH;
        } else if (amountCents >= lowThreshold) {
            color = COLOR_MEDIUM;
        } else {
            color = COLOR_LOW;
        }
        return new SingleDayDecorator(day, color);
    }

    @Override
    public boolean shouldDecorate(CalendarDay calendarDay) {
        return day.equals(calendarDay);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new DotSpan(8, color));
    }
}
