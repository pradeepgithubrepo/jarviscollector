# REPOSITORY, SERVICE & TECHNICAL DEBT CLEANUP ASSESSMENT v1.0

## Objective

Assess the repository patterns, background services, WorkManager tasks, utilities, database entities, and technical debt inside the Jarvis Android application ("Jarvis Collector"). 

The purpose is to catalog reusable components, isolate dead code, identify cleanup opportunities, and ensure complete compliance with the **JARVIS Platform Anchor Document v1.0** and the reuse governance rules before V2 modernization.

---

# Section 1 - Repository Inventory

The application defines 8 repository files to coordinate SQLite Room access and network syncing.

1. **`MobileSignalRepository`**:
   * *Purpose*: Manages CRUD operations for raw intercepted mobile SMS and notification signals.
   * *Dependencies*: `MobileSignalDao`, `Context`.
   * *Consumers*: `SyncService`, `JarvisNotificationListener`, `MainActivity`, `NotificationScreen`.
   * *Status*: **ACTIVE**
2. **`SmsRepository`**:
   * *Purpose*: Incremental scrapes of local inbox SMS records.
   * *Dependencies*: `ContentResolver`, `Context`.
   * *Consumers*: `MainActivity`, `NotificationScreen`.
   * *Status*: **ACTIVE**
3. **`NotificationRepository`**:
   * *Purpose*: Holds transient, in-memory logs of WhatsApp notification signals for UI presentation.
   * *Dependencies*: None.
   * *Consumers*: `MainActivity`, `NotificationScreen`, `JarvisNotificationListener`.
   * *Status*: **ACTIVE**
4. **`TodoRepository`**:
   * *Purpose*: Synchronizes local task modifications and syncs them back to Supabase.
   * *Dependencies*: `TodoDao`, `JarvisInsightsClient`.
   * *Consumers*: `MainActivity`, `TodoScreen`, `TodoNotificationWorker`.
   * *Status*: **ACTIVE**
5. **`FinancialRepository`**:
   * *Purpose*: Direct read queries to local cached transaction events.
   * *Dependencies*: `FinancialEventDao`.
   * *Consumers*: `MainActivity`, `HomeScreen`.
   * *Status*: **ACTIVE**
6. **`FYIRepository`**:
   * *Purpose*: Direct read queries to local cached FYI alert events.
   * *Dependencies*: `FyiEventDao`.
   * *Consumers*: `MainActivity`, `HomeScreen`.
   * *Status*: **ACTIVE**
7. **`PreferenceRepository`**:
   * *Purpose*: Synchronizes local preference configuration records.
   * *Dependencies*: `UserPreferenceDao`.
   * *Consumers*: `MainActivity`.
   * *Status*: **PARTIAL** (Staged, minimal usage).
8. **`ActionsRepository`**:
   * *Purpose*: Stub repository designed to log user action feedback.
   * *Dependencies*: `UserActionDao`.
   * *Consumers*: `TodoRepository` (logs dismissal/snooze actions).
   * *Status*: **OBSOLETE** (UI layer does not consume it; functions as a background logging utility).

---

# Section 2 - Repository Duplication Analysis

* **Duplicate Responsibilities**: None. Each repository owns distinct schema boundary objects.
* **Overlapping Repositories**: `SmsRepository` and `MobileSignalRepository` overlap slightly on data capture, but they are logically separated (`SmsRepository` scrapes Android content providers, whereas `MobileSignalRepository` handles Room persistence).
* **Repository Bloat**: The codebase lacks a dedicated repository for **Daily Briefs** and **Facts**, forcing `MainActivity` to execute direct database queries.

---

# Section 3 - Service Inventory

1. **`SyncService`**:
   * *Purpose*: Serializes pending local signals and uploads them to Supabase Storage.
   * *Dependencies*: `MobileSignalRepository`, `SupabaseUploader`, `JsonExporter`.
   * *Consumers*: `JarvisSyncWorker`, `MainActivity`.
   * *Status*: **ACTIVE**
2. **`InsightSyncService`**:
   * *Purpose*: Pulls downstream updates from Supabase REST endpoints.
   * *Dependencies*: `JarvisInsightsClient`, `JarvisDatabase`, `org.json.JSONArray`.
   * *Consumers*: `InsightSyncWorker`, `MainActivity`.
   * *Status*: **ACTIVE**
3. **`JarvisNotificationListener`** (Service):
   * *Purpose*: OS-level notification binder catching incoming WhatsApp messages.
   * *Dependencies*: `NotificationNoiseFilter`, `MobileSignalRepository`, `NotificationRepository`.
   * *Consumers*: Android OS (registered in Manifest).
   * *Status*: **ACTIVE**

---

# Section 4 - Service Consolidation Analysis

* **Consolidation Opportunity**: `SyncService` and `InsightSyncService` are organized as separate Kotlin objects. While they perform opposite flows (Upload vs. Download), their orchestration logic and error tracking can be consolidated under a single coordination manager: `JarvisSyncManager`.

---

# Section 5 - Worker Inventory

1. **`JarvisSyncWorker`**:
   * *Schedule*: Runs 3 times daily (05:55 AM, 01:55 PM, 08:55 PM).
   * *Purpose*: Automates background SMS and notification signal uploads.
   * *Dependencies*: `SyncService`.
   * *Current Usage*: Initialized on App start.
2. **`InsightSyncWorker`**:
   * *Schedule*: Runs daily at 06:20 AM.
   * *Purpose*: Automates background downloads of downstream tables.
   * *Dependencies*: `InsightSyncService`.
   * *Current Usage*: Initialized on App start.
3. **`TodoNotificationWorker`**:
   * *Schedule*: Runs twice daily (07:00 AM, 06:00 PM).
   * *Purpose*: Checks due dates and triggers local system task alerts.
   * *Dependencies*: `TodoRepository`.
   * *Current Usage*: Initialized on App start.

---

# Section 6 - Worker Consolidation Analysis

* **Duplicated Schedules**: The workers run on separate schedules.
* **Simplification Opportunity**:
  * `JarvisSyncWorkerHelper` and `InsightSyncWorkerHelper` are duplicate files that manage WorkManager constraint setups. They should be merged into a single `JarvisWorkScheduler` utility.

---

# Section 7 - Utility Inventory

1. **`JsonExporter`**:
   * *Purpose*: Converts list data to JSON strings for file uploads.
   * *Usage*: High (Called during background uploads).
   * *Reuse Potential*: High (Standard JSON helper).
2. **`NotificationNoiseFilter`**:
   * *Purpose*: String matching algorithm filters out junk WhatsApp messages.
   * *Usage*: High (Intercepts incoming signals).
   * *Reuse Potential*: High.
3. **`AppPreferences`**:
   * *Purpose*: SharedPreferences wrapper handling simple key-value states (e.g. Owner name).
   * *Usage*: High (MainActivity uses it to store backfill and registration states).
   * *Reuse Potential*: High.

---

# Section 8 - Entity Assessment

Evaluation of Room `@Entity` classes:

* **`MobileSignal`**: **ACTIVE** (Main ingestion cache).
* **`TodoEntity`**: **ACTIVE** (Downstream tasks cache).
* **`FinancialEventEntity`**: **ACTIVE** (Transactions cache).
* **`FyiEventEntity`**: **ACTIVE** (Notifications cache).
* **`UserPreferenceEntity`**: **ACTIVE** (Preference variables cache).
* **`DailyBriefEntity`**: **ACTIVE** (Daily brief cache).
* **`UserActionEntity`**: **PARTIAL** (Logs actions locally before uploading).
* **`SignalEntity`**: **OBSOLETE** (Duplicate of `MobileSignal`; unused stub).
* **`FactEntity`**: **OBSOLETE** (Unused stub).
* **`MerchantMappingEntity`**: **OBSOLETE** (Unused stub).

---

# Section 9 - DAO Assessment

Evaluation of Room database DAOs:

* **`MobileSignalDao`**: Fetches un-synced local signals. **Active.**
* **`TodoDao`**: Handles task toggles and fetches. **Active.**
* **`FinancialEventDao`**: Direct SELECT queries for transactions. **Active.**
* **`FyiEventDao`**: Direct SELECT queries for FYI alerts. **Active.**
* **`UserPreferenceDao`**: Direct key-value accessors. **Active.**
* **`UserActionDao`**: Caches logged user corrections. **Active.**
* **`DailyBriefDao`**: Accesses latest briefs. **Active.**

---

# Section 10 - Technical Debt Inventory

* **Unused Classes**:
  * `com.pradeep.jarviscollector.model.SignalEntity` (Room schema class).
  * `com.pradeep.jarviscollector.model.FactEntity` (Room schema class).
  * `com.pradeep.jarviscollector.model.MerchantMappingEntity` (Room schema class).
* **Unused Methods**:
  * `SmsRepository.readRecentSms` has duplicated blocks matching `SmsRepository.importRecentSmsToRoom`.
* **Duplicate Category Screens**: 5 identical layouts (`FamilyScreen.kt`, etc.) with copy-pasted rendering blocks.

---

# Section 11 - Test Asset Governance Assessment

* **`ExampleUnitTest.kt`**: Boilerplate empty testing stub. **Status: REMOVE**
* **`ExampleInstrumentedTest.kt`**: Boilerplate empty instrumentation stub. **Status: REMOVE**
* **Findings**: The workspace has no functional unit or instrumentation tests. 

---

# Section 12 - Duplication Assessment

1. **Duplicate Scheduling Helpers**: `JarvisSyncWorkerHelper` and `InsightSyncWorkerHelper` contain identical WorkManager configuration setups.
2. **Duplicate Category Filtering**: Category filters are hardcoded in both `MainActivity.kt` and sub-screens.
3. **Duplicate Local Generator**: `InsightSyncService` builds hardcoded templates that duplicate pipeline logic.

---

# Section 13 - Reuse Governance Compliance

Rule 2 of the Platform Anchor Document mandates **Reuse Before Create**. 

* **Compliance Status**: **Partial Compliance**.
* **Violations**: The creation of 5 separate screen files (`FamilyScreen`, `SchoolScreen`, etc.) for category filtering violates the reuse rule. Instead of extending `FyiScreen` or parameterizing list rendering, new redundant files were created.
* **Recommendation**: Consolidate layouts into parameterized composables.

---

# Section 14 - Cleanup Candidates

### 1. LOW RISK REMOVALS
* **`SignalEntity`**, **`FactEntity`**, and **`MerchantMappingEntity`**: These stub models are unused in repositories and can be safely deleted or refactored.
* Boilerplate tests (`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`).

### 2. MEDIUM RISK REMOVALS
* Duplicate category screens (`FamilyScreen.kt`, `SchoolScreen.kt`, `TravelScreen.kt`, `HealthScreen.kt`, `ShoppingScreen.kt`) once a consolidated `FyiCategoryListScreen` is implemented.

### 3. HIGH RISK REMOVALS
* Local brief text builder inside `InsightSyncService.kt` (only after replacing it with direct Supabase fetch integration).

---

# Section 15 - Android V2 Foundation Readiness

* **Readiness Evaluation**: **PARTIAL**
* **Blockers**:
  1. The app lacks an active facts download sync route.
  2. The daily brief is generated locally instead of downloading remote LLM summaries.
  3. UI navigation is hardcoded via in-memory states instead of Navigation Compose.

---

# Section 16 - Recommendation Matrix

| Component | Status | Recommendation | Action | Justification |
| --- | --- | --- | --- | --- |
| **`SignalEntity`** | Unused | **REMOVE** | Delete file | Unused Room model. |
| **`FactEntity`** | Unused | **MODIFY** | Retain & Reuse | Keep as the caching model for Facts V2. |
| **`MerchantMappingEntity`**| Unused | **REMOVE** | Delete file | Unused Room model. |
| **`ActionsRepository`** | Unused | **REMOVE** | Delete file | Actions can be handled directly by repositories. |
| **`InsightSyncService`** | Active | **MODIFY** | Code refactor | Remove local templates; sync facts & remote briefs. |
| **`JarvisSyncWorkerHelper`**| Active | **CONSOLIDATE**| Merge files | Merge with `InsightSyncWorkerHelper` scheduler. |

---

# Conclusion & Success Criteria Answers

1. **What repositories should survive**: `MobileSignalRepository`, `TodoRepository`, `FinancialRepository`, and `FYIRepository`. A new `FactsRepository` and `DailyBriefRepository` must be created.
2. **What services should survive**: `JarvisNotificationListener`, `SyncService`, and `InsightSyncService`.
3. **What workers should survive**: `JarvisSyncWorker`, `InsightSyncWorker`, and `TodoNotificationWorker` (with updated schedules).
4. **What dead code exists**: `SignalEntity`, `MerchantMappingEntity`, and boilerplate project tests.
5. **What technical debt exists**: Duplicate category screen files, duplicate WorkManager helper classes, and hardcoded UI filter configurations.
6. **What cleanup should occur**: Delete unused stubs, merge duplicate scheduling helper classes, and replace local daily brief templates with remote table integration.
7. **Whether the codebase follows reuse governance**: Partially. Category screen duplication violates reuse rules, which must be resolved during the V2 refactor.
