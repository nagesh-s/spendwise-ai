package com.spendwise.ai.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for date operations used throughout the app.
 * Centralizes date formatting and epoch day calculations.
 */
public final class DateUtils {

    private static final DateTimeFormatter MONTH_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter DISPLAY_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");

    private DateUtils() {
        // Utility class, no instantiation
    }

    /**
     * Get the current month key in YYYYMM format.
     */
    public static String getCurrentMonthKey() {
        return LocalDate.now().format(MONTH_KEY_FORMATTER);
    }

    /**
     * Get month key for a specific date.
     */
    public static String getMonthKey(LocalDate date) {
        return date.format(MONTH_KEY_FORMATTER);
    }

    /**
     * Get month key for a YearMonth.
     */
    public static String getMonthKey(YearMonth yearMonth) {
        return yearMonth.format(MONTH_KEY_FORMATTER);
    }

    /**
     * Parse month key back to YearMonth.
     */
    public static YearMonth parseMonthKey(String monthKey) {
        return YearMonth.parse(monthKey, MONTH_KEY_FORMATTER);
    }

    /**
     * Get display string for a month (e.g., "January 2026").
     */
    public static String getDisplayMonth(YearMonth yearMonth) {
        return yearMonth.format(DISPLAY_MONTH_FORMATTER);
    }

    /**
     * Get display string from month key.
     */
    public static String getDisplayMonth(String monthKey) {
        return getDisplayMonth(parseMonthKey(monthKey));
    }

    /**
     * Get the first day of the month as epoch day.
     */
    public static long getMonthStartEpochDay(YearMonth yearMonth) {
        return yearMonth.atDay(1).toEpochDay();
    }

    /**
     * Get the last day of the month as epoch day.
     */
    public static long getMonthEndEpochDay(YearMonth yearMonth) {
        return yearMonth.atEndOfMonth().toEpochDay();
    }

    /**
     * Get today's epoch day.
     */
    public static long getTodayEpochDay() {
        return LocalDate.now().toEpochDay();
    }

    /**
     * Convert epoch day to LocalDate.
     */
    public static LocalDate epochDayToDate(long epochDay) {
        return LocalDate.ofEpochDay(epochDay);
    }

    /**
     * Format amount in cents to display string.
     */
    public static String formatAmountCents(long amountCents) {
        return String.format("₹%,.2f", amountCents / 100.0);
    }

    /**
     * Parse amount string to cents.
     */
    public static long parseAmountToCents(String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr.replaceAll("[^\\d.]", ""));
            return Math.round(amount * 100);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
