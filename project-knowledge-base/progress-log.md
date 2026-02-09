# Progress Log

> **Purpose**: Track development history and provide context for resuming work.

---

## 🎯 Current State: ✅ ENHANCED (v1.1.2)

| Metric | Value |
|--------|-------|
| **Version** | v1.1.2 |
| **Status** | Feature-complete baseline + UX refinement + usability bug-fix pass |
| **Last Updated** | 2026-02-08 |

---

## ✅ Latest Completed Work (v1.1.2)

### Usability & Reliability Fixes
- ✅ Quick Tiles now add expenses on tap (long-press opens edit/delete actions).
- ✅ Biometric lock toggle now handles non-enrolled devices by guiding users to system security settings.
- ✅ Added biometric manifest permissions for better device compatibility.
- ✅ Add Expense modal date field clipping fixed via compact date format and stacked/scrollable form layout.
- ✅ Bottom navigation active-state styling switched to backward-compatible resources to avoid resource-linking failures.

### UI/UX Polish
- ✅ Dark mode palette rebalanced to remove aggressive red secondary tones.
- ✅ Unified card treatment across key lists (surface container + outline).
- ✅ Category budget action buttons changed to outlined style for clearer visual hierarchy.
- ✅ Dashboard pie chart label overlap reduced by moving detail emphasis to legend.

### Reliability Fixes
- ✅ Expense edit mode now pre-fills amount, date, category, description, and merchant.
- ✅ Budget progress mapping now uses `categoryId` instead of category-name matching.
- ✅ Smart category suggestion outputs aligned with seeded default categories.

### UX & Feature Enhancements
- ✅ Dashboard budget health card (used/limit, remaining/overrun, over-budget count).
- ✅ Dashboard spending pace cards (average per day + projected month total).
- ✅ Calendar one-tap **Today** jump action.
- ✅ Categories month summary card + **Copy Previous Month Budgets** action.
- ✅ Merchants summary card (total merchant spend + top merchant).
- ✅ Improved bottom navigation selected/unselected visual states.
- ✅ Clearer headers/subtitles on Quick Tiles and Recurring screens.

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

---

## 🔁 How to Resume Work

1. Read `architecture.md` for the updated system overview.
2. Open the relevant module guide in `modules/`.
3. Follow existing patterns: Fragment → ViewModel → Repository → DAO.
4. Use `troubleshooting.md` if an issue is symptom-driven.

---

## 📅 Last Updated
2026-02-08 - v1.1.2 usability and UI polish stabilization pass
