# REPOSITORY WRITE PATH & CACHE GOVERNANCE AUDIT v1.0

## Objective

Validate that repository write paths and cache behavior within the Jarvis Android application ("Jarvis Collector") comply with the remote-first architecture principles. This audit evaluates transaction write schedules, network failure scenarios, data freshness, and staleness risks to ensure remote Supabase remains the sole system of record.

---

# Section 1 - TodoRepository Write Flow

When a user modifies a task (Completes, Snoozes, or Dismisses/Deletes) in the UI, the [TodoRepository](file:///c:/jarvis/jarviscollector/app/src/main/java/com/pradeep/jarviscollector/repository/TodoRepository.kt) coordinates local database and remote REST API write cycles.

### Execution Path (For `markTodoComplete`, `snoozeTodo`, and `deleteTodo`)

```text
Step 1: User interacts with UI (e.g., taps "Complete")
  ↓
Step 2: UI calls TodoRepository.markTodoComplete(context, todoId)
  ↓
Step 3: Repository launches CoroutineScope (Dispatchers.IO)
  ↓
Step 4: Update Local Database (Room cache)
        - Calls getDao(context).updateStatus(id, "COMPLETED", timestamp)
  ↓
Step 5: Send REST PATCH call to Supabase
        - Payload: {"status": "COMPLETED", "updated_at": timestamp}
        - Call: JarvisInsightsClient.updateRow("todos", "todo_id=eq.$id", payload)
  ↓
Step 6: Log User Correction Action
        - Calls ActionsRepository.logAction(..., "todos", id, "todo_complete")
```

---

# Section 2 - Source Of Truth Verification

By tracing the write path, we verify when the local Room database updates relative to network responses:

* **Room Update Timeline**: Room is updated **before** the REST update success is verified. The repository launches a coroutine, writes immediately to the local SQLite table (`TodoDao.updateStatus`), and then makes the asynchronous OkHttp PATCH request to Supabase.
* **Failure Class**: 
  > [!WARNING]
  > **Classification: REMOTE RISK**
  > Because Room is updated before REST success, if the REST network call fails (due to timeout or bad gateway), the local Room cache will show `"COMPLETED"` while Supabase remains `"OPEN"`. During the next sync cycle, the local change will be overwritten by Supabase, causing the task to reappear on the user's dashboard.

---

# Section 3 - Failure Scenario Audit

Simulation of network failures on write paths and cache boundaries:

| Failure Scenario | Room Cache State | UI Presentation State | Remote Supabase State | Divergence Risk |
| --- | --- | --- | --- | --- |
| **Network Timeout** | Updated to COMPLETED | Shows completed (optimistic update) | Remains OPEN | Yes (Diverges until next pull sync overwrites cache) |
| **Supabase HTTP 500**| Updated to COMPLETED | Shows completed (optimistic update) | Remains OPEN | Yes (Diverges until next pull sync overwrites cache) |
| **Auth Failure** | Updated to COMPLETED | Shows completed (optimistic update) | Remains OPEN | Yes (Diverges until next pull sync overwrites cache) |
| **Device Offline** | Updated to COMPLETED | Shows completed (optimistic update) | Remains OPEN | Yes (Diverges until next pull sync overwrites cache) |

---

# Section 4 - Cache Freshness Audit

Evaluation of cached entity refresh watermarks:

* **Todos**:
  * *Last Updated Field*: `updated_at` (String ISO)
  * *Refresh Trigger*: `InsightSyncWorker` schedule or UI pull-to-refresh.
  * *Refresh Frequency*: Daily at 06:20 AM (via WorkManager) or manual triggers.
  * *TTL / Expiration Policy*: None (No local expiry watermark is checked).
* **FYI Alerts & Financial Events**:
  * *Last Updated Field*: `updated_at` (FYI) / `created_at` (Finance).
  * *Refresh Trigger / Frequency*: Daily at 06:20 AM.
  * *TTL*: None.
* **Daily Brief**:
  * *Last Updated Field*: `generatedAt` (String date).
  * *Refresh Trigger / Frequency*: Daily at 06:20 AM.
  * *TTL*: None.
* **User Preferences**:
  * *Last Updated Field*: `updated_at` (String ISO).
  * *Refresh Trigger / Frequency*: Daily at 06:20 AM.
  * *TTL*: None.

---

# Section 5 - Staleness Risk Assessment

Since the app relies on a cached read replica that updates on a daily schedule, there are risks of displaying stale data:

1. **Stale Todo**: **MEDIUM**. If the user completes a task on another client (Streamlit or Web Portal), it will remain "OPEN" on Android until the next synchronization cycle.
2. **Stale FYI / Financial Event**: **MEDIUM**. New transaction events occurring during the day are invisible to the mobile app until the daily worker runs or the user triggers a manual sync.
3. **Stale Daily Brief**: **LOW**. Briefs are compiled once daily (morning) centrally, matching the daily synchronization schedule.
4. **Stale Preference**: **LOW**. Configuration settings rarely change.

---

# Section 6 - Cache Governance Matrix

Cache guidelines for local SQLite tables:

| Cached Table | Cache Purpose | Cache Lifetime | Refresh Mechanism | Invalidation Mechanism | Offline Support Requirement |
| --- | --- | --- | --- | --- | --- |
| **`todos`** | Fast offline rendering | Until next pull | `InsightSyncService` bulk GET | Destructive Overwrite (`deleteAll`) | Read & Write (Queued actions) |
| **`financial_events`**| Offline reminder scan | Until next pull | `InsightSyncService` bulk GET | Destructive Overwrite (`deleteAll`) | Read Only |
| **`fyi_events`** | Offline alert scans | Until next pull | `InsightSyncService` bulk GET | Destructive Overwrite (`deleteAll`) | Read Only |
| **`daily_briefs`** | Offline summary reading | Until next pull | `InsightSyncService` bulk GET | Destructive Overwrite (`deleteAll`) | Read Only |
| **`user_preferences`**| Settings caching | Permanent | `InsightSyncService` bulk GET | Destructive Overwrite (`deleteAll`) | Read Only |

---

# Section 7 - Remote-First Compliance Recheck

Re-scoring compliance metrics without assuming perfect alignment:

* **`TodoRepository`**: **75** (Minor concern: Optimistic Room updates prior to REST success can lead to short-term state drift if networks fail).
* **`FinancialRepository`**: **100** (Strict read-only cache).
* **`FYIRepository`**: **100** (Strict read-only cache).
* **`PreferenceRepository`**: **75** (Minor concern: SharedPreferences cache defaults could override remote variables).
* **Cache Layer**: **100** (Cleanly handles destructive overwrites).
* **Sync Layer**: **100** (Uses direct OkHttp integrations mapped to Postgrest profiles).

---

# Section 8 - Architectural Recommendation

### 1. Repository Layer: **MINOR ADJUSTMENTS**
* *Recommendation*: Modify the write paths in `TodoRepository` (`markTodoComplete`, `snoozeTodo`, `deleteTodo`) to run the REST PATCH update **first**. Local Room updates and UI state changes should only execute **after** receiving a successful `200 OK` or `204 No Content` response from Supabase. If offline, the request should fail instantly or be cached in the user action queue without altering the primary Todo task list.

### 2. Cache Layer: **KEEP CURRENT DESIGN**
* *Recommendation*: The destructive invalidate-and-insert caching strategy prevents database fragmentation and resolves conflicts by designating Supabase as the source of truth.

### 3. Sync Layer: **KEEP CURRENT DESIGN**
* *Recommendation*: okHttp-based pulls mapped to the `jarvis_insights_schema` are robust. No modifications required.
