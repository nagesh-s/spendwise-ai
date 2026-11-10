# SpendWise AI privacy policy

**Last updated:** 2026-11-10

This policy describes the SpendWise AI application in this repository. Before distributing a build under your own name, update this policy to identify that distributor and provide a working privacy contact. Google Play or another distributor may require a hosted privacy-policy URL and additional disclosures.

## Plain-language summary

SpendWise is designed to keep financial records on your device. Its core features do not require an account or SpendWise server, and the application manifest declares no Internet permission. Optional features read SMS or notifications only after you grant the relevant Android access. Exported documents and Android backups are outside the app's control once saved elsewhere.

**Important:** voice recognition uses Android's `SpeechRecognizer`. Your device's speech-recognition provider may process audio using an online service. Subsequent expense/investment parsing and bundled model inference run on-device, but SpendWise cannot promise that the speech-recognition step never transmits audio.

## Data the app handles

- Manually entered expenses, merchants, categories, accounts, budgets, goals, buckets, investments, schedules, valuations, notes, and optional receipt photos
- Expense and investment events parsed from permitted SMS or notification sources, including source text, sender, timestamps, parsing evidence, and learned correction/mapping choices
- In-app progress, settings, import cursors, and preferences stored in Room or SharedPreferences
- Voice transcripts returned by Android's speech-recognition service

No real personal financial records are included intentionally in this source repository.

## Permissions and optional features

### SMS

`READ_SMS` lets you scan or periodically import transaction messages. Processing occurs on-device. Depending on the parsed message, the app stores transaction details, sender, and raw source text in its local ingestion history. You can revoke SMS access in Android Settings or disable automatic import in the app. Disabling import or revoking access does not erase already imported data.

### Notification listener

Notification access is enabled separately in Android's special system settings. When enabled, the listener receives notifications available to it, filters supported financial content, and stores relevant financial text for local ingestion and source traceability. Revoke access in Android Settings to stop further ingestion. Revocation does not delete existing records.

### Microphone and speech recognition

`RECORD_AUDIO` is requested only when you choose voice entry. SpendWise passes the returned transcript to its local parser. Audio capture and recognition are performed by Android's available `SpeechRecognizer` provider. Whether that provider processes audio locally or remotely depends on the device, provider, language configuration, and Android version. Consult that provider's privacy policy or avoid voice input if this is unacceptable to you.

### Notifications

`POST_NOTIFICATIONS` is used for reminders, investment digests, and background-work status. Notification content may reveal financial information on your lock screen; Android notification settings control this behavior.

### Biometric lock

Biometric authentication is handled by Android's biometric APIs. The app does not request fingerprints or other biometric templates. It receives authentication outcomes and stores a lock preference. Biometric lock is a privacy aid, not encryption of exported documents or a replacement for device security.

## Storage, export, and backups

The primary database and preferences are stored in the app's local sandbox. No Internet permission is declared, and SpendWise does not operate an account or analytics server.

You may explicitly export JSON backups, CSV reports, or PDF reports through Android's Storage Access Framework. The destination and any subsequent sharing or synchronization are controlled by you and the apps/services involved. `DataBackupManager` JSON import/export covers only its documented entities; it is not a complete migration of every database table, receipt, preference, or raw ingestion record. JSON import merges supported records rather than providing a guaranteed full restore.

Android backup is enabled in the application manifest. Device or cloud backups may therefore include app data according to your Android version, device, backup settings, and provider. Protect your device and disable backup where required if that is not acceptable. Exported files are not encrypted by SpendWise.

## Deletion

You can delete supported individual records in the app. There is currently no single in-app control that deletes every table and preference. To remove app data, use Android's app-info screen and **Clear storage/data**, or uninstall the app; this local app data is not recoverable unless an external or Android backup exists. Then remove exported files, receipt copies, backups, and any cloud-synced copies separately using their respective storage providers.

For sensitive bug reports, use synthetic examples. Never send full bank SMS, account numbers, private keys, or exported financial databases.

## Security contact

Use the repository's **Security → Report a vulnerability** private-reporting flow once enabled by the maintainer. Until then, contact the repository maintainer privately through their public GitHub contact information. Do not open a public issue containing sensitive financial or security details. See [SECURITY.md](SECURITY.md).

## Children

SpendWise is a general personal-finance utility, not specifically directed to children. It has no user account system or age-verification mechanism.

## Changes

Update the date and description when behavior or distribution practices change. Release-specific privacy changes should also be recorded in the project's normal release notes.

## Disclaimer

This document records the behavior of the current source code and is not legal advice or a substitute for privacy disclosures required by your jurisdiction or app distributor.
