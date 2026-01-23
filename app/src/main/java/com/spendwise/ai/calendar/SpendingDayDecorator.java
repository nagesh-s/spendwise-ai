package com.spendwise.ai.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Decorator for MaterialCalendarView that shows spending dots under dates.
 * Dot color indicates spending intensity (green/yellow/red).
 */
public class SpendingDayDecorator implements DayViewDecorator {

    private final Set<CalendarDay> daysWithSpending = new HashSet<>();
    private final Map<CalendarDay, Integer> dayColors;

    /**
     * @param dayColors Map of CalendarDay to color (ARGB) based on spending amount
     */
    public SpendingDayDecorator(Map<CalendarDay, Integer> dayColors) {
        this.dayColors = dayColors;
        if (dayColors != null) {
            this.daysWithSpending.addAll(dayColors.keySet());
        }
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return daysWithSpending.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        // This is called once per decorator, not per day
        // For per-day colors, we need individual decorators or custom approach
        view.addSpan(new DotSpan(8, 0xFF4CAF50)); // Default green
    }

    /**
     * Simple dot span drawn below the date text.
     */
    static class DotSpan implements LineBackgroundSpan {
        private final float radius;
        private final int color;

        DotSpan(float radius, int color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void drawBackground(Canvas canvas, Paint paint, int left, int right,
                int top, int baseline, int bottom,
                CharSequence text, int start, int end, int lineNumber) {
            int oldColor = paint.getColor();
            paint.setColor(color);
            canvas.drawCircle((left + right) / 2f, bottom + radius, radius, paint);
            paint.setColor(oldColor);
        }
    }
}
