# Daily Brief — Android Implementation Plan

> **Status**: Backend sync working. Android screen is legacy. ViewModel missing. This document is the complete implementation blueprint.

---

## 1. CURRENT BACKEND CAPABILITY

### Supabase Table: `daily_briefs` (in `jarvis_insights_schema`)

Confirmed fields from live REST API inspection:

| Field | Type | Notes |
| :--- | :--- | :--- |
| `id` or `brief_id` | UUID / text | Primary key |
| `generated_at` | timestamptz | When the brief was generated |
| `version` | text | Brief version string e.g. "1.0" |
| `content` or `items_json` | text / jsonb | JSON array of brief content strings |
| `brief_type` | text | Expected: "MORNING" or "EVENING" (not yet in Android entity) |
| `payload_json` | jsonb | Extended brief metadata (not yet in Android entity) |

### Current Sync Status
✅ `InsightSyncService.syncInsights()` fetches `daily_briefs` table.
✅ Mapped fields: `brief_id or id → id`, `content or items_json or itemsJson → itemsJson`, `generated_at → generatedAt`, `version → version`.
✅ Data successfully writes to Room `daily_briefs` table.
❌ `brief_type` and `payload_json` not yet extracted or stored.

---

## 2. PHASE 1 — ROOM ENTITY EXTENSION

### Current Entity
```kotlin
@Entity(tableName = "daily_briefs")
data class DailyBriefEntity(
    @PrimaryKey val id: String,
    val generatedAt: String,
    val version: String,
    val itemsJson: String
)
```

### Required Entity (after Phase 1)
```kotlin
@Entity(tableName = "daily_briefs")
data class DailyBriefEntity(
    @PrimaryKey val id: String,
    val generatedAt: String,
    val version: String,
    val itemsJson: String,        // JSON array of brief content strings
    val briefType: String?,       // "MORNING" or "EVENING"
    val payloadJson: String?      // Optional JSON metadata blob
)
```

### Room Migration Required
- Bump `JarvisDatabase` version from 5 → 6.
- Add `AutoMigration(from = 5, to = 6)` **or** keep `fallbackToDestructiveMigration()`.
- Recommended: keep `fallbackToDestructiveMigration()` in development.

### InsightSyncService Update
In `InsightSyncService.kt`, in the `DailyBrief` sync block, extract additional fields:
```kotlin
DailyBriefEntity(
    id = obj.optString("brief_id", obj.optString("id", "")),
    generatedAt = obj.optString("generated_at", ""),
    version = obj.optString("version", "1.0"),
    itemsJson = obj.optString("content", obj.optString("items_json", "[]")),
    briefType = if (obj.isNull("brief_type")) null else obj.optString("brief_type"),
    payloadJson = if (obj.isNull("payload_json")) null else obj.optString("payload_json")
)
```

---

## 3. PHASE 2 — DAO UPDATE

File: `InsightDaos.kt`

### Current DailyBriefDao
```kotlin
@Dao
interface DailyBriefDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(brief: DailyBriefEntity)

    @Query("SELECT * FROM daily_briefs ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getLatest(): DailyBriefEntity?

    @Query("SELECT * FROM daily_briefs ORDER BY generatedAt DESC")
    suspend fun getAll(): List<DailyBriefEntity>

    @Query("DELETE FROM daily_briefs")
    suspend fun deleteAll()
}
```

### Add These Methods
```kotlin
// Reactive latest brief for ViewModel observation
@Query("SELECT * FROM daily_briefs ORDER BY generatedAt DESC LIMIT 1")
fun getLatestFlow(): Flow<DailyBriefEntity?>

// Filter by brief type (MORNING / EVENING)
@Query("SELECT * FROM daily_briefs WHERE briefType = :type ORDER BY generatedAt DESC LIMIT 1")
suspend fun getLatestByType(type: String): DailyBriefEntity?

@Query("SELECT * FROM daily_briefs WHERE briefType = :type ORDER BY generatedAt DESC LIMIT 1")
fun getLatestFlowByType(type: String): Flow<DailyBriefEntity?>
```

---

## 4. PHASE 3 — REPOSITORY

### Create: `DailyBriefRepository.kt`

**Pattern**: Kotlin object (consistent with all other repositories).

```kotlin
object DailyBriefRepository {

    private fun getDao(context: Context) = JarvisDatabase.getDatabase(context).dailyBriefDao()

    suspend fun getLatest(context: Context): DailyBriefEntity? {
        return getDao(context).getLatest()
    }

    suspend fun getLatestByType(context: Context, type: String): DailyBriefEntity? {
        return getDao(context).getLatestByType(type)
    }

    fun getLatestFlow(context: Context): Flow<DailyBriefEntity?> {
        return getDao(context).getLatestFlow()
    }

    fun getLatestFlowByType(context: Context, type: String): Flow<DailyBriefEntity?> {
        return getDao(context).getLatestFlowByType(type)
    }

    suspend fun getAll(context: Context): List<DailyBriefEntity> {
        return getDao(context).getAll()
    }
}
```

---

## 5. PHASE 4 — VIEWMODEL

### Create: `DailyBriefViewModel.kt`

**Location**: `ui/dashboard/DailyBriefViewModel.kt` (or `ui/brief/`)

**Pattern**: AndroidViewModel with StateFlow (same pattern as `HomeDashboardViewModel`).

```kotlin
data class BriefSectionItem(
    val text: String,
    val index: Int
)

data class DailyBriefUiState(
    val sections: List<BriefSectionItem> = emptyList(),
    val generatedAt: String = "",
    val briefType: String = "MORNING",
    val isEmpty: Boolean = false,
    val isLoading: Boolean = true,
    val isError: Boolean = false
)

class DailyBriefViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DailyBriefUiState())
    val uiState: StateFlow<DailyBriefUiState> = _uiState.asStateFlow()

    init {
        loadBrief()
        observeLiveUpdates()
    }

    fun loadBrief() {
        viewModelScope.launch {
            try {
                val brief = DailyBriefRepository.getLatest(getApplication())
                if (brief == null) {
                    _uiState.value = DailyBriefUiState(
                        isLoading = false, isEmpty = true
                    )
                    return@launch
                }

                val parsed = parseBriefSections(brief.itemsJson)

                _uiState.value = DailyBriefUiState(
                    sections = parsed,
                    generatedAt = brief.generatedAt,
                    briefType = brief.briefType ?: "MORNING",
                    isEmpty = parsed.isEmpty(),
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load daily brief", e)
                _uiState.value = DailyBriefUiState(isLoading = false, isError = true)
            }
        }
    }

    private fun parseBriefSections(itemsJson: String): List<BriefSectionItem> {
        return try {
            val array = JSONArray(itemsJson)
            (0 until array.length()).map { i ->
                BriefSectionItem(text = array.getString(i), index = i)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun observeLiveUpdates() {
        viewModelScope.launch {
            DailyBriefRepository.getLatestFlow(getApplication()).collectLatest {
                loadBrief()
            }
        }
    }

    companion object {
        private const val TAG = "DailyBriefViewModel"
    }
}
```

---

## 6. PHASE 5 — COMPOSE SCREEN REDESIGN

### Rewrite: `DailyBriefScreen.kt`

**Replace the current parameter-heavy composable with a ViewModel-driven screen.**

**Required Layout Structure**:

```
DailyBriefScreen
├── TopAppBar
│   ├── "Daily Brief"
│   ├── Generated-at timestamp
│   └── Back button
│
├── [LOADING STATE]  → CircularProgressIndicator
│
├── [EMPTY STATE]    → "No brief available for today"
│
├── [ERROR STATE]    → "Unable to load brief"
│
└── LazyColumn
    ├── Item: HEADER CARD
    │   ├── Greeting ("Good morning, Pradeep")
    │   ├── Date (formatted)
    │   └── Brief type badge (MORNING / EVENING)
    │
    ├── Items: BRIEF CONTENT CARDS
    │   └── One card per BriefSectionItem
    │       ├── Numbered index pill
    │       └── Text content
    │
    └── Item: QUICK STATS ROW (optional, from HomeDashboard data)
        ├── Open Tasks count
        └── Unread Facts count
```

**Design**: Dark slate `#0F172A` background, `#1E293B` cards. Match Home Dashboard styling exactly.

**Signature**:
```kotlin
@Composable
fun DailyBriefScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyBriefViewModel = viewModel()
)
```

> **Important**: Remove the current parameter-heavy signature that passes todos, financialEvents, fyiEvents, facts, notifications, etc. The new screen only consumes the ViewModel. Adjust `JarvisNavigation.kt` composable binding accordingly.

---

## 7. PHASE 6 — NOTIFICATION INTEGRATION

### Create: `DailyBriefNotificationHelper.kt`

**Responsibilities**:
- Schedule a morning notification at 7:00 AM.
- Schedule an evening notification at 8:00 PM.
- On notification tap: deep-link to `brief` route.

**Pattern**: Use `WorkManager` with `setInitialDelay()` calculated from current time to next trigger.

```kotlin
object DailyBriefNotificationHelper {

    private const val MORNING_WORK_TAG = "daily_brief_morning"
    private const val EVENING_WORK_TAG = "daily_brief_evening"

    fun scheduleMorningBrief(context: Context) {
        val delay = calculateDelayToTime(hour = 7, minute = 0)
        val work = OneTimeWorkRequestBuilder<DailyBriefNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(MORNING_WORK_TAG)
            .setInputData(workDataOf("brief_type" to "MORNING"))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(MORNING_WORK_TAG, ExistingWorkPolicy.REPLACE, work)
    }

    fun scheduleEveningBrief(context: Context) {
        val delay = calculateDelayToTime(hour = 20, minute = 0)
        val work = OneTimeWorkRequestBuilder<DailyBriefNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(EVENING_WORK_TAG)
            .setInputData(workDataOf("brief_type" to "EVENING"))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(EVENING_WORK_TAG, ExistingWorkPolicy.REPLACE, work)
    }

    private fun calculateDelayToTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
```

### Create: `DailyBriefNotificationWorker.kt`

```kotlin
class DailyBriefNotificationWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val briefType = inputData.getString("brief_type") ?: "MORNING"
        val brief = DailyBriefRepository.getLatestByType(applicationContext, briefType)

        if (brief != null) {
            showBriefNotification(applicationContext, briefType, brief.itemsJson)
            // Reschedule for next day
            DailyBriefNotificationHelper.let {
                if (briefType == "MORNING") it.scheduleMorningBrief(applicationContext)
                else it.scheduleEveningBrief(applicationContext)
            }
        }

        return Result.success()
    }

    private fun showBriefNotification(context: Context, type: String, itemsJson: String) {
        val title = if (type == "MORNING") "☀️ Your Morning Brief" else "🌙 Evening Summary"
        val firstLine = try {
            JSONArray(itemsJson).optString(0, "Your daily brief is ready")
        } catch (e: Exception) {
            "Your daily brief is ready"
        }

        val deepLinkIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deep_link_route", "brief")
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "daily_brief_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(firstLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(firstLine))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(type.hashCode(), notification)
    }
}
```

### Notification Channel Setup
In `MainActivity.kt` or `Application.onCreate()`:
```kotlin
val channel = NotificationChannel(
    "daily_brief_channel",
    "Daily Brief",
    NotificationManager.IMPORTANCE_DEFAULT
).apply {
    description = "Morning and evening JARVIS briefings"
}
NotificationManagerCompat.from(context).createNotificationChannel(channel)
```

---

## 8. PHASE 7 — HOME DASHBOARD INTEGRATION

### Add Brief Preview Card to HomeScreen

In `HomeDashboardViewModel`, add a `latestBriefPreview: String?` field:
```kotlin
data class HomeDashboardUiState(
    // existing fields...
    val latestBriefPreview: String? = null,  // First line of latest brief
    val briefGeneratedAt: String? = null
)
```

Load the brief in `loadDashboardData()`:
```kotlin
val latestBrief = db.dailyBriefDao().getLatest()
val briefPreview = latestBrief?.let {
    try { JSONArray(it.itemsJson).optString(0) } catch (e: Exception) { null }
}
```

In `HomeScreen.kt`, add a "Today's Brief" card above the summary section:
```
Today's Brief Card
├── "DAILY BRIEF" label
├── First line of brief preview (truncated)
├── Generated at timestamp
└── "Read More" chevron → navigate to "brief"
```

---

## 9. FULL DATA FLOW DIAGRAM

```
Supabase
└── daily_briefs table
    └── GET /rest/v1/daily_briefs
        └── InsightSyncService.syncInsights()
            └── JSON mapping: id, generatedAt, itemsJson, briefType, payloadJson
                └── DailyBriefDao.insert()
                    └── Room: daily_briefs table
                        └── DailyBriefRepository.getLatestFlow()
                            └── DailyBriefViewModel
                                └── DailyBriefUiState (sections, generatedAt)
                                    └── DailyBriefScreen (Compose)
                                        └── LazyColumn of brief content cards

                        └── DailyBriefRepository.getLatestByType("MORNING")
                            └── DailyBriefNotificationWorker
                                └── Local notification with deep-link to "brief"

                        └── DailyBriefDao.getLatest()
                            └── HomeDashboardViewModel (brief preview)
                                └── HomeScreen "Today's Brief" card
```

---

## 10. ACCEPTANCE CRITERIA

| # | Criterion | Verification |
| :--- | :--- | :--- |
| 1 | `DailyBriefEntity` includes `briefType` and `payloadJson` | View `InsightEntities.kt` |
| 2 | Sync extracts `brief_type` from Supabase response | Check `InsightSyncService` mapping |
| 3 | `DailyBriefViewModel` exposes reactive `StateFlow<DailyBriefUiState>` | Code review |
| 4 | `DailyBriefScreen` renders without crashing when brief is null | Test with empty DB |
| 5 | `DailyBriefScreen` renders brief content sections correctly | Test with data |
| 6 | Morning notification fires at 7:00 AM with brief content | Device testing |
| 7 | Evening notification fires at 8:00 PM | Device testing |
| 8 | Tapping notification navigates to Brief screen | Deep-link test |
| 9 | Home Dashboard shows brief preview card | Visual verification |
| 10 | Auto-refresh works — new sync updates the Brief screen without restart | Background sync test |

---

## 11. FILES TO CREATE / MODIFY

| Action | File | Notes |
| :--- | :--- | :--- |
| **MODIFY** | `model/InsightEntities.kt` | Add `briefType` and `payloadJson` to `DailyBriefEntity` |
| **MODIFY** | `database/InsightDaos.kt` | Add `getLatestFlow()`, `getLatestByType()`, `getLatestFlowByType()` to `DailyBriefDao` |
| **MODIFY** | `database/JarvisDatabase.kt` | Bump version to 6 |
| **MODIFY** | `service/InsightSyncService.kt` | Extract `brief_type` and `payload_json` in brief mapping |
| **CREATE** | `repository/DailyBriefRepository.kt` | New repository object |
| **CREATE** | `ui/brief/DailyBriefViewModel.kt` | New ViewModel with StateFlow |
| **REWRITE** | `ui/DailyBriefScreen.kt` | Remove parameter-heavy signature, consume ViewModel |
| **CREATE** | `service/DailyBriefNotificationHelper.kt` | Scheduler for morning/evening |
| **CREATE** | `service/DailyBriefNotificationWorker.kt` | WorkManager worker |
| **MODIFY** | `ui/dashboard/HomeDashboardViewModel.kt` | Add `latestBriefPreview` field |
| **MODIFY** | `ui/HomeScreen.kt` | Add "Today's Brief" card |
| **MODIFY** | `navigation/JarvisNavigation.kt` | Update `brief` composable binding to use new ViewModel-driven screen |
| **MODIFY** | `MainActivity.kt` | Register notification channel |
