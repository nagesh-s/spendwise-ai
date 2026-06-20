package com.spendwise.ai.data;

import android.content.Context;
import android.net.Uri;

import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.entity.Budget;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.data.entity.Merchant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles JSON export and import of all app data.
 * Uses Storage Access Framework for file access.
 */
public class DataBackupManager {

    private final Context context;
    private final AppDatabase database;
    private final ExecutorService executor;

    // JSON keys
    private static final String KEY_VERSION = "backupVersion";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_EXPENSES = "expenses";
    private static final String KEY_CATEGORIES = "categories";
    private static final String KEY_MERCHANTS = "merchants";
    private static final String KEY_BUDGETS = "budgets";
    private static final String KEY_BUCKETS = "buckets";
    private static final String KEY_INVESTMENTS = "investments";

    private static final int BACKUP_VERSION = 2;

    public interface BackupCallback {
        void onSuccess(String message);

        void onError(String error);
    }

    public DataBackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Export all data to JSON file at the given URI.
     */
    public void exportData(Uri destinationUri, BackupCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject backup = new JSONObject();
                backup.put(KEY_VERSION, BACKUP_VERSION);
                backup.put(KEY_TIMESTAMP, System.currentTimeMillis());

                // Export categories
                List<Category> categories = database.categoryDao().getAllCategoriesSync();
                JSONArray categoriesArray = new JSONArray();
                for (Category category : categories) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", category.getId());
                    obj.put("name", category.getName());
                    obj.put("colorArgb", category.getColorArgb());
                    obj.put("iconKey", category.getIconKey());
                    obj.put("isDefault", category.isDefault());
                    categoriesArray.put(obj);
                }
                backup.put(KEY_CATEGORIES, categoriesArray);

                // Export merchants
                List<Merchant> merchants = database.merchantDao().getAllMerchantsSync();
                JSONArray merchantsArray = new JSONArray();
                for (Merchant merchant : merchants) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", merchant.getId());
                    obj.put("name", merchant.getName());
                    merchantsArray.put(obj);
                }
                backup.put(KEY_MERCHANTS, merchantsArray);

                // Export expenses
                List<Expense> expenses = database.expenseDao().getAllExpensesSync();
                JSONArray expensesArray = new JSONArray();
                for (Expense expense : expenses) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", expense.getId());
                    obj.put("dateEpochDay", expense.getDateEpochDay());
                    obj.put("amountCents", expense.getAmountCents());
                    obj.put("description", expense.getDescription());
                    obj.put("categoryId", expense.getCategoryId());
                    if (expense.getMerchantId() != null) {
                        obj.put("merchantId", expense.getMerchantId());
                    }
                    expensesArray.put(obj);
                }
                backup.put(KEY_EXPENSES, expensesArray);

                // Export budgets
                List<Budget> budgets = database.budgetDao().getAllBudgetsSync();
                JSONArray budgetsArray = new JSONArray();
                for (Budget budget : budgets) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", budget.getId());
                    obj.put("monthKeyYYYYMM", budget.getMonthKeyYYYYMM());
                    obj.put("categoryId", budget.getCategoryId());
                    obj.put("limitCents", budget.getLimitCents());
                    budgetsArray.put(obj);
                }
                backup.put(KEY_BUDGETS, budgetsArray);

                // Export buckets
                List<Bucket> bucketsList = database.bucketDao().getAllBucketsSync();
                JSONArray bucketsArray = new JSONArray();
                for (Bucket bucket : bucketsList) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", bucket.getId());
                    obj.put("name", bucket.getName());
                    obj.put("type", bucket.getType());
                    obj.put("targetPercent", bucket.getTargetPercent());
                    obj.put("currentBalanceCents", bucket.getCurrentBalanceCents());
                    obj.put("colorArgb", bucket.getColorArgb());
                    obj.put("iconKey", bucket.getIconKey());
                    obj.put("sortOrder", bucket.getSortOrder());
                    obj.put("isDefault", bucket.isDefault());
                    obj.put("description", bucket.getDescription());
                    obj.put("timelineLabel", bucket.getTimelineLabel());
                    obj.put("investmentAdvice", bucket.getInvestmentAdvice());
                    bucketsArray.put(obj);
                }
                backup.put(KEY_BUCKETS, bucketsArray);

                // Export investments
                List<Holding> investmentsList = database.investmentDao().getAllInvestmentsSync();
                JSONArray investmentsArray = new JSONArray();
                for (Holding inv : investmentsList) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", inv.getId());
                    obj.put("bucketId", inv.getBucketId());
                    obj.put("name", inv.getName());
                    obj.put("jobTitle", inv.getJobTitle());
                    obj.put("platform", inv.getPlatform());
                    obj.put("assetType", inv.getAssetType());
                    obj.put("investedAmountCents", inv.getInvestedAmountCents());
                    obj.put("currentValueCents", inv.getCurrentValueCents());
                    obj.put("lastUpdatedEpochDay", inv.getLastUpdatedEpochDay());
                    obj.put("notes", inv.getNotes());
                    obj.put("isSip", inv.isSip());
                    investmentsArray.put(obj);
                }
                backup.put(KEY_INVESTMENTS, investmentsArray);

                // Write to file
                try (OutputStream outputStream = context.getContentResolver().openOutputStream(destinationUri)) {
                    if (outputStream != null) {
                        String jsonString = backup.toString(2);
                        outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();

                        int totalItems = categories.size() + merchants.size() + expenses.size()
                                + budgets.size() + bucketsList.size() + investmentsList.size();
                        callback.onSuccess("Exported " + totalItems + " items successfully!");
                    } else {
                        callback.onError("Could not open file for writing");
                    }
                }

            } catch (JSONException | IOException e) {
                callback.onError("Export failed: " + e.getMessage());
            }
        });
    }

    /**
     * Import data from JSON file at the given URI.
     * Merges with existing data (does not delete existing).
     */
    public void importData(Uri sourceUri, BackupCallback callback) {
        executor.execute(() -> {
            try {
                // Read JSON from file
                StringBuilder stringBuilder = new StringBuilder();
                try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                }

                JSONObject backup = new JSONObject(stringBuilder.toString());

                int version = backup.optInt(KEY_VERSION, 1);
                if (version > BACKUP_VERSION) {
                    callback.onError("Backup file is from a newer version of the app");
                    return;
                }

                int importedCount = 0;

                // Import categories (skip defaults that already exist)
                if (backup.has(KEY_CATEGORIES)) {
                    JSONArray categoriesArray = backup.getJSONArray(KEY_CATEGORIES);
                    for (int i = 0; i < categoriesArray.length(); i++) {
                        JSONObject obj = categoriesArray.getJSONObject(i);
                        String name = obj.getString("name");
                        boolean isDefault = obj.optBoolean("isDefault", false);

                        // Check if category already exists
                        Category existing = database.categoryDao().findByName(name);
                        if (existing == null) {
                            Category category = new Category(
                                    name,
                                    obj.getInt("colorArgb"),
                                    obj.optString("iconKey", null),
                                    isDefault);
                            database.categoryDao().insert(category);
                            importedCount++;
                        }
                    }
                }

                // Import merchants
                if (backup.has(KEY_MERCHANTS)) {
                    JSONArray merchantsArray = backup.getJSONArray(KEY_MERCHANTS);
                    for (int i = 0; i < merchantsArray.length(); i++) {
                        JSONObject obj = merchantsArray.getJSONObject(i);
                        String name = obj.getString("name");

                        Merchant existing = database.merchantDao().findByName(name);
                        if (existing == null) {
                            Merchant merchant = new Merchant(name);
                            database.merchantDao().insert(merchant);
                            importedCount++;
                        }
                    }
                }

                // Import expenses
                if (backup.has(KEY_EXPENSES)) {
                    JSONArray expensesArray = backup.getJSONArray(KEY_EXPENSES);
                    for (int i = 0; i < expensesArray.length(); i++) {
                        JSONObject obj = expensesArray.getJSONObject(i);

                        // Create expense with new IDs (categories/merchants may have different IDs)
                        Expense expense = new Expense(
                                obj.getLong("dateEpochDay"),
                                obj.getLong("amountCents"),
                                obj.optString("description", null),
                                obj.getLong("categoryId"),
                                obj.has("merchantId") ? obj.getLong("merchantId") : null);
                        database.expenseDao().insert(expense);
                        importedCount++;
                    }
                }

                // Import budgets
                if (backup.has(KEY_BUDGETS)) {
                    JSONArray budgetsArray = backup.getJSONArray(KEY_BUDGETS);
                    for (int i = 0; i < budgetsArray.length(); i++) {
                        JSONObject obj = budgetsArray.getJSONObject(i);

                        String monthKey = obj.getString("monthKeyYYYYMM");
                        long categoryId = obj.getLong("categoryId");

                        // Check if budget already exists for this month/category
                        Budget existing = database.budgetDao().getBudgetForCategoryAndMonth(categoryId, monthKey);
                        if (existing == null) {
                            Budget budget = new Budget(
                                    monthKey,
                                    categoryId,
                                    obj.getLong("limitCents"));
                            database.budgetDao().insert(budget);
                            importedCount++;
                        }
                    }
                }

                callback.onSuccess("Imported " + importedCount + " new items!");

            } catch (JSONException | IOException e) {
                callback.onError("Import failed: " + e.getMessage());
            }
        });
    }
}
