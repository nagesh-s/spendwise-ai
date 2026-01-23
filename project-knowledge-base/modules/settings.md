# Settings Module

> **Purpose**: Explains how settings and data backup work.

---

## 📌 Status: ✅ IMPLEMENTED (v1.0.0)

---

## 🎯 What This Screen Does

Settings screen provides:
- **Dark mode toggle** - switches app theme
- **Export data** - saves all data to JSON file
- **Import data** - restores data from JSON backup
- **App version** display

---

## 🔄 How It Works

### Dark Mode Toggle
1. User toggles switch
2. `viewModel.setDarkModeEnabled(isChecked)` called
3. ViewModel saves preference to SharedPreferences
4. App follows system night mode with override

### Export Data
1. User taps "Export Data" card
2. `launchExport()` opens SAF file picker via `ACTION_CREATE_DOCUMENT`
3. User picks location, names file (default: spendwise_backup.json)
4. `DataBackupManager.exportData(uri)` called
5. Loads all data from DAOs (using sync methods)
6. Writes JSON to selected file
7. Shows success toast with item count

### Import Data
1. User taps "Import Data" card
2. `launchImport()` opens SAF file picker via `ACTION_OPEN_DOCUMENT`
3. User selects JSON backup file
4. `DataBackupManager.importData(uri)` called
5. Parses JSON, inserts new records (skips duplicates)
6. Shows success toast with imported count

---

## 📁 Files

| File | Purpose |
|------|---------|
| `settings/SettingsFragment.java` | UI, SAF launchers |
| `settings/SettingsViewModel.java` | Dark mode preference |
| `data/DataBackupManager.java` | JSON export/import logic |
| `fragment_settings.xml` | Screen layout |

---

## 📦 Backup JSON Format

```json
{
  "backupVersion": 1,
  "timestamp": 1737654321000,
  "categories": [...],
  "merchants": [...],
  "expenses": [...],
  "budgets": [...]
}
```

Each array contains objects with all entity fields.

---

## 🐛 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Export fails | File write error | Check SAF permissions, try different location |
| Import fails to parse | Wrong JSON format | Ensure file is valid SpendWise backup |
| "Backup from newer version" | Version mismatch | Update app or use older backup |
| Dark mode not saving | SharedPreferences issue | Check SettingsViewModel |
| Duplicates after import | Same data imported twice | Import skips existing, normal behavior |

---

## 📅 Last Updated
2026-01-23 - v1.0.0 with full import/export
