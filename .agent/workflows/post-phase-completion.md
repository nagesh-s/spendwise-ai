---
description: Mandatory checklist to run after completing any development phase
---

# Post-Phase Completion Checklist

// turbo-all

Run this workflow **every time** a phase (Phase 1, Phase 2, Phase 3, etc.) or a significant version milestone is completed.

## Steps

1. **Update CHANGELOG.md**
   - Add a new version entry at the top of `CHANGELOG.md`
   - Include `### Added`, `### Changed`, `### Fixed`, `### Technical` sections as applicable
   - List every feature with a bold label and one-line description
   - Include database version, migration, and permission details in Technical

2. **Update Memory Bank** (`memorybank/`)
   - `data-model.md` — Add any new entities, updated fields, and current DB version
   - `room-migrations.md` — Document new migration with SQL details
   - `navigation-map.md` — Add any new fragment routes
   - `stack-and-versions.md` — Update if new dependencies were added
   - `privacy-and-security.md` — Update if new permissions were added (e.g., READ_SMS)

3. **Update Project Knowledge Base** (`project-knowledge-base/`)
   - `progress-log.md` — Update current state section, add latest work bullet points, add version to history table
   - `architecture.md` — Add new screens to file map tables, update version history
   - If a new module was introduced, consider adding a guide in `modules/`

4. **Update task.md**
   - Mark the completed phase as ✅ in the header
   - Mark all sub-tasks as `[x]`

5. **Git Commit**
   - Stage all changes: `git add -A`
   - Commit with message: `feat(phase-N): [Phase Name] - [brief summary]`
   - Example: `feat(phase-2): Core Features - tags, accounts, goals, analytics, SMS reader`
   - Tag the version: `git tag vX.Y.Z`

6. **Notify User**
   - Summarize what was completed
   - Mention the version number and git tag
   - Ask about next steps

## Reminder
This workflow MUST be followed after every phase completion. It ensures:
- Future sessions can resume work without context loss
- The project history is traceable
- Documentation stays in sync with code
