All data remains local, with user-initiated JSON import/export via system file pickers for clear consent and sandboxed URIs.

No external network access is required for core operation, reducing surface area. Voice understanding fallback now runs fully on-device, and no remote endpoint is used for parsing speech into expense fields.

Biometric app lock supports biometric factors and device credential fallback where available.

If biometric hardware exists but enrollment is missing, settings should guide users to system enrollment rather than silently disabling the feature.

For advanced local inference, `offlineLlm` flavor uses on-device MediaPipe LLM with a bundled Gemma model asset auto-copied into app-private storage; no transcript is sent to a server.

SMS auto-import now stores only a local last-sync timestamp (`SmsSyncState`) to support incremental scans; message content is not uploaded.
