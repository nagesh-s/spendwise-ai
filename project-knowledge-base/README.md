# SpendWise AI - Project Knowledge Base

> **Purpose**: Complete documentation for understanding, debugging, and extending the app. Designed for non-developers to find issues and AI agents to fix them.

---

## 📂 Documentation Structure

| File | Purpose | When to Use |
|------|---------|-------------|
| [architecture.md](./architecture.md) | **START HERE** - System overview, file map, data flow | Understanding how everything connects |
| [troubleshooting.md](./troubleshooting.md) | Issue catalog by symptom | When something breaks |
| [progress-log.md](./progress-log.md) | Development history, versions | Resume work or understand changes |

### Module Guides

| Module | File | Covers |
|--------|------|--------|
| Data Layer | [data-layer.md](./modules/data-layer.md) | Database, entities, DAOs, queries |
| Dashboard | [dashboard.md](./modules/dashboard.md) | Charts, monthly totals, navigation |
| Calendar | [calendar.md](./modules/calendar.md) | Date selection, spending dots |
| Expense Editor | [expense-editor.md](./modules/expense-editor.md) | Add/edit expenses, smart suggestions |
| Categories | [categories-budgets.md](./modules/categories-budgets.md) | Budget setting, progress tracking |
| Merchants | [merchants.md](./modules/merchants.md) | Spending analytics by merchant |
| Settings | [settings.md](./modules/settings.md) | Dark mode, import/export |

---

## 🔍 Finding Issues (For Non-Coders)

### Something Crashed?
1. Open `troubleshooting.md`
2. Find your symptom in the tables
3. The "File to Check" column tells where the problem is

### Feature Not Working?
1. Identify which screen has the issue
2. Open that module's guide (e.g., `calendar.md`)
3. Read "How It Works" to understand the flow
4. Check "Common Issues" section for known fixes

### Need to Explain Issue to an AI?
1. Say which screen/feature is broken
2. Describe what happens vs what should happen
3. Share any error messages from Logcat
4. Reference the file from documentation

---

## 🤖 For AI Agents

### First Time with This Project?
1. Read `architecture.md` for system overview
2. Understand the file map (which file does what)
3. Check data flow diagrams for how features work

### Fixing a Bug?
1. User describes symptom → find in `troubleshooting.md`
2. Identify "File to Check" → view that file
3. Fix the issue following the pattern in similar code
4. Test the fix

### Adding a Feature?
1. Find similar feature in module guides
2. Follow the same pattern (Fragment → ViewModel → Repository → DAO)
3. Add proper LiveData observers for UI updates

---

## 📁 Key File Locations

```
app/src/main/java/com/spendwise/ai/
├── MainActivity.java          # App entry
├── SpendWiseApplication.java  # ThreeTen init
├── dashboard/                 # Dashboard screen
├── calendar/                  # Calendar screen
├── categories/                # Categories & budgets
├── merchants/                 # Merchant analytics
├── settings/                  # Settings screen
├── common/                    # Shared (ExpenseAdapter, ExpenseEditor)
├── data/
│   ├── entity/               # Table definitions
│   ├── dao/                  # SQL queries
│   ├── pojo/                 # Query results
│   ├── repository/           # Business logic
│   ├── AppDatabase.java      # Room DB
│   └── DataBackupManager.java # Import/export
└── util/
    └── DateUtils.java        # Date helpers

app/src/main/res/
├── layout/                   # Screen layouts
├── values/                   # Light theme, colors, strings
├── values-night/             # Dark theme
└── drawable/                 # Icons
```

---

## 📅 Last Updated
2026-02-08 - v1.1.1 docs refreshed for UX + reliability enhancement pass
