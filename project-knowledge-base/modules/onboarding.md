# Onboarding Module

> Purpose: explain first-run onboarding behavior and control flags.

---

## Status: Implemented (v1.6.0)

## What It Does

- Shows a 3-step onboarding experience on first app launch:
  - Welcome
  - AI insights value
  - Voice input value
- Supports `Skip`, `Next`, and `Start` actions.
- Sets `first_run=false` and routes user to `MainActivity` after completion.

## Launch Logic

1. `MainActivity` checks SharedPreferences key `first_run`.
2. If true, app starts `OnboardingActivity` and finishes `MainActivity`.
3. On completion, onboarding writes `first_run=false` and opens main app.

## Files

| File | Purpose |
|---|---|
| `onboarding/OnboardingActivity.java` | Pager setup and completion logic |
| `onboarding/OnboardingAdapter.java` | ViewPager2 adapter |
| `onboarding/OnboardingItem.java` | Slide content model |
| `res/layout/activity_onboarding.xml` | Onboarding container layout |
| `res/layout/item_onboarding.xml` | Single slide layout |

## Last Updated
2026-02-14
