# Settings Module

> Purpose: explain preferences, feature entry points, backup/restore, and report export.

---

## Status: Implemented + Expanded (v1.6.0+)

## What This Screen Does

- Appearance:
  - dark mode toggle
- Feature entry points:
  - Quick Tiles
  - Recurring Expenses
  - Budgets & Categories
  - Goals
  - Analytics
  - Auto Transactions (SMS)
  - Auto Transactions background sync toggle
  - Biometric lock toggle/setup
- Data actions:
  - JSON backup/export
  - JSON import/restore
  - CSV report export
  - PDF report export
- About:
  - dynamic app version display

## Data/Export Flow

1. Fragment launches SAF create/open intents.
2. `SettingsViewModel` loads sync lists (expenses/categories/accounts/merchants) on background threads.
3. `ReportGenerator` creates CSV/PDF output and writes to selected URI.
4. Export/import status is surfaced to user via LiveData + toasts.

## Files

| File | Purpose |
|---|---|
| `settings/SettingsFragment.java` | UI actions, navigation, SAF launchers |
| `settings/SettingsViewModel.java` | Dark mode + CSV/PDF export orchestration |
| `data/DataBackupManager.java` | JSON import/export |
| `data/export/ReportGenerator.java` | CSV + PDF report generation |
| `security/BiometricHelper.java` | biometric capability + lock preference |
| `res/layout/fragment_settings.xml` | settings layout |

## Common Issues

| Issue | Likely Cause | Check |
|---|---|---|
| CSV/PDF export fails | invalid SAF URI or stream failure | `SettingsViewModel.exportCsv/exportPdf` |
| Biometric toggle resets | biometric not enrolled or unsupported | `setupBiometricToggle()` |
| Feature card not opening | nav ID mismatch | `nav_graph.xml` + card click handlers |
| SMS auto-import not running | auto-import switch disabled | `SmsAutoImportPrefs` + `WorkScheduler.setSmsAutoImportEnabled()` |
| Version display incorrect | package info fallback path | `setupAppVersion()` |

## Last Updated
2026-02-17
