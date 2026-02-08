# Settings Module

> **Purpose**: Explains how settings, feature entry points, and data backup work.

---

## 📌 Status: ✅ IMPLEMENTED + EXPANDED (v1.1.x)

---

## 🎯 What This Screen Does

Settings provides:
- **Dark mode toggle**
- **Feature cards**:
  - Quick Tiles management
  - Recurring expenses management
  - Biometric lock toggle
- **Data controls**:
  - Export data to JSON
  - Import data from JSON
- App version display

---

## 🔄 How It Works

### Dark Mode
1. Toggle updates `SettingsViewModel`.
2. Value persists in SharedPreferences.
3. App theme updates via `AppCompatDelegate`.

### Feature Navigation
- `card_quick_tiles` navigates to `navigation_quick_tiles`.
- `card_recurring` navigates to `navigation_recurring`.
- Biometric switch is enabled only when device biometric is available.

### Export / Import
1. Export uses SAF `ACTION_CREATE_DOCUMENT` and writes JSON through `DataBackupManager`.
2. Import uses SAF `ACTION_OPEN_DOCUMENT` and parses JSON through `DataBackupManager`.
3. Success/error toasts are shown on completion.

---

## 📁 Files

| File | Purpose |
|------|---------|
| `settings/SettingsFragment.java` | UI wiring, feature navigation, SAF launchers |
| `settings/SettingsViewModel.java` | Dark mode persistence |
| `data/DataBackupManager.java` | Export/import JSON logic |
| `security/BiometricHelper.java` | Biometric capability + preference handling |
| `fragment_settings.xml` | Settings layout |

---

## 🐛 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Biometric switch disabled | Device has no biometric enrollment/support | Verify device setup and helper checks |
| Import/export does nothing | SAF URI unavailable | Check result callback and URI null-check path |
| Feature card not opening screen | Nav ID mismatch | Check settings click handlers + nav graph IDs |

---

## 📅 Last Updated
2026-02-08 - v1.1.x settings feature routing documented
