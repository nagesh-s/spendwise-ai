# Progress Log

> **Purpose**: Track development history and provide context for resuming work.

---

## 🎯 Current State: ✅ Phase 2 COMPLETE + STABILIZED QA BASELINE (v1.2.2)

| Metric | Value |
|--------|-------|
| **Version** | v1.2.2 |
| **Status** | Phase 2 complete + stabilization + CI/test gate + SMS import hardening |
| **Last Updated** | 2026-02-14 |

---

## ✅ Latest Completed Work (v1.2.2 — CI + QA hardening)

### CI Quality Gate
- ✅ Added GitHub Actions workflow `.github/workflows/android-ci.yml`
- ✅ CI now verifies:
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:assembleDebugAndroidTest`

### Automated Test Expansion
- ✅ `TransactionParserTest` (unit): sender filtering, merchant extraction, debit/credit precedence, invalid sender handling
- ✅ `AppDatabaseMigrationTest` (androidTest): migration `3→4` schema/data assertions
- ✅ `ExpenseEditorViewModelSaveTest` (androidTest): save + edit + delete persistence coverage
- ✅ `CategoryBudgetIntegrityTest` (androidTest): default-category delete protection + budget spend linking accuracy
- ✅ Verification commands confirmed:
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:assembleDebugAndroidTest`
  - `./gradlew :app:connectedDebugAndroidTest`

### SMS Import Hardening
- ✅ Added duplicate check before insert (date + amount + category + description)
- ✅ Import failure path now preserves retryability (no false imported state)
- ✅ Permission UX handles first-time deny, rationale, and permanently denied states with app-settings deep link

### Release Prep
- ✅ Version bumped to `versionCode 122` / `versionName 1.2.2`
- ✅ Added release runbook: `project-knowledge-base/release-smoke-checklist.md`

### Build/Tooling Cleanup
- ✅ Switched compile target from Java 8 to Java 11 (removes obsolete source/target warnings)
- ✅ Removed deprecated Gradle constraint flag usage
- ✅ Fixed debug duplicate-class conflict by excluding legacy support transitive deps from `material-calendarview`

---

## ✅ Stabilization Work (v1.2.1)

### UI/Flow Fixes
- ✅ Categories FAB now opens a working add-category dialog
- ✅ Add-category validation added for empty and duplicate names
- ✅ New categories get deterministic palette color assignment

### Stability Follow-up
- ✅ Expense save operations kept on background executor (main-thread safety)
- ✅ Settings routing for newly added Phase 2 feature screens is wired

---

## ✅ Phase 2 Feature Delivery (v1.2.0)

### Tags & Custom Labels
- ✅ Tag/ExpenseTag entities with many-to-many join table
- ✅ TagDao with CRUD + expense-tag linking queries
- ✅ ChipGroup UI in expense editor with "New Tag" dialog

### Multi-Account Support
- ✅ Account entity (CASH, BANK, CREDIT_CARD, WALLET, OTHER)
- ✅ Account dropdown selector in expense editor
- ✅ Default Cash account seeded during migration

### Financial Goals Tracking
- ✅ FinancialGoal entity (SAVINGS, LIMIT, DEBT types)
- ✅ GoalsFragment with add goal / update progress dialogs
- ✅ Progress bars and status tracking

### Advanced Analytics Dashboard
- ✅ Period selection (week/month/quarter/year)
- ✅ Spending trend LineChart + category PieChart
- ✅ Top-5 categories breakdown

### Auto Transaction Reader (SMS)
- ✅ TransactionParser with regex patterns for Indian banks (SBI, HDFC, ICICI, UPI)
- ✅ SmsPermissionHelper with runtime permission flow
- ✅ AutoTransactionsFragment with scan + one-tap import
- ✅ READ_SMS permission in AndroidManifest

### Technical
- ✅ Database v4 with explicit Migration(3→4)
- ✅ Build verified: `./gradlew assembleDebug` passes

---

## 📦 Version History

| Version | Description | Date |
|---------|-------------|------|
| v0.0.1 | Project scaffold, navigation | 2026-01-22 |
| v0.1.0 | Data layer - entities, DAOs | 2026-01-22 |
| v0.2.0 | Repositories, ViewModels | 2026-01-22 |
| v0.3.0 | Core screens + dialogs | 2026-01-22 |
| v0.3.2 | Theme fixes, budget dialog | 2026-01-23 |
| v0.4.0 | JSON import/export | 2026-01-23 |
| v0.5.0 | Smart suggestions, autocomplete | 2026-01-23 |
| v1.0.0 | Calendar decorators, polish | 2026-01-23 |
| v1.1.0 | QuickTiles widget, recurring expenses, reminders, biometric lock | 2026-02-08 |
| v1.1.1 | UX refinement + dashboard/category/merchant insights + edit-flow fixes | 2026-02-08 |
| v1.1.2 | UX polish + quick tile, biometric, modal-date, nav compatibility fixes | 2026-02-08 |
| v1.2.0 | Phase 2: Tags, multi-account, goals, analytics, SMS reader | 2026-02-14 |
| v1.2.1 | Stabilization: category add flow + settings wiring + cleanup | 2026-02-14 |
| v1.2.2 | CI gate + expanded tests + SMS hardening + release prep + build cleanup | 2026-02-14 |

---

## 🔁 How to Resume Work

1. Read `architecture.md` for the updated system overview.
2. Open the relevant module guide in `modules/`.
3. Follow existing patterns: Fragment → ViewModel → Repository → DAO.
4. Use `troubleshooting.md` if an issue is symptom-driven.
5. **After completing a phase**: Follow `.agent/workflows/post-phase-completion.md`

---

## 📅 Last Updated
2026-02-14 - v1.2.2 CI gate + QA hardening complete
