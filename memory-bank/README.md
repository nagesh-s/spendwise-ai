# SpendWise AI Memory Bank

Canonical engineering reference for SpendWise AI. Read this directory for the current architecture, data model, and contributor rules. Use [the project knowledge base](../project-knowledge-base/README.md) for module guides, troubleshooting, and historical progress; dated entries are historical records rather than a description of the current release.

The public [README](../README.md) covers setup and product capabilities. The [privacy policy](../PRIVACY.md), [security policy](../SECURITY.md), and [contribution guide](../CONTRIBUTING.md) are authoritative for their respective topics.

## Current architecture

SpendWise AI is an offline-first Android app written in Java. It uses Views/XML, View Binding, fragments, `LiveData`, Room, and WorkManager. `MainActivity` hosts the primary navigation graph; onboarding and app-lock flows use their own activities.

Primary tabs are Dashboard, Calendar, Buckets, Merchants, and Settings. Categories, quick tiles, recurring expenses/investments, goals, analytics, auto transactions, profile, and bucket details are secondary destinations.

### Investment ledger

```text
SMS / notification / manual input
              ↓
       Parser + correction memory
              ↓
        IngestionEvent (source evidence)
              ↓
     InvestmentLedgerService (transaction boundary)
              ↓
Provider → Holding → InvestmentTransaction
                       ↘ InvestmentEventLink
                       ↘ ValuationSnapshot
```

A debit and an allotment are evidence for a logical contribution, not necessarily two contributions. `InvestmentLedgerService` owns evidence matching, transaction creation/confirmation, and materialized holding aggregates. Expected SIP occurrences do not count as holdings or net worth. See [data model](data-model.md) and [architecture](architecture.md).

`Investment` and `InvestmentSource` remain as legacy Java source files; they are **not** registered in the active Room schema. Do not use them for new work.

## Local processing and privacy

Room stores the app's database locally. Preferences and receipt images use app-local storage where applicable. `IngestionEvent` can retain the sender and raw body of source messages. The Storage Access Framework handles user-selected JSON/CSV/PDF files.

The app has no `INTERNET` permission and no SpendWise account/server. Deterministic text parsing, semantic suggestions, and optional `offlineLlm` parsing are on-device. Android's installed `SpeechRecognizer` provider can use an online service, so speech capture is not guaranteed to remain offline.

The `offlineLlm` flavor requires a separately licensed, user-supplied Gemma model file. Model weights are not included in this public repository. See [build and release](build-and-release.md) and [THIRD_PARTY_NOTICES](../THIRD_PARTY_NOTICES.md).

JSON backup is intentionally partial: it does not export every ledger table, source message, preference, or receipt, and is not a guaranteed full-device restore. See [import/export](import-export-json.md) and the [privacy policy](../PRIVACY.md).

## Engineering reference

| Document | Use |
| --- | --- |
| [Architecture](architecture.md) | Component responsibilities, ingestion, and background work |
| [Data model](data-model.md) | Current Room entities and relationships |
| [Room migrations](room-migrations.md) | Schema history, destructive-migration policy, and upgrade warning |
| [DAO query notes](room-daos-and-queries.md) | Representative active Room queries |
| [Navigation map](navigation-map.md) | Navigation XML destinations and arguments |
| [Build and release](build-and-release.md) | Toolchain, flavors, signing, and verification |
| [Stack and versions](stack-and-versions.md) | Dependency reference; Gradle remains authoritative |
| [Testing and quality](testing-and-quality.md) | Current tests, commands, and unrun-test reporting |
| [Java conventions](coding-standards-java.md) | Project coding patterns |
| [Privacy and security](privacy-and-security.md) | Sensitive-data and permission considerations |
| [Smart suggestions](smart-suggestions.md) | Expense and investment suggestion/assignment logic |
| [UI/UX](ui-ux-guidelines.md), [charts](charts-guide.md), [calendar](calendar-guide.md), [icons](icons-and-assets.md) | Presentation conventions |
| [Budgets](budgets-and-logic.md), [performance](performance.md) | Business and performance conventions |
| [Acceptance checklist](acceptance-checklist.md) | Manual feature verification notes |
| [Investment plan](Plan/PLAN.md) | Historical design plan; not a live implementation-status checklist |
