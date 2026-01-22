# SpendWise AI — Copilot Instructions

Authoritative context for code generation in this repository. Generate Java (AndroidX) with Views/XML, not Kotlin or Compose.

## Project Summary
- Single-activity host (`MainActivity`) with BottomNavigationView + NavHostFragment for fragment navigation.  
- Local-only persistence via Room; no network or cloud calls.  
- Charts: MPAndroidChart (PieChart, BarChart).  
- Calendar: MaterialCalendarView.  
- Icons: Font Awesome via bundled TTF.  
- Files: Storage Access Framework (SAF) for JSON export/import.  
- Theming: Material DayNight; Settings toggle chooses system / light / dark.

## Architectural Rules
- Use Java + Android Views/XML; do not introduce Jetpack Compose or Kotlin.  
- Fragments are thin; put transformations and business logic in ViewModels; use LiveData or observable patterns suitable for Java.  
- Navigation graph (`app_nav_graph.xml`) defines destinations: DashboardFragment, CalendarFragment, CategoriesBudgetsFragment, MerchantsFragment, SettingsFragment, and `ExpenseEditorDialogFragment`.  
- Bottom nav IDs map 1:1 with fragment IDs; use `NavController` for actions and dialog args.

## Data Model (canonical)
- Expense(id, dateEpochDay: long, amountCents: long, description: String, categoryId: long, merchantId: long)  
- Category(id, name: String, colorArgb: int, iconKey: String, isDefault: boolean)  
- Merchant(id, name: String)  
- Budget(id, monthKeyYYYYMM: String, categoryId: long, limitCents: long)  
- QuickTile(id, categoryId: long, merchantId: long, label: String, amountCents: long)
Indices: dateEpochDay, categoryId, merchantId, (monthKeyYYYYMM, categoryId).

## DAO Query Templates
Use null-safe aggregates and return primitive fields in POJOs for hot paths:
@Query("SELECT IFNULL(SUM(amountCents),0) FROM Expense WHERE dateEpochDay BETWEEN :start AND :end")
long totalForRange(long start, long end);

@Query("SELECT c.id AS categoryId, c.name AS name, IFNULL(SUM(e.amountCents),0) AS totalCents " +
"FROM Expense e JOIN Category c ON e.categoryId=c.id " +
"WHERE e.dateEpochDay BETWEEN :start AND :end " +
"GROUP BY c.id ORDER BY totalCents DESC")
List<CategoryTotal> categoryTotals(long start, long end);

@Query("SELECT m.id AS merchantId, m.name AS name, COUNT(e.id) AS txns, " +
"IFNULL(SUM(e.amountCents),0) AS totalCents, AVG(e.amountCents) AS avgCents " +
"FROM Expense e JOIN Merchant m ON e.merchantId=m.id " +
"WHERE e.dateEpochDay BETWEEN :start AND :end " +
"GROUP BY m.id ORDER BY totalCents DESC LIMIT :limit")
List<MerchantAgg> topMerchants(long start, long end, int limit);

## UI Guidelines
- Use ViewBinding in fragments/activities and Material Components for widgets.  
- Respect dark mode via DayNight theme and a Settings toggle; do not hardcode colors (use theme attributes + night resources).  
- Keep fragments lean; do not run heavy Room queries or JSON parsing on the main thread.

## Charts (MPAndroidChart)
- Reuse DataSet instances and call `chart.invalidate()`; avoid reallocating datasets on every observer tick.  
- Pie: category distribution; Bar: budget vs actual per category.  
Minimal patterns:
PieDataSet ds = new PieDataSet(entries, "By Category");
ds.setColors(colors);
pieChart.setData(new PieData(ds));
pieChart.invalidate();

## Calendar (MaterialCalendarView)
- Precompute a map of dateEpochDay -> dailyTotalCents for current month; decorators color cells by spend tiers.  
- On date select, open `ExpenseEditorDialogFragment` with args (dateEpochDay, optional expenseId).

## Import/Export (SAF JSON)
- Export: use ACTION_CREATE_DOCUMENT and write JSON via ContentResolver to the returned URI.  
- Import: use ACTION_OPEN_DOCUMENT, parse JSON off main thread, upsert into Room; validate schema version.  
- Do not request legacy external storage permissions.

## Smart Helpers
- Category and merchant suggestions: tokenize description, match recent/frequent history locally; no online calls.  
- Quick Add tiles: rank by recency-frequency (time-decayed).

## Testing & Quality Gates
- DAO tests with in-memory Room for aggregates and budgets.  
- Navigation smoke tests for bottom-nav routes and dialog flows.  
- Lint + basic UI checks must pass before PR merge.

## Performance & Privacy
- Cache current month aggregates in memory for Dashboard and Calendar.  
- Keep UI thread free of DB/IO; do not add analytics or trackers.  
- App is offline-first; adding network requires product sign-off.

## Strict Do/Don’t
DO: Java + AndroidX Views, Room, Navigation, MPAndroidChart, MaterialCalendarView, SAF, Font Awesome TTF.  
DON’T: Compose, Kotlin, Recharts/web libs, network calls, broad storage permissions, floating-point currency.

## FINAL NOTE
For deeper context, cross-reference the documentation in `memorybank/`— each topic above maps to a dedicated guide you can open for examples and edge cases.