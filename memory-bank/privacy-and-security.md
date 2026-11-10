# Privacy and security

Read the public [PRIVACY.md](../PRIVACY.md) for user-facing behavior. These are implementation notes, not a promise that every feature always works offline.

- The manifest has no `INTERNET` permission. The app has no SpendWise server, account, advertising SDK, or analytics tracker.
- Room stores categories, expenses, investment ledgers, preferences-related state, and ingestion evidence locally. `IngestionEvent.sourceSender` and `sourceBody` may retain the sender and raw text of imported SMS/notification messages.
- SMS access (`READ_SMS`) requires a user-granted dangerous permission. Notification processing requires explicit Notification Listener access in system settings.
- Speech uses Android's installed `SpeechRecognizer` provider. Subsequent parsing is local, but that provider can process audio online; do not claim voice capture is always offline.
- `offlineLlm` performs optional local parsing after model provisioning, but Gemma weights are not bundled in this public repository. Their compatibility, license, and storage requirements must be handled by the distributor/user.
- The bundled Universal Sentence Encoder Lite model supports on-device semantic similarity. Attribution is in [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).
- Biometric lock supports biometric factors and, where available, device credentials. Do not treat the screen lock as encryption of exported files.
- JSON/CSV/PDF files are written to user-selected Storage Access Framework URIs. They are not encrypted by SpendWise and can contain sensitive financial data.
- JSON import/export is a partial backup, not a full database snapshot; raw ingestion events, the complete transaction ledger, preferences, and receipt images are not all covered.
- `android:allowBackup="true"` permits Android platform backup behavior; exported document storage is distinct from platform app backup.
- There is no single in-app delete-everything action. Removing the app and its local data does not delete separately exported documents.
- Synthetic fixtures only in public source/tests: no live SMS inboxes, real financial records, private signing material, or credentials.
- `key.properties`, signing keystores, and `local.properties` must remain untracked. Use the full-repository Gitleaks command in [SECURITY.md](../SECURITY.md) before publication.
- The experimental socket/federated-sync classes are not a production transport: they have no discovery protocol, model-authentication design, or production privacy guarantees and are inaccessible without normal app internet permission.
