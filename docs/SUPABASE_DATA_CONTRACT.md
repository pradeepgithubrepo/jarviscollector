# SUPABASE DATA CONTRACT v1.0

This document defines the authoritative data contract between the **Jarvis Android App**, the **remote Supabase database**, and the **broader Jarvis Platform Pipeline**. 

---

# Section 1 - Complete Supabase Schema Inventory

The remote database contains tables supporting three core functions: Ingestion/Consumption (Client-facing), Agent Pipeline Internals, and Financial/Facts Analytics. 

To maintain the **Presentation Layer Only** rule, the Android app is restricted to **Client-Facing** schemas and must not access or touch internal pipeline states.

---

### Supabase Table Usage & Mapping Matrix

| Supabase Table Name | Streamlit Usage | Android App Integration / Target Entity | Purpose / Synchronization Flow |
| --- | --- | --- | --- |
| **`todo_items`** (or `todos`) | Main Todos screen (Complete/Delete actions) | **`todos`** (maps to `TodoEntity` in Room DB) | **Sync Direction: Bidirectional**<br>- *Pull*: Synced to local Room DB to generate notifications & view task lists.<br>- *Push*: Actions (Complete/Snooze/Delete) updated immediately via PATCH REST requests. |
| **`fyi_events`** | FYI accordion screens (Mark Read/Delete actions) | **`fyi_events`** (maps to `FyiEventEntity` in Room DB) | **Sync Direction: Bidirectional**<br>- *Pull*: Pulled to populate category alert sub-screens.<br>- *Push*: Status toggled to `READ` or deleted via PATCH requests. |
| **`financial_events`**| Finance Overview & category spends | **`financial_events`** (maps to `FinancialEventEntity` in Room DB) | **Sync Direction: Pull Only**<br>- Synced to local Room DB to build the financial reminder summaries and snapshots. |
| **`daily_briefs`** | Renders Morning/Evening Brief cards | **`daily_briefs`** (maps to `DailyBriefEntity` in Room DB) | **Sync Direction: Pull Only**<br>- Should download LLM briefs directly instead of generating them locally. |
| **`facts`** | Renders profile entity cards (Family, Vehicles) | **`facts`** (maps to `FactEntity` in Room DB) | **Sync Direction: Pull Only**<br>- Local read-only caching for offline facts access. |
| **`user_preferences`**| Not exposed directly (inherited settings) | **`user_preferences`** (maps to `UserPreferenceEntity` in Room DB) | **Sync Direction: Pull Only**<br>- Synchronizes system configuration settings and variables (like system user profile names). |
| **`signals`** | Displays 7-day ingestion charts on dashboard | **`signals`** (maps to `SignalEntity` stub in Room DB) | **Sync Direction: Ingestion Log**<br>- Main list of all processed signals. |
| **`qualified_signals`**| KPI counts for active raw signals | N/A | **Sync Direction: Internal pipeline log**<br>- Used by Streamlit dashboard only. Android prohibited. |
| **`monthly_spending_summary`**<br>**`monthly_category_spend`**<br>**`monthly_category_trends`** | Used to calculate and draw Personal Finance KPI tiles and category spend averages | N/A | **Sync Direction: Aggregate Reports**<br>- Used to construct dashboards. Android app currently reads raw event tables directly to calculate simple aggregates. |
| **`system_status`** | System Health KPI cards | N/A | **Sync Direction: Diagnostics**<br>- Used to check pipeline run state. Android prohibited. |
| **`pipeline_runs`** | Recent pipeline runs history | N/A | **Sync Direction: Diagnostics**<br>- Used to check pipeline history. Android prohibited. |
| **`fact_relationships`**| Renders knowledge graph relationships | **`fact_relationships`** (stub class) | **Sync Direction: Optional Pull**<br>- Used to link entities (e.g. Spouse -> Child). |

---

### Ingestion Storage Buckets (File-based Ingestion)

In addition to database tables, both platforms map to:
- **`jarvis-signals` (Supabase Storage Bucket)**:
  - **Android (Push)**: The `SupabaseUploader` exports raw captured SMS/WhatsApp records as JSON files and uploads them to the `incoming/` folder inside this bucket.
  - **Streamlit**: Does not upload signals; only monitors ingestion run times.

---

# Section 2 - Detailed Schema Documentation (Client-Facing)

The detailed fields for tables accessed or cached directly by the Android app:

### 1. Table: `todos` (or `todo_items`)
* **Columns**:
  | Column Name | Data Type | Nullable | Primary Key | Purpose |
  | --- | --- | --- | --- | --- |
  | `todo_id` | UUID / String | NO | YES | Unique identifier |
  | `title` | String | YES | NO | Short task name |
  | `description` | String | YES | NO | Detail/actions |
  | `priority` | String | YES | NO | Severity (HIGH, MEDIUM, LOW) |
  | `status` | String | NO | NO | Status (OPEN, COMPLETED, SNOOZED, DISMISSED) |
  | `due_date` | String (ISO) | YES | NO | Target deadline |
  | `source_signal_id`| UUID / String | YES | NO | Origin signal reference |
  | `created_at` | String (ISO) | YES | NO | Creation time |
  | `updated_at` | String (ISO) | YES | NO | Last modified |

### 2. Table: `financial_events`
* **Columns**:
  | Column Name | Data Type | Nullable | Primary Key | Purpose |
  | --- | --- | --- | --- | --- |
  | `financial_event_id` | UUID / String | NO | YES | Unique event identifier |
  | `merchant` | String | YES | NO | Normalized merchant name |
  | `amount` | Double | YES | NO | Monetary cost |
  | `currency` | String | YES | NO | Currency unit (e.g., `"INR"`) |
  | `category` | String | YES | NO | Spend classification |
  | `status` | String | YES | NO | Settlement status (Upcoming, Settled, Refunded) |
  | `event_timestamp` | String (ISO) | YES | NO | Event date |
  | `source_signal_id`| UUID / String | YES | NO | Origin signal reference |

### 3. Table: `fyi_events`
* **Columns**:
  | Column Name | Data Type | Nullable | Primary Key | Purpose |
  | --- | --- | --- | --- | --- |
  | `fyi_event_id` | UUID / String | NO | YES | Unique notification identifier |
  | `title` | String | YES | NO | Info header |
  | `summary` | String | YES | NO | Text body |
  | `category` | String | YES | NO | Category (school, family, travel) |
  | `read_flag` | Boolean | YES | NO | Click tracker |
  | `source_signal_id`| UUID / String | YES | NO | Origin signal reference |

### 4. Table: `daily_briefs`
* **Columns**:
  | Column Name | Data Type | Nullable | Primary Key | Purpose |
  | --- | --- | --- | --- | --- |
  | `id` | String | NO | YES | ISO Date (e.g., `"2026-06-29"`) |
  | `generatedAt` | String | NO | NO | Time of compilation |
  | `version` | String | NO | NO | Formatting template version |
  | `itemsJson` | String (JSON) | NO | NO | Serialized section array |

---

# Section 3 - Android Consumption Mapping

Downstream ingestion map:
* **Ingestion (Upload)**:
  * SMS and WhatsApp alerts collected locally are saved temporarily in Room (`mobile_signals`), serialized, and uploaded to the `jarvis-signals` storage bucket in Supabase via `SyncService` / `JarvisSyncWorker`.
* **Insights (Download)**:
  * `InsightSyncService` queries tables: `todos`, `financial_events`, `fyi_events`, and `user_preferences`.
  * These are downloaded and stored in the local Room cache database (`jarvis_mobile.db`).

---

# Section 4 - Architectural Gap Analysis (Full DB)

### Why 80% of Remote Tables are Missing from the Android Contract:
1. **Pipeline Separation**: The pipeline tables (`pipeline_runs`, `understood_signals`, `qualified_signals`, `processed_files`) are run in the Python backend. Android only needs the clean, qualified end-products of this execution. Exposing internal pipeline tables directly to mobile clients breaches the **Presentation Layer Only** rule.
2. **Calculations Offloading**: Tables like `monthly_spending_summary`, `monthly_category_spend`, `salary_cycles`, and `transfer_pairs` are computed centrally. Android must not pull raw transfer pairs to determine balances; it should instead consume high-level summarized cards.
3. **Staged/Stubbed Entities**: Schema entities like `facts`, `merchant_mappings`, and `signals` exist as Kotlin models inside the codebase but are currently dead/stubbed because background sync services have not yet been wired up to query them.

---

# Section 5 - Contract Recommendations for Android V2

1. **Keep Android Clean**: Android must remain decoupled from agent run monitoring (`pipeline_runs`) and signal qualification (`qualified_signals`).
2. **Add Facts Access**: Add access to the `facts` table to enable the long-term memory experience.
3. **Clarify Todo Name Mapping**: Ensure the Python backend and Android agree on table naming (`todos` vs `todo_items`). Currently, Android maps to `todos` but remote has both definitions.
