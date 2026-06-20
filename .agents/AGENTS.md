# Git Workflow Rules

1. **Never commit directly to `master`**.
2. **Staging Branch**: All active development should happen on the `staging` branch. Periodically, `staging` is merged into `master`.
3. **Feature Branches**: For every feature, bugfix, or release, create a new branch (e.g., `feat/review-queue`) off of `staging`.
4. **Merge and Delete**: Once the feature branch is merged into `staging`, the source branch MUST be deleted to maintain a clean repository.
