# REMOTE-FIRST ARCHITECTURE COMPLIANCE ASSESSMENT v1.0

## Objective

Validate the Jarvis Android application ("Jarvis Collector") against the **Jarvis Remote-First Architecture Principle**. 

Under this model, the Android application acts strictly as a **Signal Collector** (ingesting raw notifications and SMS messages) and a **Thin Client** (displaying parsed reports). It must never become a secondary source of truth, perform AI reasoning, or execute financial calculations locally. All authoritative configurations must reside on remote Supabase.

---

# Section 1 - Source of Truth Inventory

Evaluation of database management states across major features:

| Major Feature | Source of Truth | Current Source | Desired Source | Alignment Status |
| --- | --- | --- | --- | --- |
| **Daily Brief** | Remote Supabase | Local Device (templated strings) | Remote Supabase | **NON-COMPLIANT** |
| **Todos** | Remote Supabase | Remote Supabase (via local cache updates) | Remote Supabase | **COMPLIANT** |
| **FYI Alerts** | Remote Supabase | Remote Supabase (via local cache updates) | Remote Supabase | **COMPLIANT** |
| **Facts** | Remote Supabase | N/A (Stubbed, not synced) | Remote Supabase | **NON-COMPLIANT** |
| **Finance** | Remote Supabase | Remote Supabase (via local cache updates) | Remote Supabase | **COMPLIANT** |
| **Preferences** | Remote Supabase | Local device SharedPreferences | Remote Supabase | **PARTIALLY COMPLIANT** |
| **User Actions** | Remote Supabase | Local Room queue, synced immediately | Remote Supabase | **COMPLIANT** |
| **Raw Signals** | Local Device (temp) | Local Room DB, uploaded to S3 | Remote Supabase (S3 bucket) | **COMPLIANT** |

---

# Section 2 - Local Intelligence Detection

Locations where the Android application performs calculations or generates intelligence:

1. **Daily Brief Text Templating** (`InsightSyncService.kt`):
   * *Local Action*: Formulates JSON arrays with summary strings by counting pending tasks, summing unpaid bill amounts, and category matching.
   * *Violation*: Violates Rule 1 (Presentation Layer Only).
2. **Financial Aggregate Calculations** (`HomeScreen.kt` / `MainActivity.kt`):
   * *Local Action*: Computes count of unpaid bills and filters transactions locally.
   * *Violation*: Offloads aggregations that should be calculated by backend pipeline tables.
3. **SMS Ingestion Date Logic** (`SmsRepository.kt`):
   * *Local Action*: Inspects local phone clock offsets to determine the cutoff timestamp for scrapes.
   * *Violation*: Risk of missing signals if device date-time is changed.

---

# Section 3 - Local Database Assessment

Classification of Room database tables:

* **`mobile_signals`**: **Queue**. Local buffer storing WhatsApp alerts and SMS messages prior to background bucket upload.
* **`todos`**: **Cache**. Downstream read replica. Write updates are posted to remote immediately.
* **`financial_events`**: **Cache**. Downstream read-only replica.
* **`fyi_events`**: **Cache**. Downstream read-only replica.
* **`user_preferences`**: **Cache**. Holds settings.
* **`daily_briefs`**: **Source of Truth (Violated)**. Room holds the master text record generated locally by the app, rather than replicating Supabase.
* **`user_actions`**: **Queue**. Temporarily stores user snooze/complete actions before posting to `user_actions` table.
* **`facts`**: **Obsolete (Stub)**. Unused model.
* **`signals`**: **Obsolete (Stub)**. Unused model.
* **`merchant_mappings`**: **Obsolete (Stub)**. Unused model.

---

# Section 4 - Repository Compliance

* **`MobileSignalRepository`** (Signal Collector): **COMPLIANT**. Acts as a temporary local queue before uploading logs.
* **`SmsRepository`** (Signal Collector): **COMPLIANT**. Ingestion scraper.
* **`TodoRepository`**: **COMPLIANT**. Uses a hybrid cache strategy (modifies Room cache and immediately sends REST update calls to Supabase).
* **`FinancialRepository`**: **COMPLIANT**. Pure read-only downstream cache.
* **`FYIRepository`**: **COMPLIANT**. Pure read-only downstream cache.
* **`ActionsRepository`**: **COMPLIANT**. Write-through queuing utility syncing actions to remote database.
* **`PreferenceRepository`**: **PARTIALLY COMPLIANT**. Reads preferences from Supabase, but fails to use them as the primary source of truth (falls back to local `SharedPreferences`).

---

# Section 5 - Screen Compliance

* **HomeScreen**: **NON-COMPLIANT**. Calculates unpaid bill statistics and handles categories locally.
* **DailyBriefScreen**: **NON-COMPLIANT**. Renders locally synthesized templates.
* **TodoScreen**: **COMPLIANT**. Acts as a pure reader and updater.
* **FyiScreen**: **COMPLIANT**. Reads downstream table.
* **FinancialScreen**: **COMPLIANT**. Reads downstream table.
* **Facts (Missing)**: **NON-COMPLIANT** (Not implemented).

---

# Section 6 - Service Compliance

* **`SyncService`**: **COMPLIANT**. Uploads raw JSON queues to storage.
* **`InsightSyncService`**: **NON-COMPLIANT**. Generates Daily Brief summaries locally rather than downloading them.
* **`JarvisNotificationListener`**: **COMPLIANT**. Collects signal notifications.
* **Background Workers**: **COMPLIANT**. They execute synchronization tasks without generating intelligence.

---

# Section 7 - Local Cache Strategy

For offline capability, the following entities **must** remain cached locally:
1. **`mobile_signals`**: Essential queue buffer for offline signal collection.
2. **`user_actions`**: Queue buffer for completed/snoozed todos when offline.
3. **`user_preferences`**: Caches basic theme/username preferences.

---

# Section 8 - Entities That Must Become Remote Only

The following entities must serve strictly as **Read-Only Caches** representing Supabase states:
1. **`daily_briefs`**: Android must only display compiled briefings retrieved from Supabase.
2. **`facts`**: Memory records must be fetched from the remote database.
3. **`todos`**: Tasks must represent remote `todo_items`.
4. **`financial_events`**: Transactions must reside on remote Supabase.
5. **`fyi_events`**: Alerts must reside on remote Supabase.

---

# Section 9 - Duplication Risk Assessment

* **Data Duplication**: The Android Room cache has duplication with Supabase records (e.g. `todos` and `financial_events` are exact replica mirrors). This is acceptable for offline functionality.
* **Logic Duplication**: The static daily brief templates in `InsightSyncService` duplicate python summarization agents. If the python agent updates its formatting, the Android app will display mismatched, outdated briefs.
* **Source of Truth Violations**: Storing the master Daily Brief text in the local Room database violates the rule that Supabase is the sole source of intelligence.

---

# Section 10 - Architecture Compliance Matrix

| Component | Status | Rationale |
| --- | --- | --- |
| **`InsightSyncService`** | **NON-COMPLIANT** | Generates Daily Brief text templates locally. |
| **`HomeScreen`** | **PARTIALLY COMPLIANT**| Performs custom filtering and counts. |
| **`PreferenceRepository`**| **PARTIALLY COMPLIANT**| Relies on local device SharedPreferences instead of remote sync. |
| **`TodoRepository`** | **COMPLIANT** | Coordinates bidirectional updates correctly. |
| **`SyncService`** | **COMPLIANT** | Clean ingestion pipeline. |

---

# Section 11 - Migration Recommendations

Recommended refactoring plan:

### 1. `InsightSyncService`
* *Current State*: Runs local aggregation routines.
* *Target State*: Fetches `"daily_briefs"` table from Supabase.
* *Migration Approach*: Remove text templates; parse JSON response directly into Room.
* *Risk Level*: **Low**.

### 2. `PreferenceRepository`
* *Current State*: Relies on local SharedPreferences.
* *Target State*: Queries remote `user_preferences`.
* *Migration Approach*: Use SharedPreferences only as a cache; pull configuration from remote on start.
* *Risk Level*: **Low**.

### 3. Facts Sync
* *Current State*: No facts integration.
* *Target State*: Pulls remote `facts` table.
* *Migration Approach*: Create `FactDao` and query facts via `JarvisInsightsClient`.
* *Risk Level*: **Low**.

---

# Conclusion & Success Criteria Answers

1. **Exactly what remains local**: Raw SMS/Notification signal buffers (`mobile_signals`) and user action queues (`user_actions`).
2. **Exactly what must come from Supabase**: Daily Brief narratives, financial transaction records, FYI alerts, long-term memory facts, and user preferences.
3. **Whether Android violates the remote-first architecture**: Yes. The local Daily Brief template generator is a direct violation of remote-first governance.
4. **Which repositories need redesign**: `PreferenceRepository` (to sync preferences) and the missing `FactsRepository`.
5. **Which entities should become cache-only**: `daily_briefs`, `facts`, `todos`, `financial_events`, and `fyi_events`.
6. **Which entities should be removed entirely**: Obsolete stubs like `SignalEntity` and `MerchantMappingEntity` can be deleted.
7. **How Android becomes a true thin client**: By removing all formatting, calculations, and aggregations from `InsightSyncService`, replacing them with REST table queries, and rendering downloaded payloads statelessy.
