# Troubleshooting Guide

> **Purpose**: Quick reference for common issues and solutions. Use this to identify WHERE problems are and HOW to fix them.

---

## 🚨 Crash Issues

### App Crashes on Launch

| Symptom | Cause | File to Check | Solution |
|---------|-------|---------------|----------|
| `InflateException` on MaterialCardView | Theme mismatch | `values/themes.xml` | Change to `Theme.Material3.DayNight.NoActionBar` |
| `Failed to resolve attribute` | Missing Material3 colors | `values/colors.xml` | Add `md_theme_*` color definitions |
| ClassNotFoundException ThreeTen | ThreeTenABP not initialized | `SpendWiseApplication.java` | Add `AndroidThreeTen.init(this)` in onCreate |
| Application not found | Manifest issue | `AndroidManifest.xml` | Add `android:name=".SpendWiseApplication"` |

### App Crashes on Specific Screen

| Screen | File to Check |
|--------|---------------|
| Dashboard | `DashboardFragment.java`, `fragment_dashboard.xml` |
| Calendar | `CalendarFragment.java`, `fragment_calendar.xml` |
| Categories | `CategoriesFragment.java`, `fragment_categories.xml` |
| Merchants | `MerchantsFragment.java`, `fragment_merchants.xml` |
| Settings | `SettingsFragment.java`, `fragment_settings.xml` |
| Expense Editor | `ExpenseEditorDialogFragment.java` |

---

## 🔧 Build Issues

### Gradle build fails

| Error | Cause | Solution |
|-------|-------|----------|
| `Unable to locate a Java Runtime` | Java not in PATH | Build in Android Studio or set JAVA_HOME |
| Cannot resolve JitPack dependency | Missing repository | Add `maven { url 'https://jitpack.io' }` to settings.gradle |
| `attr/mcv_*` not found | Invalid theme attributes | Remove `mcv_*` items from themes.xml |

---

## 📊 Data Issues

### Dashboard Shows Wrong Data

| Symptom | Check | File |
|---------|-------|------|
| Totals are 0 | Are there expenses? | Check `expenses` table |
| Wrong month showing | Month navigation | `DashboardViewModel.currentMonth` |
| Categories missing | Categories exist? | Check `categories` table |

### Expenses Not Saving

| Symptom | Check | File |
|---------|-------|------|
| "Enter valid amount" error | Amount validation | `ExpenseEditorViewModel.validate()` |
| "Select category" error | Category selected? | Check dropdown selection |
| Silent failure | DB write failing | Check Logcat for Room errors |

### Budget Progress Wrong

| Symptom | Check | File |
|---------|-------|------|
| Always 0% | No expenses in category | Add expenses |
| Wrong percentage | Query issue | `BudgetDao.getBudgetsWithSpend()` |
| Budget not saving | Dialog callback | `BudgetEditorDialog` |

---

## 🌙 Dark Mode Issues

### Text Not Visible

| Element | File to Check | What to Look For |
|---------|---------------|------------------|
| Calendar dates | `values-night/themes.xml` | `CalendarDateTextAppearance` style |
| General text | `values-night/colors.xml` | `md_theme_dark_onSurface` color |
| Icons | `drawable/*.xml` | Remove hardcoded `#000000` |

### Colors Wrong

| Issue | File | Solution |
|-------|------|----------|
| Using light colors in dark | `values-night/themes.xml` | Use `md_theme_dark_*` colors |
| Progress bar wrong color | `CategoryBudgetAdapter.java` | Check indicator color logic |

---

## 📁 Import/Export Issues

### Export Fails

| Error | Cause | Check |
|-------|-------|-------|
| "Could not open file" | Permission issue | SAF should handle permissions |
| Crash during export | Sync method missing | Check DAO has `*Sync()` methods |

### Import Fails

| Error | Cause | Solution |
|-------|-------|----------|
| "Backup from newer version" | Version mismatch | Update app |
| JSON parse error | Invalid file | Use valid backup file |
| Nothing imported | All duplicates | Normal - skips existing records |

---

## 🔄 Navigation Issues

### Screen Not Loading

1. Check `nav_graph.xml` for fragment declaration
2. Check fragment ID matches in navigation action
3. Check fragment class exists and compiles

### Bottom Navigation Not Working

1. Check `activity_main.xml` has BottomNavigationView
2. Check `MainActivity.java` sets up NavController
3. Check menu item IDs match nav_graph fragment IDs

---

## 📋 Quick Debug Steps

For ANY issue:
1. Check Logcat for stack trace
2. Identify which file the error points to
3. Use this guide to find related files
4. Check the module guide for that feature

---

## 📅 Last Updated
2026-01-23 - v1.0.0 with comprehensive issues
