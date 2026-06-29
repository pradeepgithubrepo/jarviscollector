# NAVIGATION & USER EXPERIENCE BLUEPRINT ASSESSMENT v1.0

## Objective

Assess the current navigation structure, routing models, screen hierarchies, and user journey paths within the Jarvis Android application ("Jarvis Collector"). 

This assessment evaluates screen relationships, redundant layouts, navigation complexity, and compliance with the **JARVIS Android App v1.0 Master Architecture & Design Instruction Set**. It provides the navigation blueprint to build a premium, glassmorphic client interface.

---

# Section 1 - Current Navigation Inventory

The application contains 11 navigation destinations. They are currently managed using an in-memory state router inside `MainActivity.kt`.

| Screen Name | Entry Point | Exit Paths | Navigation Method | Purpose |
| --- | --- | --- | --- | --- |
| **HomeScreen** | App Launch / Root | Todos, Financial, FYI, Daily Brief, Settings, Categories | Direct State Swap | Command center dashboard displaying pending action summaries and KPI counts. |
| **TodoScreen** | HomeScreen | Home (Back button) | Callback lambda | Lists active tasks; supports Snooze/Complete/Dismiss actions. |
| **FinancialScreen**| HomeScreen | Home (Back button) | Callback lambda | Lists processed cashflow events and transaction records. |
| **FyiScreen** | HomeScreen | Home (Back button) | Callback lambda | Lists all informational FYI alerts. |
| **DailyBriefScreen**| HomeScreen | Home (Back button) | Callback lambda | Displays compiled daily morning and evening brief sections. |
| **NotificationScreen**| Settings icon | Home (Back button) | Callback lambda | System settings, manual sync tools, and raw mobile signal logs. |
| **FamilyScreen** | Home Grid | Home (Back button) | Callback lambda | FYI alerts pre-filtered by `"family"` category. |
| **SchoolScreen** | Home Grid | Home (Back button) | Callback lambda | FYI alerts pre-filtered by `"school"` category. |
| **TravelScreen** | Home Grid | Home (Back button) | Callback lambda | FYI alerts pre-filtered by `"travel"` category. |
| **HealthScreen** | Home Grid | Home (Back button) | Callback lambda | FYI alerts pre-filtered by `"health"` category. |
| **ShoppingScreen** | Home Grid | Home (Back button) | Callback lambda | FYI alerts pre-filtered by `"shopping"` category. |

---

# Section 2 - Navigation Graph

The current navigation hierarchy is a flat, two-level star topology centered on the HomeScreen.

```text
Home (MainActivity.kt Router)
├── DailyBrief (DailyBriefScreen.kt)
├── Todos (TodoScreen.kt)
├── Finance (FinancialScreen.kt)
├── FYI (FyiScreen.kt)
├── Settings (NotificationScreen.kt)
└── Category List
    ├── Family (FamilyScreen.kt)
    ├── School (SchoolScreen.kt)
    ├── Travel (TravelScreen.kt)
    ├── Health (HealthScreen.kt)
    └── Shopping (ShoppingScreen.kt)
```

---

# Section 3 - Home Screen Assessment

### Current HomeScreen Evaluation
* **Strengths**: 
  * Simple, quick navigation to sub-screens.
  * Clear numeric badge indicators for pending tasks and notifications.
  * Immediate access to settings and manual sync triggers.
* **Weaknesses**:
  * Uses standard default material colors; lacks the premium glassmorphic styling mandated by Section 4 of the Android Master Instructions.
  * Over-cluttered with five near-identical category tiles (Family, School, etc.) rather than a clean, unified dashboard grid.
  * Lacks Net Cashflow cash indicators and Facts counts.

### Component Classification: **REBUILD**
> [!NOTE]
> Rebuild the HomeScreen to transition from a launcher template to an executive command center displaying cashflow carousels, memory card highlights, and brief modals.

---

# Section 4 - Screen Importance Analysis

To guide navigation improvements, screens are prioritized by utility:

### 1. PRIMARY
* **Home**: Central entry point to immediately scan cashflow, briefs, and actions.
* **Daily Brief**: Flagship experience offering high-level morning priorities.
* **Todo**: Core action hub allowing users to swipe tasks away.
* **Finance**: Critical cashflow drilldown dashboard.

### 2. SECONDARY
* **Facts (Future)**: Browse memories and registered bank/insurance configurations.
* **FYI**: Informational news feed categorized dynamically.

### 3. UTILITY
* **Settings (Notification Logs)**: Backfilling, diagnostic sync logs, and permissions.
* **Profile Settings**: Personal profile configuration details.

---

# Section 5 - Bottom Navigation Assessment

The platform specifies a **Bottom Navigation Bar** with five standard destinations:
`Home` | `Brief` | `Tasks` | `FYI` | `Profile`

* **Can current navigation support this?**: No. The current app has no bottom bar. It relies entirely on home grid tiles.
* **Required changes**:
  1. Integrate Jetpack Compose `Navigation Compose` (`NavController` and `NavHost`).
  2. Implement a global `Scaffold` container hosting the bottom navigation bar.
  3. Relocate settings and categories to secondary menus/drawers to clean up the primary view.

---

# Section 6 - Home Return Rule Assessment

> [!IMPORTANT]
> **Rule**: Every screen must support returning to Home with a visible Home Icon or Back button in one tap.

* **Current compliance**: **90% Compliant**. 
* **Violations**: Sub-screens have a standard back icon returning to Home, but Settings and category list views lack a dedicated, persistent Home icon.
* **Required changes**: Implement a standard, sticky Top App Bar across all sub-screens containing a home shortcut icon alongside the back navigation.

---

# Section 7 - User Journey Assessment

User journeys are evaluated by complexity and tap counts:

1. **Morning Check-In**:
   * *Flow*: Open App → Click Daily Brief tile.
   * *Taps*: 1 tap.
   * *Pain Point*: Users must click a card instead of seeing a Daily Brief popup automatically on launch.
2. **Task Completion**:
   * *Flow*: Open App → Click Todo tile → Click "Complete" button → Click Back.
   * *Taps*: 3 taps.
   * *Pain Point*: Cannot swipe to complete directly from the HomeScreen.
3. **Financial Review**:
   * *Flow*: Open App → Click Finance tile → Scroll list.
   * *Taps*: 1 tap.
   * *Pain Point*: Financial drills are flat. Users see raw transaction lists immediately instead of structured category KPIs first.
4. **Fact Review**:
   * *Flow*: Open App → Look for facts.
   * *Taps*: **Impossible**.
   * *Pain Point*: Facts are not synced or displayed.
5. **Notification Open**:
   * *Flow*: Click push notification → Launches launcher screen.
   * *Taps*: 1 tap.
   * *Pain Point*: Does not redirect directly to the Daily Brief Screen.

---

# Section 8 - Screen Duplication Assessment

The codebase contains five duplicate category layouts:
`FamilyScreen`, `SchoolScreen`, `TravelScreen`, `HealthScreen`, and `ShoppingScreen`.

* **Evaluation**: These five files contain identical Compose columns, lists, card views, and exit buttons. The only difference is the string filter applied to `fyiEvents` (e.g., `"school"` vs `"family"`).
* **Consolidation Resolution**:
  > [!TIP]
  > **Consolidate: REMOVE 5 screens, CREATE 1 generic screen**
  > Delete the five files and implement a single reusable `FyiCategoryListScreen(categoryName: String, events: List<FyiEventEntity>)` parameterized by route arguments.

---

# Section 9 - Dashboard Readiness

Assess components ready to build the V2 Home Dashboard:

* **Daily Brief Preview**: **READY** (Uses latest cached brief date).
* **Fact Summary**: **NOT READY** (Blocked by facts sync).
* **Finance Snapshot**: **PARTIAL** (Basic unpaid bills count ready, cashflow sum is blocked).
* **Task Summary**: **READY** (Pending todos count ready).
* **FYI Summary**: **READY** (Total FYI count ready).

---

# Section 10 - Information Architecture

The future app architecture shifts from home launcher tiles to a tab-centered navigation:

```text
Global Scaffold (Host)
├── Bottom Navigation Bar
│   ├── Home Tab (HomeScreen)
│   ├── Brief Tab (DailyBriefScreen)
│   ├── Tasks Tab (TodoScreen)
│   ├── FYI Tab (FyiScreen)
│   └── Profile Tab (Settings / Preferences)
└── Navigational Drilldowns (Secondary Paths)
    ├── Home -> Category Details (Consolidated Category Screen)
    └── Finance -> Category -> Merchant -> Transactions (Drilldown Screen)
```

---

# Section 11 - Navigation Gap Analysis

1. **Gap**: Lack of Navigation Compose Framework
   * *Impact*: Harder to manage route parameters and deep-links from push notifications.
2. **Gap**: Redundant Screens
   * *Impact*: High codebase maintenance and duplication of Compose layouts (category screens).
3. **Gap**: Missing Bottom Navigation Bar
   * *Impact*: Fails the V1 UI requirements (Rule 5).
4. **Gap**: Flat Financial View
   * *Impact*: No drill-down paths from KPIs to category list items, violating Rule 12.

---

# Section 12 - Future Blueprint Recommendation

Recommended actions for all UI screens:

| Screen Name | Current Status | Target Action | Justification |
| --- | --- | --- | --- |
| **`HomeScreen`** | Active | **MODIFY** | Rebuild layout to show premium summaries; remove category grid tiles. |
| **`DailyBriefScreen`**| Active | **KEEP** | Standard card summary viewer fits rules. |
| **`TodoScreen`** | Active | **MODIFY** | Adapt to bottom bar container. |
| **`FyiScreen`** | Active | **MODIFY** | Consolidate to act as the primary FYI tab. |
| **`FamilyScreen`** | Active | **REMOVE** | Consolidate into generic category screen. |
| **`SchoolScreen`** | Active | **REMOVE** | Consolidate into generic category screen. |
| **`TravelScreen`** | Active | **REMOVE** | Consolidate into generic category screen. |
| **`HealthScreen`** | Active | **REMOVE** | Consolidate into generic category screen. |
| **`ShoppingScreen`** | Active | **REMOVE** | Consolidate into generic category screen. |
| **`CategoryScreen`** | Missing | **CREATE** | Reusable generic component handling all category FYI lists. |
| **`FactsScreen`** | Missing | **CREATE** | Brand new memory dashboard screen. |
| **`FinanceScreen`** | Active | **REBUILD** | Add drilldown hierarchy (Overview → Category → Transactions). |

---

# Section 13 - Reuse Matrix

Mapping of UI screens and reuse classifications:

| Screen | Purpose | Current Status | Reuse Recommendation | Action |
| --- | --- | --- | --- | --- |
| **`HomeScreen`** | Dashboard | Active | **MODIFY** | Rebuild layout. |
| **`DailyBriefScreen`**| Daily Summaries | Active | **KEEP** | Reuse cards list. |
| **`TodoScreen`** | Tasks | Active | **KEEP** | Reuse task cards and toggle lambdas. |
| **`NotificationScreen`**| Settings Logs | Active | **MODIFY** | Relocate to Settings tab. |

---

# Conclusion & Success Criteria Answers

1. **What the future navigation model should be**: Tabbed routing using `Navigation Compose` and a persistent global **Bottom Navigation Bar**.
2. **What screens should remain**: Home, Daily Brief, Todo, FYI, and Settings/Profile.
3. **What screens should be consolidated**: The 5 near-identical category screens (Family, School, Travel, Health, Shopping) must be consolidated into a single reusable parameterized screen.
4. **Whether Home can become the primary command center**: Yes. By utilizing KPI cards, Net cashflow indicators, and a daily brief launch popup, the HomeScreen maps out the entire daily overview.
5. **How users should move through the app**: Quick-swipe bottom tabs to browse primary features (Home, Brief, Tasks, FYI), with direct tapping on dashboard cards to launch detailed drilldowns (like Finance categories).
6. **What navigation changes are required**: Implement Jetpack Navigation Compose, add a global Bottom Navigation Bar, and deprecate redundant category layouts.
