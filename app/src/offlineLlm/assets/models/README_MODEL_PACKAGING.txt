Place the bundled offline model file here for zero-setup users:

  spendwise_gemma3_1b.task

Expected runtime behavior:
- App flavor: offlineLlm
- On first voice-use, MediaPipeGemmaExpenseExtractor copies asset
  models/spendwise_gemma3_1b.task
  into app-private storage:
  <filesDir>/models/spendwise_gemma3_1b.task
- If missing or invalid, app falls back to semantic on-device parser.

Do not commit proprietary model binaries to public repositories.
