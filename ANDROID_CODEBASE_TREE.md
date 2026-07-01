# JARVIS Android — Codebase Tree & Inventory

> **Audited**: 2026-07-01 | **DB Version**: 5 | **Build**: PASSING

---

## 1. PACKAGE TREE

```
c:\jarvis\jarviscollector\app\src\main\java\com\pradeep\jarviscollector\
│
├── MainActivity.kt                          (24,382 bytes)
│
├── database/
│   ├── JarvisDatabase.kt                    (2,440 bytes)   Room singleton, v5
│   ├── InsightDaos.kt                       (7,784 bytes)   All 11 DAO interfaces
│   └── MobileSignalDao.kt                   (2,325 bytes)   Signal queue DAO
│
├── model/
│   ├── InsightEntities.kt                   (3,065 bytes)   9 Room entities
│   ├── MobileSignal.kt                      (575 bytes)     SMS capture entity
│   └── NotificationEvent.kt                 (185 bytes)     Local notification model
│
├── navigation/
│   ├── NavRoutes.kt                         (1,794 bytes)   Screen sealed class, 16 routes
│   ├── JarvisNavigation.kt                  (~18,000 bytes) NavHost, all composable bindings
│   └── BottomNavigationBar.kt               Bottom nav bar (5 tabs)
│
├── network/
│   ├── JarvisInsightsClient.kt              (5,026 bytes)   OkHttp3 Supabase client
│   ├── JarvisApiClient.kt                   (2,663 bytes)   Signal upload client
│   ├── SupabaseUploader.kt                  (1,873 bytes)   Batch upload helper
│   └── QueryInstrumentation.kt              (912 bytes)     Query telemetry logger
│
├── repository/
│   ├── TodoRepository.kt                    (4,464 bytes)
│   ├── FactRepository.kt                    (2,515 bytes)
│   ├── FinancialRepository.kt               (5,657 bytes)
│   ├── FinancialInsightRepository.kt        (4,722 bytes)
│   ├── FYIRepository.kt                     (5,386 bytes)
│   ├── ActionsRepository.kt                 (2,630 bytes)
│   ├── NotificationRepository.kt            (430 bytes)
│   ├── NotificationCenterRepository.kt      (3,614 bytes)
│   ├── PreferenceRepository.kt              (2,424 bytes)
│   ├── MobileSignalRepository.kt            (4,551 bytes)
│   ├── SignalExplorerRepository.kt          (4,817 bytes)
│   └── SmsRepository.kt                     (3,932 bytes)
│
├── service/
│   ├── InsightSyncService.kt                (28,896 bytes)  Core sync orchestrator ⭐
│   ├── InsightSyncWorker.kt                 (2,003 bytes)   WorkManager wrapper
│   ├── InsightSyncWorkerHelper.kt           (3,124 bytes)   Schedules sync worker
│   ├── JarvisNotificationListener.kt        (4,279 bytes)   System notification capture
│   ├── JarvisSyncWorker.kt                  (1,600 bytes)   Signal upload worker
│   ├── JarvisSyncWorkerHelper.kt            (4,451 bytes)   Schedules upload worker
│   ├── SyncService.kt                       (3,041 bytes)   Legacy sync service
│   ├── TodoNotificationHelper.kt            (2,543 bytes)   Todo reminder scheduler
│   └── TodoNotificationWorker.kt            (4,361 bytes)   Todo notification worker
│
└── ui/
    ├── HomeScreen.kt                        (25,575 bytes)  ⭐ Main dashboard
    ├── FinancialScreen.kt                   (23,874 bytes)  ⭐ Financial dashboard
    ├── DailyBriefScreen.kt                  (17,827 bytes)  ⚠️ Legacy, needs redesign
    ├── NotificationScreen.kt               (16,951 bytes)  ⚠️ Partial
    ├── TodoScreen.kt                        (11,623 bytes)  ✅ Complete
    ├── FyiCategoryScreen.kt                 (8,829 bytes)   ✅ Complete
    ├── FyiScreen.kt                         (7,703 bytes)   ✅ Complete
    ├── AgentFramework.kt                    (5,411 bytes)   Utility composables
    │
    ├── dashboard/
    │   └── HomeDashboardViewModel.kt        (6,144 bytes)   ⭐ Home metrics ViewModel
    │
    ├── facts/
    │   ├── FactsScreen.kt                   (3,698 bytes)   ✅ Complete
    │   ├── FactViewModel.kt                 (963 bytes)     ✅ Complete
    │   └── FactCard.kt                      (5,112 bytes)   Reusable fact card component
    │
    ├── financial/
    │   ├── FinancialDashboardViewModel.kt   (7,613 bytes)   ⭐ Financial aggregation ViewModel
    │   ├── FinancialInsightViewModel.kt     (2,596 bytes)   Financial insight flows
    │   ├── TransactionDetailViewModel.kt    (3,248 bytes)   ✅ Complete
    │   ├── TransactionDetailScreen.kt       (6,349 bytes)   ✅ Complete
    │   └── FinancialActionCard.kt           (4,719 bytes)   Legacy action card (unused in new UI)
    │
    ├── notification/
    │   └── NotificationCenterScreen.kt      ⚠️ Partial
    │
    ├── actioncenter/
    │   └── ActionCenterScreen.kt
    │
    ├── debug/
    │   └── DebugDataPipelineScreen.kt       Debug tool (accessible from Profile)
    │
    ├── signalexplorer/
    │   └── SignalExplorerScreen.kt          Debug trace tool
    │
    └── theme/
        └── (theme files)
```

---

## 2. SCREEN INVENTORY

| Screen | File | Route | ViewModel | Status |
| :--- | :--- | :--- | :--- | :--- |
| Home Dashboard | `HomeScreen.kt` | `home` | `HomeDashboardViewModel` | ✅ Complete |
| Tasks | `TodoScreen.kt` | `tasks` | inline (no dedicated VM) | ✅ Complete |
| Task Detail | inline composable | `task_detail/{id}` | — | ⚠️ Placeholder |
| Facts | `facts/FactsScreen.kt` | `facts` | `FactViewModel` | ✅ Complete |
| Fact Detail | inline composable | `fact_detail/{id}` | — | ⚠️ Placeholder |
| FYI Overview | `FyiScreen.kt` | `fyi` | — | ✅ Complete |
| FYI Category | `FyiCategoryScreen.kt` | `fyi_category/{category}` | — | ✅ Complete |
| Financial Dashboard | `FinancialScreen.kt` | `finance` | `FinancialDashboardViewModel` + `FinancialInsightViewModel` | ✅ Complete |
| Transaction Detail | `financial/TransactionDetailScreen.kt` | `transaction_detail/{id}` | `TransactionDetailViewModel` | ✅ Complete |
| Daily Brief | `DailyBriefScreen.kt` | `brief` | — (no VM, receives params) | ⚠️ Legacy |
| Profile | inline composable | `profile` | — | Basic |
| Notification Center | `notification/NotificationCenterScreen.kt` | `notification_center` | — | ⚠️ Partial |
| Action Center | `actioncenter/ActionCenterScreen.kt` | `action_center` | — | Basic |
| Signal Explorer | `signalexplorer/SignalExplorerScreen.kt` | `signal_explorer/{type}/{id}` | — | Debug tool |
| Debug Pipeline | `debug/DebugDataPipelineScreen.kt` | `debug_pipeline` | — | Debug tool |

---

## 3. REPOSITORY INVENTORY

| Repository | Type | Pattern | Supabase Table |
| :--- | :--- | :--- | :--- |
| `TodoRepository` | object | Remote-First | `todo_items` |
| `FactRepository` | object | Remote-First | `facts` |
| `FinancialRepository` | object | Remote-First | `financial_events` |
| `FinancialInsightRepository` | object | Remote-First | `financial_facts` |
| `FYIRepository` | object | Remote-First | `fyi_events` |
| `ActionsRepository` | object | Fire-and-forget | `user_actions` |
| `NotificationRepository` | object | Local | — |
| `NotificationCenterRepository` | object | Local | — |
| `PreferenceRepository` | object | Local + Remote | `user_preferences` |
| `MobileSignalRepository` | object | Upload queue | `mobile_signals` (upload target) |
| `SignalExplorerRepository` | object | Read-only | Multiple |
| `SmsRepository` | object | Device read | — |

---

## 4. VIEWMODEL INVENTORY

| ViewModel | Type | Init Flow | Key State |
| :--- | :--- | :--- | :--- |
| `HomeDashboardViewModel` | AndroidViewModel | Loads on init, observes 3 Flows | `StateFlow<HomeDashboardUiState>` |
| `FactViewModel` | AndroidViewModel | Directly wraps DAO Flow via stateIn | `StateFlow<List<FactInsightEntity>>` |
| `FinancialDashboardViewModel` | AndroidViewModel | Loads on init, observes financialEventDao Flow | `StateFlow<FinancialDashboardUiState>` |
| `FinancialInsightViewModel` | ViewModel | Wraps FinancialInsightRepository flows via stateIn | 5 `StateFlow<List<FinancialInsightEntity>>` |
| `TransactionDetailViewModel` | AndroidViewModel | Loads single entity on `loadTransaction(id)` | `StateFlow<TransactionDetailUiState>` |

---

## 5. ROOM ENTITY INVENTORY

| Entity | Table | PK Type | Fields Count | DAO |
| :--- | :--- | :--- | :--- | :--- |
| `TodoEntity` | `todos` | String | 9 | `TodoDao` |
| `FinancialEventEntity` | `financial_events` | String | 10 | `FinancialEventDao` |
| `FyiEventEntity` | `fyi_events` | String | 9 | `FyiEventDao` |
| `UserPreferenceEntity` | `user_preferences` | String | 3 | `UserPreferenceDao` |
| `UserActionEntity` | `user_actions` | String | 6 | `UserActionDao` |
| `DailyBriefEntity` | `daily_briefs` | String | 4 | `DailyBriefDao` |
| `FactInsightEntity` | `facts` | String | 9 | `FactInsightDao` |
| `NotificationEntity` | `notifications` | String | 10 | `NotificationDao` |
| `FinancialInsightEntity` | `financial_insights` | String | 10 | `FinancialInsightDao` |
| `SyncDiagnosticsEntity` | `sync_diagnostics` | String | 7 | `SyncDiagnosticsDao` |
| `MobileSignal` | `mobile_signals` | Int (AutoGen) | varies | `MobileSignalDao` |

---

## 6. NAVIGATION ROUTE INVENTORY

| Route Object | Route String | Args | Composable Registered | Status |
| :--- | :--- | :--- | :--- | :--- |
| `Screen.Home` | `home` | — | ✅ | Working |
| `Screen.Brief` | `brief` | — | ✅ | Legacy UI |
| `Screen.Tasks` | `tasks` | — | ✅ | Working |
| `Screen.Facts` | `facts` | — | ✅ | Working |
| `Screen.Finance` | `finance` | — | ✅ | Working |
| `Screen.Fyi` | `fyi` | — | ✅ | Working |
| `Screen.Profile` | `profile` | — | ✅ | Basic |
| `Screen.FyiCategory` | `fyi_category/{category}` | `category: String` | ✅ | Working |
| `Screen.NotificationDetail` | `notification/{id}` | `id: String` | ✅ | Working |
| `Screen.NotificationCenter` | `notification_center` | — | ✅ | Partial |
| `Screen.ActionCenter` | `action_center` | — | ✅ | Working |
| `Screen.SignalExplorer` | `signal_explorer/{entityType}/{entityId}` | 2x String | ✅ | Debug |
| `Screen.DebugPipeline` | `debug_pipeline` | — | ✅ | Debug |
| `Screen.TaskDetail` | `task_detail/{id}` | `id: String` | ✅ | ⚠️ Placeholder |
| `Screen.FactDetail` | `fact_detail/{id}` | `id: String` | ✅ | ⚠️ Placeholder |
| `Screen.TransactionDetail` | `transaction_detail/{id}` | `id: String` | ✅ | ✅ Complete |
