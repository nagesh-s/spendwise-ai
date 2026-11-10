---
description: Mandatory checklist to run after completing any feature or phase
---

# Post-Feature Completion Checklist

**Run this checklist every time you complete a feature, sub-feature, or phase.**

## 1. UI Accessibility Check (CRITICAL)
- [ ] **Can the user REACH this feature from the app?** Verify there is a visible button, card, menu item, FAB, or navigation link that triggers the new functionality.
- [ ] If a new Fragment/screen was added:
  - [ ] Is it in `nav_graph.xml`?
  - [ ] Is there a clickable card/button in Settings, Dashboard, or another existing screen that navigates to it?
  - [ ] Does the click handler exist in the parent Fragment/Activity?
- [ ] If new functionality was added to an existing screen:
  - [ ] Is there a visible UI element (button, FAB, toggle, menu item) that triggers it?
  - [ ] Is the UI element wired to the functional code?
- [ ] If seed/default data is needed:
  - [ ] Does it work on **fresh installs** (not just migrations)?
  - [ ] Is seeding in the `prepopulateCallback.onOpen()` or equivalent?

## 2. Documentation Updates
- [ ] `CHANGELOG.md` — Add an entry only for a versioned release or user-visible milestone
- [ ] `project-knowledge-base/progress-log.md` — Update current state and latest work
- [ ] `project-knowledge-base/architecture.md` — Add new packages/components if applicable
- [ ] `memory-bank/data-model.md` — Update if entities changed
- [ ] `memory-bank/navigation-map.md` — Update if navigation destinations changed
- [ ] `memory-bank/room-migrations.md` — Update if DB version changed

## 3. Build Verification
- [ ] Use JDK 21 for Gradle; application Java source/target compatibility remains Java 11.
- [ ] Add or update focused tests for behavior changes, using synthetic data.
- [ ] Follow [CONTRIBUTING.md](../../CONTRIBUTING.md) for app builds, JVM tests, Android-test APK assembly, and both lint flavors.
- [ ] Run affected connected-device instrumentation tests or explicitly report that they were not run.

## 4. Handoff
- [ ] Leave changes unstaged and uncommitted for review.
- [ ] Report completed work, checks run, results, and remaining limitations.
- [ ] Never stage, commit, tag, or push without explicit authorization for that action.

## RULE: Never consider a feature "done" until a user can trigger it from the running app.
