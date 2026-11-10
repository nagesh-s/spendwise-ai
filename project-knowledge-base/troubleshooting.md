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
| `attribute itemActiveIndicatorColor not found` | Material component version lacks this attribute | Use `app:itemBackground` state selector instead of active indicator attr |

---

## 📊 Data Issues

### Database Upgrade Fails

| Symptom | Check | Action |
|---|---|---|
| App cannot open after upgrading an old database | `AppDatabase` registered migrations | No 9→10 migration exists. Design and test a migration/backfill or an explicit upgrade policy before shipping to existing users; do not enable destructive release migration to hide the issue |
| Debug reinstall works but production upgrade is unverified | Debug destructive fallback and migration tests | Fresh debug installs are not upgrade proof; current migration instrumentation tests only 3→4 |
| Holdings missing after JSON import | Partial backup format | JSON format 3 does not restore the full ledger, evidence links, review/correction state, or preferences |

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

### Quick Tiles Not Adding Expenses

| Symptom | Check | File |
|---------|-------|------|
| Tile tap only opens edit flow | Tile click handler target | `QuickTilesFragment.onTileClick()` |
| Widget tile tap opens app instead of silent add | Widget provider click path still routes to activity | `QuickTilesWidgetProvider.onReceive()` + `addExpenseFromWidgetTile(...)` |
| Widget tile tap does nothing | PendingIntent template/fill-in mismatch | `QuickTilesWidgetProvider.updateWidget()` + `QuickTileRemoteViewsFactory.getViewAt()` |
| Widget list flickers on refresh | Full widget update broadcast used instead of dataset notify | `QuickTilesWidgetProvider.requestUpdate()` |

### SMS Sync/Import Issues

| Symptom | Check | File |
|---------|-------|------|
| Manual sync keeps re-reading old messages | last sync timestamp not persisted/shared | `SmsSyncState` + `AutoTransactionsFragment.scanSms()` |
| Auto import duplicates old scans | worker not using shared sync state | `SmsAutoImportWorker.doWork()` |
| Manual and auto import disagree on time window | helper method mismatch (`readRecentTransactions` vs `readTransactionsSince`) | `SmsPermissionHelper` callers |

### Budget Progress Wrong

| Symptom | Check | File |
|---------|-------|------|
| Always 0% | No expenses in category | Add expenses |
| Wrong percentage | Query issue | `BudgetDao.getBudgetsWithSpend()` |
| Budget not saving | Dialog callback | `BudgetEditorDialog` |

---

## 🎙️ Voice Input Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| Repeated `"No match"` while speaking | recognizer final callback failed to produce stable text | Ensure `VoiceInputHelper` partial-result fallback is active (`onPartialResults` + `ERROR_NO_MATCH` fallback path) |
| `Error 13: Unknown error` in logs | language pack unsupported/unavailable for selected recognizer language | Ensure latest `VoiceInputHelper` is deployed (maps language errors), then verify system speech language/download packs and retry |
| `Error 5` (`Could not start voice capture`) appears intermittently | recognizer service state race/busy callback | Verify one-time client-error retry path in `VoiceInputHelper` and avoid rapid repeated mic taps |
| Voice opens dialog but amount is empty | transcript had no parseable numeric token | Keep transcript prefilled and manually confirm amount in editor |
| Wrong category chosen from voice | heuristic keyword collision | Verify available category names and parser keyword rules in `VoiceExpenseParser` |
| On-device fallback never improves parse | transcript lacks strong amount/category cues | Verify `OnDeviceSemanticExpenseExtractor` confidence thresholds and keyword mappings |
| `offlineLlm` flavor does not use Gemma path | separately supplied model asset missing, copy failed, or invalid file | Obtain a compatible licensed Gemma model, place it at `app/src/offlineLlm/assets/models/spendwise_gemma3_1b.task` before building, and verify app can copy it to `files/models/spendwise_gemma3_1b.task`; model weights are not in this public repository |
| `offlineLlm` build fails resolving MediaPipe | dependency sync/cache issue | Re-sync Gradle and verify `com.google.mediapipe:tasks-genai` resolves from `google()` |
| Gemma output ignored | model returned non-JSON or low-confidence fields | Inspect parser logs and JSON format in `MediaPipeGemmaExpenseExtractor.parseResponse(...)` |
| Busy/microphone errors | recognizer service or permissions unavailable | Re-grant mic permission and retry after service cooldown |

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
| Dark UI looks red/harsh | `values-night/colors.xml` | Rebalance secondary/container colors to brand teal range |

### Security Toggle Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| Biometric switch appears disabled | Enrollment missing or no capability | Use enrollment flow (`Settings.ACTION_BIOMETRIC_ENROLL`) and capability checks |
| Biometric toggle cannot be turned on | `isBiometricAvailable()` too strict for unenrolled devices | Use `hasBiometricCapability()` for UI enablement and gate actual enable on availability |

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
2026-02-16 - added widget background action + SMS incremental sync troubleshooting notes
