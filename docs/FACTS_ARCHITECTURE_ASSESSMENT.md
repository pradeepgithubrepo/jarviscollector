# FACTS ARCHITECTURE ASSESSMENT v1.0

## Executive Summary & Objective

The Jarvis platform includes a validated **Fact Agent** responsible for maintaining the system's long-term memory of entities (Family members, Bank accounts, Insurance policies, Vehicles, Contacts). 

This assessment reviews how Facts are represented in the remote Supabase database, and evaluates the current state of Fact handling in the Android application. It outlines the current gaps, UI readiness, and architectural requirements to build a fully realized Facts experience.

---

# Section 1 - Facts Inventory

An audit of the Android codebase reveals that the Facts architecture is completely stubbed out.

### Fact-related Code Elements:
* **Database Table**: A local SQLite table `"facts"` is declared via Room annotations.
* **Room Entity**: [FactEntity](file:///c:/jarvis/jarviscollector/app/src/main/java/com/pradeep/jarviscollector/model/InsightEntities.kt#L74-L82) is defined in the package `com.pradeep.jarviscollector.model`.
* **Database Declarations**: `FactEntity::class` is registered in the `@Database` entity array inside [JarvisDatabase.kt](file:///c:/jarvis/jarviscollector/app/src/main/java/com/pradeep/jarviscollector/database/JarvisDatabase.kt#L27).
* **Missing Components**:
  * No Room DAO (`FactDao`) is defined.
  * No Database Accessor (`abstract fun factDao(): FactDao`) is exposed on the `JarvisDatabase` class.
  * No Repository layer (`FactsRepository`) exists.
  * No Sync Service tasks exist in `InsightSyncService` or `InsightSyncWorker`.
  * No UI Composable or Navigation route exists for displaying Facts.

---

# Section 2 - Supabase Facts Analysis

In the remote Supabase database (specifically within the `jarvis_insights_schema` profile), Facts are stored in the `facts` table, mapped to the Fact Agent.

### Schema Blueprint (`facts` Table)

| Column Name | Data Type | Nullable | Primary Key | Purpose | Example Value |
| --- | --- | --- | --- | --- | --- |
| `fact_id` | UUID / String | NO | YES | Unique Fact record key | `"fact-58a2-bb"` |
| `entity` | String | YES | NO | Entity Category / Subject | `"Honda Civic"` |
| `fact` | String | YES | NO | Synthesized memory narrative | `"Insurance policy #POL-882 expires 2026-12-10"` |
| `confidence` | Double | YES | NO | Certainty probability (0.0 to 1.0) | `0.98` |
| `source_signal_id`| UUID / String | YES | NO | Reference to signal origin | `"sig-992-abc"` |
| `created_at` | String (ISO) | YES | NO | Time fact was extracted | `"2026-06-29T10:15:00Z"` |

* **Relationships**: Linked to `fact_relationships` table (storing edges like Spouse -> Child) and `financial_facts`.
* **Sample Record**:
  `{"fact_id":"f1", "entity":"Wife", "fact":"Name is Anjali, phone is +91-98765-43210", "confidence":1.0}`

---

# Section 3 - Android Facts Consumption

* **Download Facts**: **No**. `InsightSyncService` has no logic to call `JarvisInsightsClient.fetchTable("facts")`.
* **Cache Facts**: **No**. While Room knows about the `facts` table, it does not write data because there are no DAOs or sync workers.
* **Display Facts**: **No**. There are no Fact screens; only category FYIs (which are informational alerts, not durable facts).
* **Update Facts**: **No**. User correction feedback loops for Facts are completely missing.

---

# Section 4 - Existing FactEntity Analysis

The stubbed `FactEntity` in the codebase acts as an obsolete placeholder.

### Current `FactEntity` Status: **UNUSED**

* **Why it exists / Original purpose**: Placed during the database bootstrapping phase as a template for mapping downstream Jarvis memory tables.
* **Safe removal or future reuse**:
  > [!TIP]
  > **Reclassification: KEEP & MODIFY**
  > Do not remove `FactEntity`. It should be retained and modified to support the upcoming V2 Fact syncing implementation. A corresponding `FactDao` must be created.

---

# Section 5 - Fact Agent Alignment

The Jarvis backend pipeline compiles durable facts centrally. However, **none of the Fact Agent outputs are visible to Android**. 

* **Currently Visible**: None.
* **Missing**: 100% of all generated facts are inaccessible on the mobile application due to the missing sync pipeline.

---

# Section 6 - Fact Categories

The backend pipeline organizes facts into clean classifications. The current local stub has no category columns, but the target experience requires filtering by:
* **Family** (Spouse name, kids' details)
* **Financial** (Connected banks, account numbers)
* **Insurance** (Vehicle policy numbers, health policies)
* **Vehicles** (Model name, registration numbers)
* **Contacts** (Key phone numbers/emails)
* **Accounts** (Provider login identifiers, excluding secrets)

---

# Section 7 - Facts UI Readiness

Assessment of the Android presentation capability:

> [!WARNING]
> **Status: NOT READY**
> Exposing facts requires creating:
> 1. `FactsScreen.kt` featuring categorized card grids.
> 2. `MemoryViewer` to browse chronological updates.
> 3. Profile cards (e.g. Vehicle cards displaying policy numbers, registration numbers).
> 4. Navigation hooks to access details.

---

# Section 8 - Home Dashboard Integration

In the future experience, Facts should enhance the Home Dashboard with:
1. **Fact Count**: Total stored memories (e.g. "You have 12 saved facts").
2. **Memory Highlights**: Small card carousel summarizing key details (e.g., "Wife's birthday is tomorrow").
3. **Policy Summary**: Highlights active policies and vehicle coverages.

* **Current Status**: Unsupported. No queries exist to get counts or summaries.

---

# Section 9 - Gap Analysis

The architectural gaps preventing the Facts experience are:
1. **Missing Room DAO**: No `FactDao` interface.
2. **Missing Room Accessor**: `JarvisDatabase` lacks abstract `factDao()` handler.
3. **Missing Sync Logic**: `InsightSyncService` lacks fetch logic for the `facts` table.
4. **Missing Repository**: No `FactsRepository` class.
5. **Missing UI Screens**: No screen displays the long-term memory store.

---

# Section 10 - Recommendation Matrix

To build the Facts experience, the following refactoring actions must be scheduled:

| Component | Current Status | Recommendation | Action |
| --- | --- | --- | --- |
| **`FactEntity`** | Unused Stub | **MODIFY** | Retain schema; add category columns if needed. |
| **`FactDao`** | Missing | **NEW** | Create interface with `@Insert`, `@Query`, and `@Delete` methods. |
| **`JarvisDatabase`** | Incomplete | **MODIFY** | Add `abstract fun factDao(): FactDao`. |
| **`InsightSyncService`** | Missing Fact Pull | **MODIFY** | Append `JarvisInsightsClient.fetchTable("facts")` sync task. |
| **`FactsRepository`** | Missing | **NEW** | Create repository wrapper fetching facts cached in Room. |
| **`FactsScreen`** | Missing | **NEW** | Build Compose view layout displaying memory cards. |

---

# Conclusion & Success Criteria Answers

1. **Whether Facts already exist in Supabase**: Yes, compiled centrally by the Python Fact Agent.
2. **Whether Android currently consumes Facts**: No. It is not downloaded or read.
3. **Whether the existing FactEntity is reusable**: Yes, it forms the base schema model.
4. **What is required to build the Facts experience**: A new Room `FactDao`, database accessor hook, a REST API fetch inside `InsightSyncService`, a `FactsRepository`, and a `FactsScreen` Compose UI.
5. **Whether Facts can be integrated into Home Dashboard**: Yes, by exposing a count of facts and displaying key memory card highlights.
6. **Whether Android is aligned with the validated Fact Agent architecture**: No. Android is currently blind to the Fact Agent's memory store.
