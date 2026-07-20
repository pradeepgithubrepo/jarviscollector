# JARVIS Android — Comprehensive Assessment Report
> **Produced**: 2026-07-17 | **DB Version**: 6 | **Schema**: `jarvis_insights_schema` | **Build**: Passing

---

## Table of Contents
1. [Executive Summary](#1-executive-summary)
2. [Current Screen Inventory & Analysis](#2-current-screen-inventory--analysis)
3. [Navigation Architecture Analysis](#3-navigation-architecture-analysis)
4. [Widget & Component Inventory](#4-widget--component-inventory)
5. [API & Network Layer Analysis](#5-api--network-layer-analysis)
6. [Room & Model Layer Analysis](#6-room--model-layer-analysis)
7. [Supabase Schema Mapping Report](#7-supabase-schema-mapping-report)
8. [Table Classification Matrix](#8-table-classification-matrix)
9. [Screen-by-Screen Deep Dive](#9-screen-by-screen-deep-dive)
10. [V2 Information Architecture Proposal](#10-v2-information-architecture-proposal)
11. [Schema Leverage Opportunities](#11-schema-leverage-opportunities)
12. [Implementation Priority Roadmap](#12-implementation-priority-roadmap)

---

## 1. Executive Summary

JARVIS Collector is a Kotlin/Compose Android thin-client backed by a Supabase database (`jarvis_insights_schema`). Its three roles are:
- **Signal Collector** — ingests SMS/WhatsApp notifications and queues them for upload.
- **Thin Client** — downloads AI-generated intelligence (Todos, Facts, FYI, Financial events, Daily Briefs) and caches in Room.
- **Notification Surface** — delivers priority notifications and Todo reminders via WorkManager.

### Overall Health Scorecard

| Dimension | Score | Assessment |
|---|---|---|
| Architecture Compliance (thin-client law) | 🟡 88% | Minor heuristic income classification remains on-device |
| Navigation Structure | 🟡 75% | Bottom bar exists; 2 detail screens still placeholder; Brief tab routes to old screen |
| Data Sync Completeness | 🟢 92% | 9 Supabase tables synced; 5+ available schema tables not consumed |
| Screen Completeness | 🟡 60% | 2 detail screens placeholder; DailyBrief legacy; Profile route mismapped |
| Schema Alignment | 🟡 70% | Partial field mismatches; 6 Supabase tables unused by Android |
| Code Quality | 🟢 85% | Clean separation; some prop-drilling through JarvisNavHost |

---

## 2. Current Screen Inventory & Analysis

### 2.1 Screen Registry

| # | Screen | File | Route | ViewModel | Status | Issues |
|---|---|---|---|---|---|---|
| 1 | **Home Dashboard** | `HomeScreen.kt` | `home` | `HomeDashboardViewModel` | ✅ Complete | Mild prop-drilling |
| 2 | **Tasks** | `TodoScreen.kt` | `tasks` | None (inline) | ✅ Complete | No dedicated VM; no Task Detail |
| 3 | **Task Detail** | `JarvisNavigation.kt` (inline) | `task_detail/{id}` | None | ⛔ Placeholder | Text placeholder only |
| 4 | **Facts** | `facts/FactsScreen.kt` | `facts` | `FactViewModel` | ✅ Complete | No Fact Detail screen |
| 5 | **Fact Detail** | `JarvisNavigation.kt` (inline) | `fact_detail/{id}` | None | ⛔ Placeholder | Text placeholder only |
| 6 | **FYI Overview** | `FyiScreen.kt` | `fyi` | None | ✅ Complete | — |
| 7 | **FYI Category** | `FyiCategoryScreen.kt` | `fyi_category/{category}` | None | ✅ Complete | — |
| 8 | **Financial Dashboard** | `FinancialScreen.kt` | `finance` | `FinancialDashboardViewModel` + `FinancialInsightViewModel` | ✅ Complete | Income classification via string heuristic |
| 9 | **Transaction Detail** | `financial/TransactionDetailScreen.kt` | `transaction_detail/{id}` | `TransactionDetailViewModel` | ✅ Complete | — |
| 10 | **Daily Brief** | `DailyBriefScreen.kt` | `brief` | None | ⚠️ Legacy | No VM; receives no data; legacy layout |
| 11 | **Profile** | `NotificationScreen.kt` (routed to Profile!) | `profile` | None | ⚠️ Wrong mapping | `Profile` route renders `NotificationScreen`, not a profile |
| 12 | **Notification Center** | `notification/NotificationCenterScreen.kt` | `notification_center` | None | ⚠️ Partial | Not actionable; no deep-link routing |
| 13 | **Action Center** | `actioncenter/ActionCenterScreen.kt` | `action_center` | None | Basic | Shows `user_actions` log |
| 14 | **Signal Explorer** | `signalexplorer/SignalExplorerScreen.kt` | `signal_explorer/{type}/{id}` | None | Debug | Dev tool |
| 15 | **Debug Pipeline** | `debug/DebugDataPipelineScreen.kt` | `debug_pipeline` | None | Debug | Dev tool |
| 16 | **Splash** | `ui/splash/SplashScreen.kt` | `splash` | None | ✅ Complete | — |
| 17 | **Name Selection** | `ui/name_selection/NameSelectionScreen.kt` | `name_selection` | None | ✅ Complete | — |

### 2.2 Bottom Navigation Bar Tabs

```
[ Home ] | [ Tasks ] | [ Facts ] | [ Finance ] | [ Profile ]
```
- The `Brief` route/screen object still exists in `Screen` sealed class but is **not a bottom nav tab**.
- `Profile` tab navigates to `NotificationScreen` — a critical mapping error.

---

## 3. Navigation Architecture Analysis

### 3.1 Navigation Graph (Current)

```
Splash ──→ NameSelection ──→ Home
                              │
           ┌──────────────────┼──────────────────────┐
           ▼                  ▼                        ▼
          Tasks             Facts                  Finance
           │                  │                        │
      task_detail/{id}   fact_detail/{id}    transaction_detail/{id}
      [⛔ PLACEHOLDER]   [⛔ PLACEHOLDER]         [✅ REAL SCREEN]
           
           │                  │                        │
           ▼                  ▼                        ▼
          FYI             Brief (Legacy)          Profile
           │                                      [⚠️ Actually NotificationScreen]
      fyi_category/{cat}
           
           │
      notification_center
      action_center
      signal_explorer/{type}/{id}   [debug]
      debug_pipeline                 [debug]
```

### 3.2 Navigation Issues

| # | Issue | Severity | Impact |
|---|---|---|---|
| N-1 | `Profile` route renders `NotificationScreen`, not a profile screen | **Critical** | Users see sync controls when tapping Profile tab |
| N-2 | `task_detail/{id}` is a centered Text placeholder | **High** | Tasks cannot be drilled into |
| N-3 | `fact_detail/{id}` is a centered Text placeholder | **High** | Facts cannot be drilled into |
| N-4 | `DailyBriefScreen` receives no data — renders empty | **High** | Daily Brief tab is effectively broken |
| N-5 | `Screen.Brief` object registered in sealed class but not in bottom bar | **Medium** | Dead route reference; accessible from Home card only |
| N-6 | `JarvisNavHost` receives 30+ parameters — extreme prop drilling | **Medium** | Maintainability risk; violates MVVM separation |
| N-7 | `notification/{id}` route renders inline placeholder Text | **Low** | Notification detail is unimplemented |

### 3.3 Navigation Strengths

- ✅ Jetpack Navigation Compose implemented with `NavHostController`.
- ✅ `launchSingleTop = true` and `restoreState = true` on all bottom tab clicks.
- ✅ `popUpTo` with `saveState` prevents stack duplication.
- ✅ Parameterized `FyiCategoryScreen` consolidates 5 legacy category screens.
- ✅ `TransactionDetail` fully wired with ViewModel.
- ✅ Deep-link routing from `NotificationCenterScreen` implemented (maps route strings).

---

## 4. Widget & Component Inventory

### 4.1 Reusable Components

| Component | File | Used By | Status |
|---|---|---|---|
| `FactCard` | `ui/facts/FactCard.kt` | `FactsScreen` | ✅ Reusable |
| `FinancialActionCard` | `ui/financial/FinancialActionCard.kt` | Legacy | ⚠️ Unused in new UI |
| `AgentFramework` composables | `ui/AgentFramework.kt` | Multiple screens | ✅ Utility composables |
| `BottomNavigationBar` | `navigation/BottomNavigationBar.kt` | `MainActivity` | ✅ Working |

### 4.2 Missing Shared Components (Referenced in V2 Spec)

| Component | Spec Name | Priority |
|---|---|---|
| Base card container | `JarvisCard` | HIGH |
| Reusable top bar | `JarvisHeader` | HIGH |
| Empty state view | `JarvisEmptyState` | MEDIUM |
| Priority badge chip | `JarvisPriorityBadge` | MEDIUM |
| Metric tile | `JarvisMetricTile` | HIGH |

### 4.3 ViewModels

| ViewModel | Pattern | Strength | Weakness |
|---|---|---|---|
| `HomeDashboardViewModel` | AndroidViewModel + init flow | Observes 3 flows | Pulls full lists; local filtering |
| `FactViewModel` | stateIn wrapping DAO Flow | Simple, clean | Flat list only |
| `FinancialDashboardViewModel` | init observeFlow | Full aggregation | **Income classification heuristic** (string matching) |
| `FinancialInsightViewModel` | 5x stateIn flows | Well segmented | — |
| `TransactionDetailViewModel` | Single entity load | Clean pattern | — |
| *(Missing)* `DailyBriefViewModel` | — | — | Does not exist |
| *(Missing)* `TodoDetailViewModel` | — | — | Does not exist |
| *(Missing)* `FactDetailViewModel` | — | — | Does not exist |
| *(Missing)* `ProfileViewModel` | — | — | Does not exist |

---

## 5. API & Network Layer Analysis

### 5.1 Client Architecture

```
JarvisInsightsClient (OkHttp3)
├── GET  /rest/v1/{tableName}   → Accept-Profile: jarvis_insights_schema
├── PATCH /rest/v1/{tableName}  → Content-Profile: jarvis_insights_schema
└── POST  /rest/v1/{tableName}  → Content-Profile: jarvis_insights_schema

JarvisApiClient (OkHttp3)
└── POST  Supabase Storage bucket (jarvis-signals/incoming)

SupabaseUploader
└── Batch JSON upload for mobile signals
```

### 5.2 Tables Currently Fetched by `InsightSyncService`

| Table Fetched | Maps To | Status |
|---|---|---|
| `todo_items` | `todos` (Room) | ✅ Working |
| `financial_events` | `financial_events` (Room) | ✅ Working |
| `fyi_events` | `fyi_events` (Room) | ✅ Working |
| `user_preferences` | `user_preferences` (Room) | ✅ Working |
| `daily_briefs` | `daily_briefs` (Room) | ✅ Working |
| `facts` | `facts` (Room) | ✅ Working |
| `user_actions` | `user_actions` (Room) | ✅ Working |
| `financial_facts` | `financial_insights` (Room) | ✅ Working |
| ~~`notifications`~~ | — | ⚠️ Explicitly set to null; local only |

### 5.3 Write Paths

| Repository | Supabase Target Table | Write Method | Remote-First? |
|---|---|---|---|
| `TodoRepository` | `todo_items` | PATCH status, DELETE | ✅ Yes |
| `FinancialRepository` | `financial_events` | PATCH category/confirm | ✅ Yes |
| `FinancialInsightRepository` | `financial_facts` | PATCH status | ✅ Yes |
| `FYIRepository` | `fyi_events` | PATCH read_flag/status | ✅ Yes |
| `ActionsRepository` | `user_actions` | POST new action | ✅ Yes |
| `PreferenceRepository` | `user_preferences` | UPSERT | ✅ Yes |
| `FactRepository` | `facts` | PATCH read_flag | ⛔ **BROKEN** — `facts` has no `read_flag` on Supabase |

---

## 6. Room & Model Layer Analysis

### 6.1 Room DB Entity Inventory

| # | Entity | Table | PK | Fields | Purpose | Health |
|---|---|---|---|---|---|---|
| 1 | `MobileSignal` | `mobile_signals` | Int (AutoGen) | varies | SMS upload queue | ✅ Active |
| 2 | `TodoEntity` | `todos` | String | 9 | Task cache | ✅ Active |
| 3 | `FinancialEventEntity` | `financial_events` | String | 10 | Transaction cache | ✅ Active |
| 4 | `FyiEventEntity` | `fyi_events` | String | 9 | Alert cache | ✅ Active |
| 5 | `UserPreferenceEntity` | `user_preferences` | String | 3 | Config cache | ✅ Active |
| 6 | `UserActionEntity` | `user_actions` | String | 6 | Action queue/log | ✅ Active |
| 7 | `DailyBriefEntity` | `daily_briefs` | String | 9 | Brief cache | ✅ Entity complete; screen legacy |
| 8 | `FactInsightEntity` | `facts` | String | 9 | Facts cache | ✅ Active |
| 9 | `NotificationEntity` | `notifications` | String | 10 | Local notification log | ⚠️ Local only; not synced |
| 10 | `FinancialInsightEntity` | `financial_insights` | String | 10 | Financial facts cache | ✅ Active |
| 11 | `SyncDiagnosticsEntity` | `sync_diagnostics` | String | 7 | Sync health tracking | ✅ Active |

**DB Version**: 6 | **Migration strategy**: `fallbackToDestructiveMigration()` ⚠️

### 6.2 Field Mapping Issues

| Entity | Android Field | Supabase Field | Issue |
|---|---|---|---|
| `FactInsightEntity` | `read_flag` | *(missing)* | `read_flag` does not exist on Supabase `facts` table — PATCH will fail silently |
| `FinancialEventEntity` | `merchant` | `title` | Renaming documented; mapping works via multi-field fallback |
| `FinancialEventEntity` | `event_timestamp` | `event_date` | Handled in sync mapper |
| `FinancialInsightEntity` | `dueDate` | `event_date` | Handled in sync mapper |
| `DailyBriefEntity` | `briefType` | `brief_type` | Now mapped; was missing in v5 |

### 6.3 `DailyBriefEntity` — Extended Schema (v6)

```kotlin
data class DailyBriefEntity(
    val id: String,           // brief_id or date string
    val generatedAt: String,  // generated_at
    val version: String,
    val itemsJson: String,    // content array JSON
    val briefType: String?,   // "MORNING" or "EVENING" — NEW in v6
    val todoCount: Int?,      // todo_count — NEW in v6
    val fyiCount: Int?,       // fyi_count — NEW in v6
    val factCount: Int?,      // fact_count — NEW in v6
    val payloadJson: String?  // payload_json — NEW in v6
)
```

---

## 7. Supabase Schema Mapping Report

### 7.1 Tables Currently Used by Android

| Supabase Table | Sync Direction | Android Room Table | Status |
|---|---|---|---|
| `todo_items` | Pull + Push (PATCH/DELETE) | `todos` | ✅ Bidirectional |
| `financial_events` | Pull only | `financial_events` | ✅ Read |
| `fyi_events` | Pull + Push (PATCH read/dismiss) | `fyi_events` | ✅ Bidirectional |
| `daily_briefs` | Pull only | `daily_briefs` | ✅ Read |
| `facts` | Pull + Broken PATCH | `facts` | ⚠️ Broken write path |
| `user_preferences` | Pull + Push | `user_preferences` | ✅ Bidirectional |
| `user_actions` | Pull + Push (POST) | `user_actions` | ✅ Bidirectional |
| `financial_facts` | Pull only | `financial_insights` | ✅ Read |

### 7.2 Tables NOT Used by Android (Available in Supabase)

| Supabase Table | Purpose | Android Status | Recommended Action |
|---|---|---|---|
| `signals` | Raw processed signal log | ❌ Not synced | Add to `SignalExplorer` / Debug only |
| `qualified_signals` | Pipeline qualification log | ❌ Not synced | **Do not expose** — pipeline internal |
| `monthly_spending_summary` | Pre-aggregated monthly totals | ❌ Not synced | **HIGH VALUE** — replace ViewModel heuristic aggregation |
| `monthly_category_spend` | Category spend per month | ❌ Not synced | **HIGH VALUE** — replace local category aggregation |
| `monthly_category_trends` | Category trend over time | ❌ Not synced | **HIGH VALUE** — power the monthly trend chart |
| `fact_relationships` | Entity graph (person → vehicle, etc.) | ❌ Not synced | Consider for Facts graph view |
| `system_status` | Pipeline health KPIs | ❌ Not synced | **Do not expose** — admin only |
| `pipeline_runs` | Run history | ❌ Not synced | **Do not expose** — admin only |
| `merchant_mappings` | Merchant canonical names | ❌ Not synced | Useful for Financial display corrections |

### 7.3 Tables Expected by App but Missing/Broken on Supabase

| Android Entity | Expected Supabase Field | Reality | Impact |
|---|---|---|---|
| `FactInsightEntity.read_flag` | `read_flag` column on `facts` | Column does not exist | Silent PATCH failure; read state only local |
| `DailyBriefEntity.briefType` | `brief_type` column on `daily_briefs` | May be absent in older rows | `briefType` arrives as null for old records |
| `FinancialEventEntity.created_at` / `updated_at` | Both mapped to `created_at` | `updated_at` incorrectly mapped | Incorrect `updated_at` shown in detail |

### 7.4 New Tables Available But Not Surfaced

```
┌─────────────────────────────────────────────────────────────────┐
│  HIGH PRIORITY — Consumer-facing, safe to sync                  │
├─────────────────────────────────────────────────────────────────┤
│  monthly_spending_summary   → Powers Financial Overview cards   │
│  monthly_category_spend     → Powers Category drill-down        │
│  monthly_category_trends    → Powers 6-month trend chart        │
├─────────────────────────────────────────────────────────────────┤
│  MEDIUM PRIORITY — Enhances existing features                   │
├─────────────────────────────────────────────────────────────────┤
│  fact_relationships         → Enables Facts knowledge graph     │
│  merchant_mappings          → Canonical merchant name lookup    │
└─────────────────────────────────────────────────────────────────┘
```

### 7.5 Broken Relationships

| Relationship | Expected | Actual | Severity |
|---|---|---|---|
| `FactInsightEntity.read_flag` → `facts.read_flag` | Column exists on Supabase | Column absent on Supabase | **Critical** — silent failure |
| `FinancialEventEntity.updated_at` → `financial_events.updated_at` | Separate field | Mapped same as `created_at` in sync code | **Medium** — data fidelity issue |
| `FinancialInsightEntity` → `financial_facts.priority` | Priority from Supabase | Hard-coded to `"MEDIUM"` in sync | **Medium** — priority always wrong |
| `FinancialEventEntity.financial_event_id` → `financial_events.id` | Column named `id` on Supabase | Mapped via `optString("id", optString("financial_event_id"))` | ✅ Handled defensively |

### 7.6 Duplicate Concepts

| Concept | Entity A | Entity B | Resolution |
|---|---|---|---|
| Transaction data | `FinancialEventEntity` (raw events) | `FinancialInsightEntity` (classified facts) | Both valid — different levels; keep both |
| Notification surface | `NotificationEntity` (local) | `fyi_events` (Supabase FYI) | FYI is Supabase authoritative; NotificationEntity is local only — clarify |
| Read state | `FactInsightEntity.read_flag` (Room) | `facts.status == "ACKNOWLEDGED"` (Supabase) | `read_flag` in sync correctly derives from `status == "ACKNOWLEDGED"` — acceptable; but PATCH is still broken |
| Source of signals | `MobileSignal` (Room queue) | `signals` (Supabase) | Correct — upload queue → Supabase |

---

## 8. Table Classification Matrix

### 8.1 Tasks Domain

| Table | Direction | Role |
|---|---|---|
| `todo_items` (`todos`) | Bidirectional | Core — user actionable tasks |
| `user_actions` | Push (write actions log) | Supporting — records Complete/Snooze |

### 8.2 Financial Domain

| Table | Direction | Role |
|---|---|---|
| `financial_events` | Pull | Core — raw transaction ledger |
| `financial_facts` (`financial_insights`) | Pull | Core — AI-classified financial alerts |
| `monthly_spending_summary` | Pull (not yet synced) | 🆕 HIGH VALUE — pre-aggregated totals |
| `monthly_category_spend` | Pull (not yet synced) | 🆕 HIGH VALUE — category breakdown |
| `monthly_category_trends` | Pull (not yet synced) | 🆕 HIGH VALUE — trend time-series |
| `merchant_mappings` | Pull (not yet synced) | 🆕 MEDIUM — canonical name display |

### 8.3 Facts Domain

| Table | Direction | Role |
|---|---|---|
| `facts` | Pull + Broken PATCH | Core — long-term memory |
| `fact_relationships` | Pull (not yet synced) | 🆕 MEDIUM — entity graph connections |

### 8.4 System Domain

| Table | Direction | Role |
|---|---|---|
| `user_preferences` | Pull + Push | System configuration |
| `mobile_signals` | Upload queue | Signal ingestion queue |
| `signals` | Read-only (not yet synced) | Debug/Signal Explorer only |
| `daily_briefs` | Pull | AI morning/evening brief |
| `fyi_events` | Bidirectional | Informational alerts |
| `sync_diagnostics` | Local write | Sync health tracking |
| `notifications` | Local only | Local notification log |
| `pipeline_runs` | **Prohibited** | Admin — do not expose |
| `qualified_signals` | **Prohibited** | Pipeline internal — do not expose |
| `system_status` | **Prohibited** | Admin — do not expose |

---

## 9. Screen-by-Screen Deep Dive

### 9.1 Home Dashboard (`HomeScreen.kt`)

| Dimension | Detail |
|---|---|
| **Purpose** | Command center — KPI tiles, summary card lists, quick actions |
| **Data Sources** | `HomeDashboardViewModel` → `TodoRepository`, `FactRepository`, `FinancialEventDao` |
| **Current Metrics** | Task count, Fact count, Financial count, Alert count (high priority + snoozed tasks) |
| **Summary Cards** | Today's Tasks (top 3), Latest Facts (top 3), Upcoming Events (top 3 by due date) |

**Problems:**
- Income classification is done via string heuristics in `FinancialDashboardViewModel` — violates thin-client law minimally but is fragile.
- Daily Brief preview tile navigates to a broken screen.
- `alertCount` counts HIGH priority + SNOOZED tasks — not a true notification count; misleading label.
- Prop-drilling: `HomeScreen` receives 20+ parameters, making composition unwieldy.
- `recentFacts` filters by `read_flag != true` locally — acceptable but means unread facts only.

**Recommended Redesign:**
- Replace inline metric aggregation with authoritative `monthly_spending_summary` data from Supabase.
- Rename "Alerts" tile to "Overdue" or tie it to a proper notification count.
- Daily Brief preview card should show first line of brief with generated timestamp.
- Extract `HomeDashboardViewModel` aggregation into proper Room `@Query` with indices.
- Move prop-drilling into a `HomeDashboardUiState` that is self-contained.

---

### 9.2 Tasks Screen (`TodoScreen.kt`)

| Dimension | Detail |
|---|---|
| **Purpose** | List view of actionable todo items with Complete/Snooze/Delete actions |
| **Data Sources** | `todos` table in Room via `TodoRepository` |
| **Actions** | Complete → PATCH Supabase `todo_items`; Snooze → PATCH; Delete → DELETE |
| **Tabs** | Open / Completed |

**Problems:**
- No dedicated ViewModel — receives `todos: List<TodoEntity>` as a parameter; filtering done inside the composable.
- Task Detail screen is a placeholder — tapping a task shows only an ID.
- No priority-based visual differentiation (no color coding beyond text labels).
- No source signal trace link on the list view.

**Recommended Redesign:**
- Create `TodoViewModel` owning its own `Flow<List<TodoEntity>>` from Room.
- Build `TaskDetailScreen` with full fields (title, description, priority badge, source signal link, status timeline).
- Add swipe-to-complete gesture using `SwipeToDismiss` composable.
- Color-code priority: HIGH = red, MEDIUM = amber, LOW = muted.

---

### 9.3 Facts Screen (`facts/FactsScreen.kt`)

| Dimension | Detail |
|---|---|
| **Purpose** | Display long-term memory facts; toggle read state |
| **Data Sources** | `facts` table via `FactViewModel` / `FactRepository` |
| **Actions** | Toggle read flag (PATCH to Supabase — **currently broken**) |

**Problems:**
- `FactRepository.markFactRead()` sends `read_flag` PATCH to Supabase `facts` table which does not have that column — **silent failure**.
- Fact Detail is a placeholder.
- Facts displayed as a flat list — no category grouping (Family, Vehicle, Insurance, etc.).
- No fact count by category on the Facts screen entry point.

**Recommended Redesign:**
- **Fix write path**: PATCH `status = "ACKNOWLEDGED"` instead of `read_flag = true`.
- Build `FactDetailScreen` with full fact context, source agent, category, created date.
- Group facts by category using `LazyColumn` with sticky headers.
- Add category summary tiles at the top (count by Family, Vehicle, Insurance, etc.).
- Integrate `fact_relationships` to show entity links (e.g., Person → Vehicle).

---

### 9.4 Financial Dashboard (`FinancialScreen.kt`)

| Dimension | Detail |
|---|---|
| **Purpose** | Transaction overview with filters, category breakdown, insights, monthly trend |
| **Data Sources** | `financial_events` (Room) + `financial_facts` (Room) via `FinancialDashboardViewModel` + `FinancialInsightViewModel` |
| **Sections** | Income/Expense/Savings/Records tiles; Top categories; Recent transactions (10); Monthly trend (6mo); Financial insights |

**Problems:**
- **Income classification heuristic**: Income = title contains `"received"/"refund"/"credit"/"deposit"/"income"/"salary"` — fragile string matching violates thin-client rule. Supabase should classify debit/credit.
- `monthly_spending_summary`, `monthly_category_spend`, `monthly_category_trends` tables exist on Supabase and are pre-aggregated but **not consumed**.
- `FinancialInsightEntity.priority` is **hardcoded to "MEDIUM"** in sync — alerts never escalate.
- No drill-down from category → transactions list.
- `updated_at` on `FinancialEventEntity` is incorrectly mapped to `created_at`.

**Recommended Redesign:**
- Sync `monthly_spending_summary` → replace local income/expense aggregation entirely.
- Sync `monthly_category_spend` → replace local `categoryBreakdown` computation.
- Sync `monthly_category_trends` → replace local `monthlyTrend` construction.
- Fix `priority` mapping from `financial_facts.priority` field.
- Implement Category → Transactions drilldown route.

---

### 9.5 FYI Screen (`FyiScreen.kt`) + FYI Category (`FyiCategoryScreen.kt`)

| Dimension | Detail |
|---|---|
| **Purpose** | Informational alerts by category |
| **Data Sources** | `fyi_events` (Room) via `FYIRepository` |
| **Categories** | Family, Health, Shopping/Deliveries, Travel, School |

**Problems:**
- FYI events have no urgency/expiry concept — older events accumulate with no archival.
- No notification badge count on the FYI bottom tab.
- Category filter is case-insensitive string match — `"deliveries"` aliased to `"shopping"` only in NavHost, not inside the screen.

**Recommended Redesign:**
- Add badge count to FYI bottom tab (unread count).
- Add `status` filter to show only `NEW`/`READ` (not `DISMISSED`).
- Add auto-archive for events older than 30 days.

---

### 9.6 Daily Brief Screen (`DailyBriefScreen.kt`)

| Dimension | Detail |
|---|---|
| **Purpose** | Morning command center showing AI-generated brief |
| **Data Sources** | None currently — **receives no data** |
| **Room Entity** | `DailyBriefEntity` with `itemsJson`, `briefType`, `todoCount`, `fyiCount`, `factCount` |

**Problems:**
- `DailyBriefScreen` is bound in `JarvisNavigation.kt` with only `onBack` — no data is passed.
- No `DailyBriefViewModel` exists — the entity has all the data but the screen is disconnected.
- Brief content (`itemsJson`) is a raw JSON array string that needs parsing.
- No morning notification triggering the Brief at 7–8 AM.
- No differentiation between MORNING and EVENING briefs.
- `DailyBriefEntity` v6 has `briefType`, `todoCount`, `fyiCount`, `factCount`, `payloadJson` fields but the screen uses none of them.

**Recommended Redesign:**
- Create `DailyBriefViewModel` (AndroidViewModel) with `getLatestFlow()` from `DailyBriefDao`.
- Parse `itemsJson` → `List<String>` brief sections.
- Use `payloadJson` for rich card sections (tasks preview, financial snapshot, fact highlights).
- Show MORNING vs EVENING variant with different header theming.
- Add to `HomeDashboardViewModel`: show brief preview card with first 2 lines.
- Add `DailyBriefNotificationWorker` at 7:30 AM.

---

### 9.7 Profile / Settings Screen (`NotificationScreen.kt`)

| Dimension | Detail |
|---|---|
| **Purpose** | **BUG**: Renders sync controls, signal logs, and backfill tools under the Profile tab |
| **Data Sources** | `mobile_signals` (Room), `user_preferences` (Room), `notifications` (local) |

**Problems:**
- **Critical mismatch**: The `Profile` route renders `NotificationScreen` — a developer sync/debug tool, not a user profile.
- No actual user profile screen (name, preferences display, notification settings).
- Contains `onStartBackfill` and raw signal logs — should be dev/debug only.
- Profile content not personalized with user's name or preferences.

**Recommended Redesign:**
- Create `ProfileScreen` with: user greeting, preference toggles (notification time, sync frequency), about section.
- Move sync/backfill tools to `DebugPipeline` screen (already exists) or a hidden dev menu.
- Expose `user_preferences` table values as editable toggles.

---

### 9.8 Notification Center (`NotificationCenterScreen.kt`)

| Dimension | Detail |
|---|---|
| **Purpose** | List of local `NotificationEntity` records |
| **Data Sources** | `notifications` (Room local) via `NotificationCenterRepository` |
| **Actions** | Mark read, Archive, Navigate to route |

**Problems:**
- `NotificationEntity` is purely local — not synced from Supabase.
- Notifications are only generated from `FinancialInsightEntity` HIGH priority items (in sync service).
- No daily brief or task-due notifications routed here.
- Deep-link routing exists but only 6 route strings are supported.

**Recommended Redesign:**
- Add ToDo-due-today notifications to the local notification table.
- Add Daily Brief available notifications.
- Display unread badge count on the notification bell icon in Home top bar.
- Route notification taps directly to entity detail screens (not just screen-level routes).

---

## 10. V2 Information Architecture Proposal

### 10.1 V2 Navigation Architecture

```
Global Scaffold
├── Bottom Navigation Bar
│   ├── 🏠 Home
│   ├── ✅ ToDos
│   ├── 💰 Financial
│   ├── 🧠 Facts
│   └── 👤 Profile
│
└── Secondary (Navigational drilldowns, not in tab bar)
    ├── fyi_category/{cat}      ← from Home FYI section
    ├── task_detail/{id}        ← from ToDos or Home
    ├── fact_detail/{id}        ← from Facts
    ├── transaction_detail/{id} ← from Financial
    ├── notification_center     ← from Home bell icon
    ├── brief                   ← from Home brief card / notification
    └── signal_explorer/{t}/{id} ← debug only
```

> [!IMPORTANT]
> `FYI` is removed from the bottom navigation bar. FYI events become a card section within the Home screen, with a dedicated drilldown to the parameterized category screen. This simplifies the main navigation to the 5 highest-value destinations.

### 10.2 V2 Screen Specifications

#### 🏠 Home — Command Center

**Purpose**: Single-glance view of everything the user needs to act on today.

**Sections (top to bottom)**:
1. **Header**: Greeting (user name from `user_preferences`), date, notification bell badge, avatar.
2. **Daily Brief Preview Card**: First 2 lines of today's MORNING brief + generated time + arrow to full brief.
3. **Today's Snapshot Row** (3 chips): `N tasks open` | `₹X spent today` | `N new facts`.
4. **Quick Actions FAB row**: Add Transaction, Add ToDo, Quick Note, Voice Capture.
5. **Financial Pulse Card**: Net cashflow (Income - Expense) this month from `monthly_spending_summary`.
6. **Priority Tasks** (top 3 by priority + due date).
7. **FYI Cards** (grouped: Family, Health, Shopping, Travel — count badges per category).
8. **Latest Facts** (top 3 unread facts).

**Data Sources**: `HomeDashboardViewModel` → Room (todos, facts, financial_events, fyi_events, daily_briefs) + `monthly_spending_summary`.

---

#### ✅ ToDos — Task Hub

**Purpose**: Full task management with priority grouping and swipe actions.

**Sections**:
1. **Tab Bar**: Open | Snoozed | Completed.
2. **Priority groups**: HIGH (red) → MEDIUM (amber) → LOW (muted) with section headers.
3. **Task cards**: Title + due date + priority badge + source signal link.
4. **Swipe to complete** gesture (right swipe).
5. **Swipe to snooze** gesture (left swipe).
6. **Task Detail** screen: Full fields + source signal trace + status history.

**Data Sources**: `TodoViewModel` (new) → `TodoDao.getAllFlow()` → segmented by status + priority.

---

#### 💰 Financial — Money Dashboard

**Purpose**: Authoritative cashflow view using Supabase-aggregated data.

**Sections**:
1. **Overview Row**: Income | Expenses | Savings | Transactions (from `monthly_spending_summary`).
2. **Category Breakdown**: Top 6 categories (from `monthly_category_spend`) — tap → transaction list filtered by category.
3. **Monthly Trend**: 6-month bar chart (from `monthly_category_trends`).
4. **Financial Alerts**: HIGH priority `financial_facts` (action required, unusual activity, upcoming bills).
5. **Recent Transactions**: Tap → `TransactionDetailScreen`.

**Data Sources**: New `MonthlyFinancialViewModel` → Room (new monthly tables) + existing `financial_events`, `financial_facts`.

---

#### 🧠 Facts — Memory Dashboard

**Purpose**: Browse and manage the AI's long-term memory about the user.

**Sections**:
1. **Category Summary Row**: Family | Vehicle | Insurance | Accounts | Contacts — tap → filtered list.
2. **Facts List**: Grouped by category with sticky headers.
3. **Fact Card**: Title (fact_type) + Summary (fact_value) + Category badge + Source agent + Date.
4. **Fact Detail Screen**: Full context + `fact_relationships` links (e.g., "Part of: Family → Spouse").
5. **Mark as Read**: Updates Room only (Supabase write path fixed to PATCH `status = "ACKNOWLEDGED"`).

**Data Sources**: `FactViewModel` (extended) → `FactInsightDao.getAllFlow()` grouped by category + `fact_relationships` (new sync).

---

#### 👤 Profile — User Settings

**Purpose**: User-facing settings and preferences (not a debug tool).

**Sections**:
1. **User Card**: Name (from `user_preferences`), avatar, member since date.
2. **Notification Settings**: Morning brief time, Todo reminder time — stored in `user_preferences`.
3. **Sync Status**: Last sync times per entity (from `sync_diagnostics`).
4. **Data Privacy**: Clear local cache button, data export.
5. **App Info**: Version, build, about.
6. **Developer Tools** (hidden, 5-tap on version): → `DebugPipeline`, `SignalExplorer`, backfill controls.

**Data Sources**: `ProfileViewModel` (new) → `PreferenceRepository`, `SyncDiagnosticsDao`.

---

### 10.3 V2 Navigation Map (Detailed)

```
╔══════════════════════════════════════════════════════════════════╗
║                     BOTTOM NAVIGATION BAR                       ║
║   🏠 Home  |  ✅ ToDos  |  💰 Financial  |  🧠 Facts  |  👤 Profile  ║
╚══════════════════════════════════════════════════════════════════╝

HOME
├── Brief preview card ──────────────────────────→ brief (Full DailyBriefScreen)
├── Bell icon ───────────────────────────────────→ notification_center
├── Financial Pulse card ────────────────────────→ finance (tab)
├── Task cards (tap) ────────────────────────────→ task_detail/{id}
├── FYI Family badge ────────────────────────────→ fyi_category/family
├── FYI Health badge ────────────────────────────→ fyi_category/health
├── FYI Shopping badge ──────────────────────────→ fyi_category/shopping
├── FYI Travel badge ────────────────────────────→ fyi_category/travel
└── Fact cards (tap) ────────────────────────────→ fact_detail/{id}

TODOS
├── Task cards (tap) ────────────────────────────→ task_detail/{id}
│     └── Source signal link ───────────────────→ signal_explorer/{type}/{id}
└── Filter tabs: Open | Snoozed | Completed

FINANCIAL
├── Category cards (tap) ───────────────────────→ finance_category/{cat}   [NEW]
├── Transaction rows (tap) ─────────────────────→ transaction_detail/{id}
│     └── Source signal link ───────────────────→ signal_explorer/{type}/{id}
└── Alert cards (tap) ──────────────────────────→ insight_detail/{id}      [NEW]

FACTS
├── Category tiles (tap) ───────────────────────→ fact_category/{cat}      [NEW]
└── Fact cards (tap) ────────────────────────────→ fact_detail/{id}

PROFILE
└── Developer section (tap 5x on version) ──────→ debug_pipeline
```

---

### 10.4 Daily Brief — Standalone Screen

The `brief` route should be a full-screen morning command center, navigable from:
- Home "Today's Brief" card
- Morning notification deep-link (7:30 AM WorkManager)

**Layout**:
1. **Header**: Date, "Morning Brief" / "Evening Brief" (from `briefType`), generated time.
2. **Metrics row**: `todoCount` tasks open | `fyiCount` new alerts | `factCount` facts.
3. **Brief content cards**: One card per item in `itemsJson` array.
4. **Quick actions**: Tap to go to Tasks / Financial / Facts from within brief.

---

## 11. Schema Leverage Opportunities

The following Supabase tables are **available but not yet surfaced** in the Android app. Each represents a concrete improvement opportunity.

### 11.1 `monthly_spending_summary` → Replace ViewModel Heuristics

| Current Problem | New Capability |
|---|---|
| `FinancialDashboardViewModel` classifies income via string keyword matching | Pull pre-classified `total_income`, `total_expenses`, `net_savings` from Supabase monthly summary |
| Aggregation runs on device across potentially thousands of records | Single row per month returned from Supabase — trivial to display |

**Implementation**: Add `MonthlySpendingSummaryEntity` + DAO + sync in `InsightSyncService` + display in `FinancialDashboardViewModel`.

---

### 11.2 `monthly_category_spend` → Authoritative Category Breakdown

| Current Problem | New Capability |
|---|---|
| `categoryBreakdown` computed locally from raw events | Pull `category`, `amount`, `transaction_count` per month from Supabase |
| Local aggregation is slow; no month-over-month comparison | Show delta vs last month for each category |

---

### 11.3 `monthly_category_trends` → Authoritative Trend Chart

| Current Problem | New Capability |
|---|---|
| `monthlyTrend` is computed by grouping `financial_events` by month string in ViewModel | Pull pre-computed time-series from Supabase — accurate, authoritative |
| Trend includes all events regardless of type | Supabase separates expense/income trends |

---

### 11.4 `fact_relationships` → Facts Knowledge Graph

| Current Opportunity | Value |
|---|---|
| Facts are displayed as a flat list today | `fact_relationships` links facts to each other (e.g., Person ↔ Vehicle, Person ↔ Insurance) |
| Could render entity cards showing "related" facts | Enables a knowledge graph view in FactDetailScreen |

---

### 11.5 `facts.status = "ACKNOWLEDGED"` Fix

| Current Problem | Fix |
|---|---|
| `FactRepository.markFactRead()` PATCHes `read_flag` — column absent on Supabase | PATCH `status = "ACKNOWLEDGED"` instead |
| Silent failure means read state is only local and resets on next sync | Proper write path persists read state on Supabase |

---

### 11.6 `daily_briefs.brief_type` → Morning vs Evening Differentiation

| Current State | Opportunity |
|---|---|
| `briefType` field now synced but `DailyBriefScreen` uses none of it | Show MORNING brief card with sunrise icon; EVENING brief with moon icon |
| Home dashboard shows latest brief regardless of type | Show MORNING brief before noon, EVENING brief after 6 PM |

---

### 11.7 `daily_briefs.payload_json` → Rich Brief Cards

| Current State | Opportunity |
|---|---|
| `itemsJson` is a flat string array (each item is one text string) | `payloadJson` potentially contains structured sections (task list, financial snapshot, fact highlights) |
| Brief renders as plain text list | Render structured cards: "3 tasks due today", "₹12,400 spent", "New fact: Vehicle insurance renewal" |

---

## 12. Implementation Priority Roadmap

### Phase 1 — Critical Fixes (Week 1)

| # | Fix | Files Affected | Effort |
|---|---|---|---|
| P1-1 | Fix `Profile` route → create real `ProfileScreen.kt` | `JarvisNavigation.kt`, new `ProfileScreen.kt` | 1 day |
| P1-2 | Fix `FactRepository.markFactRead()` PATCH → send `status="ACKNOWLEDGED"` | `FactRepository.kt` | 2 hours |
| P1-3 | Fix `FinancialEventEntity.updated_at` sync mapping | `InsightSyncService.kt` | 1 hour |
| P1-4 | Fix `FinancialInsightEntity.priority` from hardcoded "MEDIUM" → Supabase field | `InsightSyncService.kt` | 1 hour |
| P1-5 | Wire `DailyBriefScreen` with data: create `DailyBriefViewModel` | New `DailyBriefViewModel.kt`, `DailyBriefScreen.kt` | 2 days |

### Phase 2 — Detail Screens (Week 2)

| # | Feature | Files Affected | Effort |
|---|---|---|---|
| P2-1 | Build `TaskDetailScreen` + `TodoViewModel` | New `TaskDetailScreen.kt`, `TodoViewModel.kt` | 2 days |
| P2-2 | Build `FactDetailScreen` | New `FactDetailScreen.kt` | 1 day |
| P2-3 | Add category grouping to `FactsScreen` | `FactsScreen.kt`, `FactViewModel.kt` | 1 day |

### Phase 3 — Financial Upgrade (Week 3)

| # | Feature | Supabase Tables | Effort |
|---|---|---|---|
| P3-1 | Sync `monthly_spending_summary` → replace VM aggregation | `monthly_spending_summary` | 2 days |
| P3-2 | Sync `monthly_category_spend` → replace category breakdown | `monthly_category_spend` | 1 day |
| P3-3 | Sync `monthly_category_trends` → replace trend chart | `monthly_category_trends` | 1 day |
| P3-4 | Build Finance Category → Transactions drilldown | New `FinanceCategoryScreen.kt` | 1 day |

### Phase 4 — Home Redesign (Week 4)

| # | Feature | Effort |
|---|---|---|
| P4-1 | Home: Daily Brief preview card wired to `DailyBriefViewModel` | 1 day |
| P4-2 | Home: Financial Pulse card from `monthly_spending_summary` | 1 day |
| P4-3 | Home: FYI section with category badges | 1 day |
| P4-4 | Daily Brief notification worker at 7:30 AM | 1 day |

### Phase 5 — Facts Graph & System Polish (Week 5+)

| # | Feature | Supabase Tables | Effort |
|---|---|---|---|
| P5-1 | Sync `fact_relationships` | `fact_relationships` | 2 days |
| P5-2 | `FactDetailScreen` with related entity links | — | 1 day |
| P5-3 | Profile screen with sync status from `sync_diagnostics` | — | 2 days |
| P5-4 | Remove prop-drilling from `JarvisNavHost` (30+ params → internal VMs) | — | 2 days |

---

## Appendix A — Sync Architecture Diagram

```
WorkManager Schedule
├── JarvisSyncWorker (05:55, 13:55, 20:55)
│     └── SyncService → SupabaseUploader → jarvis-signals bucket
│
├── InsightSyncWorker (06:20 AM daily)
│     └── InsightSyncService.syncInsights()
│           ├── fetchTable("todo_items")        → todos
│           ├── fetchTable("financial_events")  → financial_events
│           ├── fetchTable("fyi_events")        → fyi_events
│           ├── fetchTable("user_preferences")  → user_preferences
│           ├── fetchTable("daily_briefs")      → daily_briefs
│           ├── fetchTable("facts")             → facts
│           ├── fetchTable("user_actions")      → user_actions
│           ├── fetchTable("financial_facts")   → financial_insights
│           └── [NOT FETCHED] monthly_* tables, fact_relationships
│
└── TodoNotificationWorker (07:00, 18:00)
      └── Post local task-due notifications
```

---

## Appendix B — Known Issues Summary

| # | Issue | Severity | Phase |
|---|---|---|---|
| 1 | `Profile` tab renders `NotificationScreen` — wrong mapping | 🔴 Critical | P1-1 |
| 2 | `FactRepository.markFactRead()` PATCHes non-existent `read_flag` column | 🔴 Critical | P1-2 |
| 3 | `DailyBriefScreen` disconnected from all data | 🔴 Critical | P1-5 |
| 4 | `task_detail/{id}` is a text placeholder | 🟠 High | P2-1 |
| 5 | `fact_detail/{id}` is a text placeholder | 🟠 High | P2-2 |
| 6 | Income classification via string heuristic in ViewModel | 🟠 High | P3-1 |
| 7 | `monthly_*` tables not synced — aggregation is local | 🟠 High | P3 |
| 8 | `FinancialInsightEntity.priority` hardcoded "MEDIUM" | 🟡 Medium | P1-4 |
| 9 | `FinancialEventEntity.updated_at` mapped to `created_at` | 🟡 Medium | P1-3 |
| 10 | `JarvisNavHost` receives 30+ parameters (prop drilling) | 🟡 Medium | P5-4 |
| 11 | `fallbackToDestructiveMigration()` — data loss on schema change | 🟡 Medium | Ongoing |
| 12 | No `DailyBriefViewModel` | 🟡 Medium | P1-5 |
| 13 | No morning notification for Daily Brief | 🟡 Medium | P4-4 |
| 14 | `Screen.Brief` not a bottom tab — only reachable from Home card | 🟢 Low | P1 |
| 15 | `notification/{id}` route is an inline Text placeholder | 🟢 Low | P4 |
| 16 | `FinancialActionCard.kt` unused in current UI | 🟢 Low | Cleanup |
