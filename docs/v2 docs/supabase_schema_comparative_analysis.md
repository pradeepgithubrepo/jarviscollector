# JARVIS Android ↔ Supabase Comparative Schema Analysis
> **Live schema probe**: 2026-07-17 | **Schemas**: `jarvis_insights_schema` & `jarvis_insights_schemav1` | **Status**: Updated

---

## PART 1 — Live Schema Discovery Summary

**All tables confirmed present** on Supabase. ToDos are now mapped to the authoritative `jarvis_insights_schemav1.tasks` table, which holds the 20+ user tasks.

| Table | Schema | Rows | Android Uses? | Notes |
|---|---|---|---|---|
| `tasks` | `jarvis_insights_schemav1` | 20+ | ✅ Synced | **Authoritative task source** |
| `todo_items` | `jarvis_insights_schema` | 14 | ❌ DEPRECATED | Replaced by `tasks` |
| `financial_events` | `jarvis_insights_schema` | 158 | ✅ Synced | ⚠️ Android maps 7 of 13 columns |
| `fyi_events` | `jarvis_insights_schema` | 84 | ✅ Synced | ⚠️ Android maps 7 of 15 columns — key fields missing |
| `daily_briefs` | `jarvis_insights_schema` | 5 | ✅ Synced | ✅ v6 entity covers all 12 columns |
| `facts` | `jarvis_insights_schema` | 22 | ✅ Synced | 🔴 Write path broken; maps 5 of 17 columns |
| `user_preferences` | `jarvis_insights_schema` | 4 | ✅ Synced | ✅ Full 1:1 mapping |
| `user_actions` | `jarvis_insights_schema` | 25 | ✅ Synced | ✅ Full 1:1 mapping |
| `financial_facts` | `jarvis_insights_schema` | 158 | ✅ Synced | 🔴 Android maps 7 of 28 columns — massive gap |
| `monthly_spending_summary` | `jarvis_insights_schema` | 3 | ❌ NOT synced | 🟡 New — replaces ViewModel heuristics |
| `monthly_category_spend` | `jarvis_insights_schema` | 9 | ❌ NOT synced | 🟡 New — replaces category aggregation |
| `monthly_category_trends` | `jarvis_insights_schema` | 9 | ❌ NOT synced | 🟡 New — replaces trend chart |
| `salary_cycles` | `jarvis_insights_schema` | 1 | ❌ NOT synced | 🆕 Never considered — income source |
| `transfer_pairs` | `jarvis_insights_schema` | 21 | ❌ NOT synced | 🆕 Never considered — transfer deduplication |
| `signals` | `jarvis_insights_schema` | 100 | ❌ NOT synced | 🔒 Debug/SignalExplorer only |
| `fact_relationships` | `jarvis_insights_schema` | 0 | ❌ NOT synced | 🟡 Table exists, empty — future use |
| `merchant_mappings` | `jarvis_insights_schema` | 0 | ❌ NOT synced | 🟡 Table exists, empty — future use |
| `understood_signals` | `jarvis_insights_schema` | 531 | ❌ NOT synced | 🔒 Pipeline internal — SignalExplorer trace only |
| `qualified_signals` | `jarvis_insights_schema` | 1,004 | ❌ NOT synced | 🔒 Pipeline internal — do not expose |
| `processed_files` | `jarvis_insights_schema` | 24 | ❌ NOT synced | 🔒 Pipeline internal — do not expose |
| `system_status` | `jarvis_insights_schema` | 1 | ❌ NOT synced | 🔒 Admin — do not expose |
| `pipeline_runs` | `jarvis_insights_schema` | 10 | ❌ NOT synced | 🔒 Admin — do not expose |

---

## PART 2 — Table-by-Table Comparative Field Analysis

### 2.1 `tasks` (`jarvis_insights_schemav1`) — 20+ rows

**Supabase (14 columns):**
```
id, title, description, status, priority, due_datetime, notification_profile,
source_type, route_id, created_by, assigned_to, created_at, updated_at, completed_at
```

**Android `TodoEntity` (12 fields):**
```kotlin
todo_id, title, description, category, priority, status, due_date,
source_signal_id, source_agent, confidence, created_at, updated_at
```

| Supabase `tasks` Column | Android `TodoEntity` Field | Mapping / Clean Code Logic |
|---|---|---|
| `id` | `todo_id` | UUID string |
| `title` | `title` | Task headline text |
| `description` | `description` | Markdown details block |
| `source_type` | `category` | e.g. `"AUTO_GENERATED"` (default to `"General"`) |
| `priority` | `priority` | CRITICAL, HIGH, MEDIUM, LOW |
| `status` | `status` | OPEN, COMPLETED, SNOOZED, DISMISSED |
| `due_datetime` | `due_date` | Cleaned to plain date portion (split on "T") |
| `route_id` | `source_signal_id` | Mapped directly (UUID) |
| `created_by` | `source_agent` | e.g. `"JARVIS"` |
| `created_at` | `created_at` | Received ISO timestamp |
| `updated_at` | `updated_at` | Modified ISO timestamp |

**Recommended `TodoEntity` structure (as implemented):**
```kotlin
data class TodoEntity(
    @PrimaryKey val todo_id: String,
    val title: String?,
    val description: String?,
    val category: String?,           // Added from Supabase `tasks.source_type`
    val priority: String?,           // Supports CRITICAL, HIGH, MEDIUM, LOW
    val status: String,              // OPEN, COMPLETED, SNOOZED, DISMISSED
    val due_date: String?,
    val source_signal_id: String?,   // Extracted from Supabase `tasks.route_id`
    val source_agent: String?,       // Added from Supabase `tasks.created_by`
    val confidence: Double?,         // Hardcoded to 1.0 for authoritative tasks
    val created_at: String?,
    val updated_at: String?
)
```

---

| `due_date` | String (ISO) nullable | ✅ String? | OK |
| `source_agent` | "SignalUnderstandingAgent" etc. | ❌ Missing | NEW — useful for debug/display |
| `source_reference` | JSON `{'signal_id': '...'}` | ⚠️ Mapped to `source_signal_id` but as JSON dict, not UUID string | **Mapping is broken — source_reference is a JSON dict, not a plain ID** |
| `confidence` | Float (0.0–1.0) | ❌ Missing | NEW — useful for confidence badge |
| `created_at` | ISO timestamp | ✅ String? | OK |
| `updated_at` | ISO timestamp | ✅ String? | OK |
| `batch_id` | UUID nullable | ❌ Missing | Pipeline internal — skip |
| `sync_status` | "PENDING"/"SYNCED" | ❌ Missing | Useful for sync state indicator |
| `is_deleted` | Boolean | ❌ Missing | **CRITICAL — Android must filter `is_deleted = false`** |
| `deleted_at` | Timestamp nullable | ❌ Missing | Pipeline internal — skip |

> [!CAUTION]
> **`is_deleted` is not filtered!** Android syncs `deleteAll()` then `insertAll()` — so deleted todos are currently re-inserted on every sync if Supabase returns them. The fetch query must add `?is_deleted=eq.false` filter.

> [!WARNING]
> **`source_reference` mapping is broken.** Supabase stores it as `{'signal_id': 'uuid'}` JSON dict. Android's sync code tries `getString("source_signal_id")` or `getString("source_reference")` and stores the raw JSON string as `source_signal_id`. The entity and display code likely shows `{'signal_id': '...'}` as the trace ID.

**Recommended `TodoEntity` upgrade:**
```kotlin
data class TodoEntity(
    @PrimaryKey val todo_id: String,
    val title: String?,
    val description: String?,
    val category: String?,          // ADD — "FINANCIAL", "HEALTH", etc.
    val priority: String?,           // FIX — handle "CRITICAL"
    val status: String,
    val due_date: String?,
    val source_signal_id: String?,   // Keep, but fix extraction from JSON dict
    val source_agent: String?,       // ADD — for attribution display
    val confidence: Double?,         // ADD — for confidence badge
    val created_at: String?,
    val updated_at: String?
)
```

---

### 2.2 `financial_events` — 158 rows

**Supabase (13 columns):**
```
id, title, amount, currency, transaction_type, payment_channel,
paid_to, paid_from, transaction_id, event_date, source_signal_id,
created_at, category
```

**Android `FinancialEventEntity` (10 fields):**
```kotlin
financial_event_id, merchant, amount, currency, category,
status, event_timestamp, source_signal_id, created_at, updated_at
```

| Column | Supabase | Android | Gap |
|---|---|---|---|
| `id` | Int (PK) | ✅ `financial_event_id` | Mapping OK via optString |
| `title` | Transaction description | ✅ Mapped to `merchant` | Functional but semantically wrong — `title` is a sentence, not a merchant name |
| `amount` | Double | ✅ Double? | OK |
| `currency` | "INR" | ✅ String? | OK |
| `transaction_type` | **"debit"/"credit"** | ❌ Missing | **CRITICAL — this is the authoritative income/expense flag!** |
| `payment_channel` | "UPI"/"NEFT"/NULL | ❌ Missing | NEW — useful for payment method display |
| `paid_to` | Counterparty name | ❌ Missing | NEW — actual merchant/recipient name |
| `paid_from` | Source account | ❌ Missing | NEW — useful for account display |
| `transaction_id` | Bank ref number | ❌ Missing | NEW — for reconciliation |
| `event_date` | ISO timestamp | ✅ `event_timestamp` | OK |
| `source_signal_id` | Int | ✅ String? | OK |
| `created_at` | ISO timestamp | ✅ String? | OK |
| `category` | String | ✅ String? | OK |
| ~~`status`~~ | **NOT IN SUPABASE** | ❌ Android has `status` field | **Android entity has a field that doesn't exist on Supabase** |
| ~~`updated_at`~~ | **NOT IN SUPABASE** | ❌ Android maps `created_at` to `updated_at` | **`updated_at` does not exist on Supabase `financial_events`** |

> [!CAUTION]
> **`transaction_type` ("debit"/"credit") is the authoritative income/expense flag** but Android completely ignores it! Instead `FinancialDashboardViewModel` uses keyword heuristics on `merchant` (which is actually the `title` field). This is the root cause of the income classification bug.

> [!WARNING]
> **`paid_to` is the actual merchant name**, not `title`. `title` is a human-readable sentence like `"Transaction of INR 2000.0 at VA-SBIPSG-T"`. Android displays this sentence as the merchant name.

**Recommended `FinancialEventEntity` upgrade:**
```kotlin
data class FinancialEventEntity(
    @PrimaryKey val financial_event_id: String,
    val title: String?,             // Keep — display text
    val paid_to: String?,           // ADD — actual merchant/payee
    val paid_from: String?,         // ADD — source account
    val amount: Double?,
    val currency: String?,
    val transaction_type: String?,  // ADD — "debit"/"credit" — REPLACES heuristic!
    val payment_channel: String?,   // ADD — "UPI"/"NEFT"/etc.
    val transaction_id: String?,    // ADD — bank reference
    val category: String?,
    val event_timestamp: String?,
    val source_signal_id: String?,
    val created_at: String?
    // REMOVE: status, updated_at — do not exist on Supabase
)
```

---

### 2.3 `fyi_events` — 84 rows

**Supabase (15 columns):**
```
event_id, event_type, category, title, description, importance,
status, source_signal_id, duplicate_count, created_at, updated_at,
batch_id, sync_status, is_deleted, deleted_at
```

**Android `FyiEventEntity` (9 fields):**
```kotlin
fyi_event_id, title, summary, category, read_flag, status,
source_signal_id, created_at, updated_at
```

| Column | Supabase | Android | Gap |
|---|---|---|---|
| `event_id` | UUID | ✅ `fyi_event_id` via optString | OK |
| `event_type` | "ACCOUNT_CREDITED" etc. | ❌ Missing | **NEW — event classification type** |
| `category` | "FINANCIAL"/"FAMILY" etc. | ✅ String? | OK |
| `title` | String | ✅ String? | OK |
| `description` | String | ✅ Mapped to `summary` | OK |
| `importance` | "HIGH"/"MEDIUM"/"LOW" | ❌ Missing | **NEW — replaces `read_flag` as priority signal** |
| `status` | "UNREAD"/"READ"/"DISMISSED" | ✅ String? | OK — but see below |
| `source_signal_id` | UUID | ✅ String? | OK |
| `duplicate_count` | Int | ❌ Missing | **NEW — deduplication count** |
| `is_deleted` | Boolean | ❌ Missing | **CRITICAL — must filter `is_deleted=false`** |
| `read_flag` | **NOT IN SUPABASE** | ❌ Android has `read_flag` | **`read_flag` does not exist on Supabase `fyi_events`** |

> [!CAUTION]
> **`read_flag` does not exist on Supabase `fyi_events`!** Android PATCHes `read_flag` but this column is absent. The authoritative read state is `status = "UNREAD"/"READ"`. Android should PATCH `status = "READ"` instead.

> [!WARNING]
> **`importance` field is available** ("HIGH"/"MEDIUM"/"LOW") — this enables priority sorting and badge coloring which currently doesn't exist in FYI screens.

**Recommended `FyiEventEntity` upgrade:**
```kotlin
data class FyiEventEntity(
    @PrimaryKey val fyi_event_id: String,
    val event_type: String?,         // ADD — "ACCOUNT_CREDITED", etc.
    val title: String?,
    val summary: String?,            // mapped from description
    val category: String?,
    val importance: String?,         // ADD — "HIGH"/"MEDIUM"/"LOW"
    val status: String?,             // "UNREAD"/"READ"/"DISMISSED" — use this for read state
    val duplicate_count: Int?,       // ADD — dedup count
    val source_signal_id: String?,
    val created_at: String?,
    val updated_at: String?
    // REMOVE: read_flag — does not exist on Supabase
)
```

---

### 2.4 `daily_briefs` — 5 rows

**Supabase (12 columns):**
```
brief_id, brief_type, generated_at, content, todo_count, fyi_count,
fact_count, batch_id, sync_status, is_deleted, deleted_at, payload_json
```

**Android `DailyBriefEntity` (9 fields):**
```kotlin
id, generatedAt, version, itemsJson, briefType, todoCount, fyiCount,
factCount, payloadJson
```

| Column | Supabase | Android | Gap |
|---|---|---|---|
| `brief_id` | UUID | ✅ `id` via optString | OK |
| `brief_type` | "MORNING"/"EVENING" | ✅ `briefType` | OK |
| `generated_at` | ISO timestamp | ✅ `generatedAt` | OK |
| `content` | String (text or JSON array) | ✅ `itemsJson` via `optString("content")` | OK |
| `todo_count` | Int | ✅ `todoCount` | OK |
| `fyi_count` | Int | ✅ `fyiCount` | OK |
| `fact_count` | Int | ✅ `factCount` | OK |
| `payload_json` | JSON object with rich context | ✅ `payloadJson` | OK |
| `is_deleted` | Boolean | ❌ Missing | Should filter — minor |
| ~~`version`~~ | **NOT IN SUPABASE** | Android has `version` field | Field mapped but Supabase returns default "1.0" via optString |

> [!NOTE]
> `daily_briefs` entity is the **best-aligned** of all entities. The v6 upgrade was correct. Main gap is `is_deleted` filtering.

> [!IMPORTANT]
> `payload_json` sample: `{"target_date": "2026-07-01", "overdue_task_count": 1, "overdue_tasks": [...]}` — this is a rich structured object. `DailyBriefScreen` should parse and render this, not just `itemsJson` (which contains `"Cloud reasoning unavailable"` as a fallback string).

---

### 2.5 `facts` — 22 rows

**Supabase (17 columns):**
```
fact_id, fact_type, fact_value, confidence, status, owner_agent,
source_agent, source_type, first_seen, last_seen, evidence,
created_at, updated_at, batch_id, sync_status, is_deleted, deleted_at
```

**Android `FactInsightEntity` (9 fields):**
```kotlin
id, title, summary, category, priority, created_at, read_flag, status, source
```

| Column | Supabase | Android | Gap |
|---|---|---|---|
| `fact_id` | UUID | ✅ `id` via optString | OK |
| `fact_type` | "BANK_ACCOUNT"/"VEHICLE" etc. | ✅ `title` via optString("fact_type") | OK — but named `title` which is confusing |
| `fact_value` | **JSON object** `{'bank_name': 'HDFC', ...}` | ✅ `summary` via optString | **Gap: `fact_value` is a JSON dict — Android stores as string, not parsed** |
| `confidence` | Float 0.0–1.0 | ❌ Missing | **NEW — confidence badge** |
| `status` | "VERIFIED"/"PENDING"/"DEPRECATED" | ✅ String? | OK |
| `owner_agent` | "FactAgent" | ❌ Missing | Minor |
| `source_agent` | "SignalUnderstandingAgent" | ✅ `source` via optString | OK |
| `source_type` | "OBSERVED"/"INFERRED" | ❌ Missing | NEW |
| `first_seen` | ISO timestamp | ❌ Missing | NEW |
| `last_seen` | ISO timestamp | ❌ Missing | NEW |
| `evidence` | JSON array of signal IDs | ❌ Missing | NEW — for signal trace |
| `created_at` | ISO timestamp | ✅ String? | OK |
| `is_deleted` | Boolean | ❌ Missing | **CRITICAL — must filter** |
| ~~`category`~~ | **NOT IN SUPABASE** | Android has `category` | Field doesn't exist — Android hardcodes "General" via optString fallback |
| ~~`priority`~~ | **NOT IN SUPABASE** | Android has `priority` | Field doesn't exist — Android hardcodes "MEDIUM" |
| ~~`read_flag`~~ | **NOT IN SUPABASE** | Android has `read_flag` | **CRITICAL — field absent. PATCH fails silently** |

> [!CAUTION]
> **THREE fields in `FactInsightEntity` do not exist on Supabase:** `category`, `priority`, and `read_flag`. All three are hardcoded defaults in the sync mapper. The PATCH of `read_flag` silently fails. The `category` and `priority` shown in the UI are fabricated.

> [!WARNING]
> **`fact_value` is a JSON object**, not a plain string. Android stores it as `toString()` which gives something like `{'bank_name': 'HDFC Bank', 'account_last_4': 'Unknown'}`. The FactsScreen displays this raw JSON dict as the summary. This needs to be parsed properly.

**Authoritative fact_type values from live data:**
- `BANK_ACCOUNT` → `{'bank_name': '...', 'account_last_4': '...'}`
- `VEHICLE`, `INSURANCE`, `FAMILY_MEMBER`, etc. (inferred from platform docs)

**Recommended `FactInsightEntity` upgrade:**
```kotlin
data class FactInsightEntity(
    @PrimaryKey val id: String,
    val fact_type: String?,          // "BANK_ACCOUNT"/"VEHICLE"/etc. — was title
    val fact_value_json: String?,    // Raw JSON string of fact_value dict — was summary
    val confidence: Double?,         // ADD — 0.0–1.0
    val status: String?,             // "VERIFIED"/"PENDING"/"DEPRECATED"
    val source_agent: String?,       // was source
    val source_type: String?,        // ADD — "OBSERVED"/"INFERRED"
    val first_seen: String?,         // ADD
    val last_seen: String?,          // ADD
    val evidence_json: String?,      // ADD — JSON array of signal IDs
    val created_at: String?,
    val updated_at: String?,
    val read_flag: Boolean?          // KEEP — Room-only local state, never PATCH to Supabase
    // REMOVE: category, priority — do not exist on Supabase
)
```

---

### 2.6 `financial_facts` — 158 rows (**BIGGEST GAP**)

**Supabase (28 columns):**
```
id, fact_type, financial_event_id, understood_signal_id, qualified_signal_id,
amount, currency, merchant_raw, merchant_canonical, merchant_id, category,
classification_confidence, classification_method, event_date, month,
is_excluded_from_accounting_spend, is_excluded_from_lifestyle_spend,
exclusion_reason, refund_of_fact_id, is_refunded, refund_applied_to_month,
salary_source_id, transfer_pair_id, created_at, batch_id, sync_status,
is_deleted, deleted_at
```

**Android `FinancialInsightEntity` (10 fields):**
```kotlin
id, title, description, type, amount, dueDate, priority, confidence, status, createdAt
```

| Column | Supabase | Android | Gap |
|---|---|---|---|
| `id` | UUID | ✅ String PK | OK |
| `fact_type` | "BILL_PAYMENT_CC"/"EXPENSE_LIFESTYLE" | ✅ `type` via optString | OK |
| `merchant_canonical` | "SBI_CARDS" | ✅ `title` via optString | OK |
| `amount` | Double | ✅ Double? | OK |
| `currency` | "INR" | ❌ Missing | Minor |
| `merchant_raw` | Raw merchant string | ❌ Missing | NEW — original name |
| `category` | "BILL_PAYMENT_CC" etc. | ✅ Constructed in `description` as "Category: X" | **Terrible mapping — category is stuffed into description string** |
| `classification_confidence` | Float 0.0–1.0 | ⚠️ `confidence` via optString | **TYPE MISMATCH — Supabase is Float, Android stores as String** |
| `classification_method` | "fact_type"/"llm" | ❌ Missing | NEW |
| `event_date` | ISO date | ✅ `dueDate` | OK |
| `month` | "2026-03-01" | ❌ Missing | NEW — month bucketing |
| `is_excluded_from_accounting_spend` | Boolean | ❌ Missing | **KEY — determines if included in totals** |
| `is_excluded_from_lifestyle_spend` | Boolean | ❌ Missing | **KEY — lifestyle vs accounting split** |
| `exclusion_reason` | String | ❌ Missing | NEW |
| `is_refunded` | Boolean | ❌ Missing | NEW — refund tracking |
| `refund_of_fact_id` | UUID nullable | ❌ Missing | NEW |
| `transfer_pair_id` | UUID nullable | ❌ Missing | **KEY — links to internal transfers** |
| `salary_source_id` | UUID nullable | ❌ Missing | **KEY — flags salary transactions** |
| ~~`priority`~~ | **NOT IN SUPABASE** | Android hardcodes "MEDIUM" | Field does not exist |
| ~~`status`~~ | **NOT IN SUPABASE** | Android hardcodes "PENDING" | Field does not exist (there's `sync_status` but it's different) |
| `is_deleted` | Boolean | ❌ Missing | **CRITICAL — must filter** |

> [!CAUTION]
> Android only captures **7 of 28 fields** from `financial_facts`. This is the table with the richest intelligence and the worst Android mapping. `is_excluded_from_accounting_spend`, `is_excluded_from_lifestyle_spend`, `transfer_pair_id`, and `salary_source_id` are all key fields for accurate financial display that are completely ignored.

> [!WARNING]
> `confidence` is stored as `String` in Android but is a `Float` (0.0–1.0) in Supabase. The sync reads it via `optString("classification_confidence")` giving a string like `"1.0"` instead of `1.0f`.

---

### 2.7 `fyi_events` Write Path — Status Mapping

Android currently PATCHes `read_flag = true` to mark FYI as read. This field does not exist. The correct approach:

| Action | Android (current) | Should Be |
|---|---|---|
| Mark as read | PATCH `read_flag = true` | PATCH `status = "READ"` |
| Dismiss | PATCH `status = "DISMISSED"` | ✅ Correct |
| Filter active | No filter | Add `?is_deleted=eq.false&status=neq.DISMISSED` |

---

## PART 3 — New Tables Available for Android Upgrade

### 3.1 `monthly_spending_summary` — 3 rows, 16 columns

**Purpose**: Pre-computed monthly financial totals. **Completely replaces** the heuristic income/expense ViewModel calculation.

| Column | Type | Use in Android |
|---|---|---|
| `month_key` | "2026-04" | Display month header |
| `total_debits` | Double | Total money out |
| `total_credits` | Double | Total money in |
| `accounting_spend` | Double | **True expenses** (excl. transfers/investments) |
| `lifestyle_spend` | Double | Discretionary spend |
| `total_income` | Double | **True income** |
| `net_cash_flow` | Double | **Income minus expenses** — the headline number |
| `internal_transfers` | Double | YONO/internal moves (excluded from spend) |
| `insurance_premiums` | Double | Insurance breakdown |
| `investments` | Double | Investment breakdown |
| `refund_offsets` | Double | Net refunds |

**Android implementation**: New `MonthlySpendingSummaryEntity` + `MonthlySpendingDao` + sync in `InsightSyncService`. `FinancialDashboardViewModel` reads latest month from Room instead of aggregating raw events.

---

### 3.2 `monthly_category_spend` — 9 rows, 6 columns

**Purpose**: Spend by category per month. Replaces local groupBy logic.

| Column | Sample Value |
|---|---|
| `month_key` | "2026-04" |
| `category_name` | "EXPENSE_UNCLASSIFIED" |
| `amount` | 275500.0 |
| `transaction_count` | 4 |

---

### 3.3 `monthly_category_trends` — 9 rows, 7 columns

**Purpose**: Month-over-month category trend with `change_percentage`.

| Column | Sample Value |
|---|---|
| `month_key` | "2026-04" |
| `category_name` | "BILL_PAYMENT_CC" |
| `current_amount` | 9800.0 |
| `previous_amount` | 0.0 |
| `change_percentage` | 0.0 |

**Use**: Power the 6-month trend bar chart in Financial screen with authoritative data + show `+15%` delta badges.

---

### 3.4 `salary_cycles` — 1 row, 6 columns (**NEW DISCOVERY**)

**Purpose**: Tracks salary payment cycles. Salary amount: **₹3,00,000/month**.

| Column | Value |
|---|---|
| `salary_date` | 2026-06-21 |
| `salary_amount` | 300000.0 |
| `cycle_start` | 2026-06-21 |
| `cycle_end` | 2026-07-21 |

**Use**: Home screen "Financial Pulse" card can show "₹3L received — cycle ends Jul 21". Replaces guessing income from keyword matching entirely.

---

### 3.5 `transfer_pairs` — 21 rows, 9 columns (**NEW DISCOVERY**)

**Purpose**: Links debit+credit pairs that are internal fund transfers (e.g., YONO self-transfers). These should be excluded from lifestyle spend.

| Column | Value |
|---|---|
| `transfer_type` | "YONO" |
| `amount` | 2000.0 |
| `confidence` | 1.0 |

**Use**: When displaying `financial_facts`, filter out rows where `transfer_pair_id IS NOT NULL` from "expenses" to avoid double-counting transfers.

---

### 3.6 `understood_signals` — 531 rows (**SIGNAL EXPLORER UPGRADE**)

**Purpose**: The output of signal understanding — contains `contract_json` with full extraction details.

**Use**: `SignalExplorerScreen` currently reads from `MobileSignal` (local queue). It could instead trace a `financial_event` → its `source_signal_id` → `understood_signals` row → `contract_json` for full traceability. This is pipeline-internal but safe for debug view.

---

## PART 4 — Critical Sync Bugs to Fix

| # | Bug | Table | Fix |
|---|---|---|---|
| B-1 | `is_deleted` not filtered on any table | All tables | Add `?is_deleted=eq.false` to all `fetchTable()` calls |
| B-2 | `fyi_events.read_flag` PATCH fails (column absent) | `fyi_events` | PATCH `status = "READ"` instead |
| B-3 | `facts.read_flag` PATCH fails (column absent) | `facts` | PATCH `status = "ACKNOWLEDGED"` |
| B-4 | `financial_events.transaction_type` not synced | `financial_events` | Sync `transaction_type` — replaces heuristic |
| B-5 | `todo_items.source_reference` is JSON dict, stored as raw string | `todo_items` | Parse `source_reference['signal_id']` for `source_signal_id` |
| B-6 | `financial_facts.classification_confidence` stored as String | `financial_facts` | Map as `Double` not `String` |
| B-7 | `financial_facts.priority` hardcoded "MEDIUM" | `financial_facts` | Column doesn't exist — remove hardcode |
| B-8 | `financial_facts.status` hardcoded "PENDING" | `financial_facts` | Column doesn't exist — use `sync_status` instead or remove |
| B-9 | `todo_items.priority` "CRITICAL" not handled | `todo_items` | Add CRITICAL to priority handling in UI |
| B-10 | `facts.category` hardcoded "General" | `facts` | Column doesn't exist — derive from `fact_type` in sync mapper |

---

## PART 5 — Upgrade Implementation Plan (Ordered by Impact)

### TIER 1 — Fix Broken Sync (1-2 days)

| Task | Files | Impact |
|---|---|---|
| Add `?is_deleted=eq.false` to all 8 `fetchTable()` calls | `InsightSyncService.kt` | Stops syncing soft-deleted records |
| Fix FYI read: PATCH `status="READ"` not `read_flag` | `FYIRepository.kt` | Fixes silent write failure |
| Fix Facts read: PATCH `status="ACKNOWLEDGED"` | `FactRepository.kt` | Fixes silent write failure |
| Fix `source_reference` JSON parsing for Todos | `InsightSyncService.kt` | Fixes trace IDs showing `{'signal_id': ...}` |
| Fix `classification_confidence` to Double | `InsightSyncService.kt` | Correct confidence display |

### TIER 2 — Add `transaction_type` to FinancialEvent (1 day)

| Task | Files | Impact |
|---|---|---|
| Add `transaction_type` String? to `FinancialEventEntity` | `InsightEntities.kt` | Authoritative income/expense flag |
| Add `paid_to` String? to `FinancialEventEntity` | `InsightEntities.kt` | Actual merchant name |
| Update sync mapper for both fields | `InsightSyncService.kt` | |
| Replace heuristic in `FinancialDashboardViewModel` with `transaction_type == "credit"` | `FinancialDashboardViewModel.kt` | **Eliminates keyword heuristic** |
| Bump Room DB version to 7, add migration | `JarvisDatabase.kt` | |

### TIER 3 — Add 3 Monthly Summary Tables (2-3 days)

| Task | New Entity | Impact |
|---|---|---|
| Create `MonthlySpendingSummaryEntity` (16 fields) | `monthly_spending_summary` | Authoritative financial overview |
| Create `MonthlyCategorySpendEntity` (6 fields) | `monthly_category_spend` | Authoritative category breakdown |
| Create `MonthlyCategoryTrendEntity` (7 fields) | `monthly_category_trends` | Authoritative trend chart |
| Add 3 DAOs, 3 sync blocks in `InsightSyncService` | | |
| Rewrite `FinancialDashboardViewModel` to read from monthly tables | | |

### TIER 4 — Add `salary_cycles` (1 day)

| Task | New Entity | Impact |
|---|---|---|
| Create `SalaryCycleEntity` (6 fields) | `salary_cycles` | Know current salary cycle |
| Add to Home "Financial Pulse" card | `HomeScreen.kt` + `HomeDashboardViewModel` | Show "₹3L salary — N days in cycle" |

### TIER 5 — Enrich Existing Entities (2-3 days)

| Task | Files |
|---|---|
| Add `category` to `TodoEntity` | `InsightEntities.kt` + sync |
| Add `confidence` to `TodoEntity` + `FactInsightEntity` | `InsightEntities.kt` + sync |
| Add `importance` to `FyiEventEntity` (remove `read_flag`) | `InsightEntities.kt` + sync |
| Add `event_type` to `FyiEventEntity` | `InsightEntities.kt` + sync |
| Fix `FactInsightEntity`: remove `category`/`priority`, add `confidence`/`source_type`/`fact_value_json` | `InsightEntities.kt` + sync |
| Upgrade `FinancialInsightEntity`: add `merchant_raw`, `month`, `is_excluded_*`, `transfer_pair_id`, `salary_source_id` | `InsightEntities.kt` + sync |

---

## PART 6 — Room DB Version Roadmap

| DB Version | Changes | Migration |
|---|---|---|
| 6 (current) | `DailyBriefEntity` extended with `briefType`, `todoCount`, etc. | Applied |
| **7** | `FinancialEventEntity`: add `transaction_type`, `paid_to`, `paid_from`, `payment_channel`, `transaction_id`; remove `status`, `updated_at` | `ALTER TABLE financial_events ADD COLUMN ...` |
| **8** | `FyiEventEntity`: add `event_type`, `importance`, `duplicate_count`; remove `read_flag` | `ALTER TABLE fyi_events ADD COLUMN ...` |
| **9** | `FactInsightEntity`: add `confidence`, `source_type`, `first_seen`, `last_seen`, `evidence_json`, `updated_at`; remove `category`, `priority` | `ALTER TABLE facts ADD COLUMN ...` |
| **10** | Add 3 new tables: `monthly_spending_summary`, `monthly_category_spend`, `monthly_category_trends`, `salary_cycles` | Create new tables |
| **11** | `TodoEntity`: add `category`, `source_agent`, `confidence` | `ALTER TABLE todos ADD COLUMN ...` |

> [!WARNING]
> Replace `fallbackToDestructiveMigration()` with explicit `addMigrations()` calls from v7 onwards. Each version should use `ALTER TABLE ADD COLUMN` migrations to preserve cached data.

---

## PART 7 — Tables Flagged as Prohibited (Do Not Expose to Android UI)

| Table | Reason |
|---|---|
| `qualified_signals` (1,004 rows) | Pipeline qualification log — internal only |
| `pipeline_runs` (10 rows) | Pipeline execution history — admin only |
| `system_status` (1 row) | Pipeline health — admin only |
| `processed_files` (24 rows) | File ingestion log — internal only |
| `understood_signals` (531 rows) | Debug only — SignalExplorer trace source |

---

## PART 8 — Empty Tables (Ready for Future Use)

| Table | Status | When to Activate |
|---|---|---|
| `fact_relationships` | 0 rows — table exists | When FactAgent starts generating entity links |
| `merchant_mappings` | 0 rows — table exists | When merchant normalization is enabled |

---

## Summary Scorecard — Before vs After Upgrade

| Dimension | Current | After Upgrade |
|---|---|---|
| Tables synced | 8 of 21 | 12 of 21 (+ 4 new consumer tables) |
| Correct field mappings | ~55% avg across entities | ~90%+ |
| Income/expense classification | ❌ Keyword heuristic | ✅ `transaction_type` field |
| FYI read state | ❌ PATCH non-existent `read_flag` | ✅ PATCH `status` |
| Facts read state | ❌ PATCH non-existent `read_flag` | ✅ PATCH `status` |
| Financial overview data | ❌ Client-computed from raw events | ✅ Pre-computed Supabase aggregates |
| Salary awareness | ❌ Unknown | ✅ `salary_cycles` — ₹3L/month, cycle dates |
| Transfer deduplication | ❌ Counted as expense | ✅ `transfer_pairs` flags internal moves |
| `is_deleted` filtering | ❌ Soft-deleted records synced | ✅ Filtered at API level |
| Fact display quality | ❌ Raw JSON dict string as summary | ✅ Parsed fact_type + fact_value |
| Todo category | ❌ Missing | ✅ Synced from `category` field |
| Confidence badges | ❌ None | ✅ `confidence` on todos, facts, financial_facts |
