# SpendWise AI — Project Knowledge Base

Use this directory for **module navigation, troubleshooting, release checks, and historical progress**. The canonical current engineering reference is [`memory-bank/`](../memory-bank/README.md); use it for exact data-model, migration, privacy, build, and testing rules. The public project introduction is [`README.md`](../README.md).

The historical investment plan is not a source of implementation status. Source code and tests are authoritative.

## Documentation Structure

| File | Purpose | When to use |
|---|---|---|
| [Architecture](architecture.md) | Current components, data flow, and file map | Understand how features connect |
| [Troubleshooting](troubleshooting.md) | Symptom-oriented issue guide | Diagnose a problem |
| [Progress log](progress-log.md) | Dated milestones and current readiness | Resume work or understand history |
| [Release smoke checklist](release-smoke-checklist.md) | Manual and automated release gates | Verify a release candidate |
| [Memory Bank](../memory-bank/README.md) | Canonical engineering guides | Confirm current schema, APIs, and rules |

## Module Guides

| Module | File | Covers |
|---|---|---|
| Data layer | [data-layer.md](modules/data-layer.md) | Room schema 10, entities, DAOs, backup limits |
| Investment ledger | [investment-ledger.md](modules/investment-ledger.md) | Providers, holdings, transactions, evidence, aggregates |
| Buckets and schedules | [buckets.md](modules/buckets.md) | Salary allocation, bucket assignment, SIP reconciliation |
| Dashboard | [dashboard.md](modules/dashboard.md) | Charts, insights, voice input, navigation |
| Calendar | [calendar.md](modules/calendar.md) | Date selection and spending calendar |
| Expense editor | [expense-editor.md](modules/expense-editor.md) | Add/edit expenses and smart suggestions |
| Categories and budgets | [categories-budgets.md](modules/categories-budgets.md) | Category management and budget progress |
| Merchants | [merchants.md](modules/merchants.md) | Merchant analytics |
| Settings | [settings.md](modules/settings.md) | Preferences and partial JSON/CSV/PDF export |
| Profile and gamification | [profile-gamification.md](modules/profile-gamification.md) | App-local XP, badges, and profile |
| Onboarding | [onboarding.md](modules/onboarding.md) | First-run introduction and launch flow |

## Finding Issues

1. Start with [Troubleshooting](troubleshooting.md) and the affected module guide.
2. Inspect Logcat and compare actual behavior with the described flow.
3. Use [Memory Bank](../memory-bank/README.md) for current schema/data ownership rules.
4. Follow existing code patterns, use synthetic data, and run relevant unit/instrumentation tests.
5. Do not use old progress-log validation commands as evidence that today's tree passes.

For investment mismatches, inspect `ingestion_events`, `investment_event_links`, `investment_transactions`, and `holdings` as one evidence chain. Route investment writes through `InvestmentLedgerService`, not a parser, worker, listener, or UI DAO call.

## Package Map

```text
app/src/main/java/com/spendwise/ai/
├── MainActivity.java              # App shell and navigation
├── SpendWiseApplication.java     # ThreeTen init and worker scheduling
├── dashboard/ calendar/ categories/ merchants/
├── buckets/ recurring/ goals/ analytics/ quicktiles/
├── settings/ security/ onboarding/ gamification/
├── voice/ sms/ notifications/     # Input, parsing, and source listeners
├── domain/                       # Investment ledger write boundary
├── engine/                       # Allocation, ingestion, semantic/reconciliation logic
├── review/                       # Uncertain investment review flow
├── work/                         # WorkManager jobs and maintenance
├── data/
│   ├── entity/ dao/ pojo/ repository/
│   ├── export/                   # CSV/PDF reports
│   ├── AppDatabase.java          # Room schema 10 and migrations through 8→9
│   └── DataBackupManager.java    # Partial JSON backup format 3
└── util/                         # Date and shared helpers

app/src/main/res/
├── layout/ values/ values-night/ drawable/
│                                   # Views, themes, strings, and icons
```

`offlineLlm` requires a separately supplied, compatible licensed Gemma model; weights are not in the public repository. Speech recognition uses the installed Android provider and can involve online processing. See [`PRIVACY.md`](../PRIVACY.md).

## Current-State Warning

The app declares `versionName "1.9.0"` and Room schema **10**. There is no 9→10 migration/backfill, and instrumentation currently tests only the 3→4 migration. JSON export is not a complete backup. These are blockers for existing-user upgrade claims, not problems documentation alone can resolve.

## Last Updated
2026-11-10 — schema-10 architecture, ledger guide, migration/backup limitations, and documentation ownership.
