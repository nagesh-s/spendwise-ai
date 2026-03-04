# Buckets & Investments Module

## Purpose
Goal-based investing and salary split management with flexible N-buckets (4 defaults + optional custom buckets), plus holdings tracking and import.

## Main Screens
- `BucketsFragment`
  - Grid-style bucket overview (2 columns on phones, 3 on larger screens)
  - Net-worth visibility toggle with eye icon state
  - Salary allocation entry and split application
- `BucketDetailFragment`
  - Holdings summary (current value, invested, P/L, count)
  - Add/edit investment dialog
  - Voice-assisted investment prefill
  - Import holdings menu

## Data Flow
1. Salary amount is stored/edited via `BucketSalaryPrefs`.
2. Allocation is calculated by `BucketAllocationEngine`.
3. Buckets + totals are observed via `BucketViewModel` and Room-backed DAOs.
4. Investments are listed/edited via `BucketDetailViewModel`.

## Holdings Import Paths
- SMS path:
  - `SmsPermissionHelper` reads SMS.
  - `InvestmentSmsParser` converts messages to `ParsedInvestmentSignal`.
  - `InvestmentAutoIngestionEngine` inserts/updates while preventing duplicates.
- Notification path:
  - `InvestmentNotificationListenerService` receives broker/email app notifications.
  - Notification text is parsed through `InvestmentSmsParser`.
  - `InvestmentAutoIngestionEngine` applies the same dedupe merge logic.

## Current Import UX
- Available options:
  - `Auto-import from SMS now`
  - `Sync from notifications (apps/email)`
- Removed from active menu:
  - AA/Broker/Email direct sync placeholders (not production-ready)

## Duplicate Prevention Strategy
- Each parsed signal carries:
  - `sourceKey` (unique event signature)
  - `correlationKey` (links debit/allotment/correction phases)
- Ingestion behavior:
  - Debit then allotment updates the same row where possible.
  - Corrections can revise amounts/platform/name.
  - Fuzzy fallback matching handles near-equal amounts across close time windows.

## Key Files
- `app/src/main/java/com/spendwise/ai/buckets/BucketsFragment.java`
- `app/src/main/java/com/spendwise/ai/buckets/BucketDetailFragment.java`
- `app/src/main/java/com/spendwise/ai/engine/BucketAllocationEngine.java`
- `app/src/main/java/com/spendwise/ai/engine/InvestmentAutoIngestionEngine.java`
- `app/src/main/java/com/spendwise/ai/sms/InvestmentSmsParser.java`
- `app/src/main/java/com/spendwise/ai/notifications/InvestmentNotificationListenerService.java`
- `app/src/main/java/com/spendwise/ai/notifications/InvestmentNotificationSyncHelper.java`
