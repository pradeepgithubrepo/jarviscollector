# JARVIS ANDROID APP v1.0

## Master Architecture & Design Instruction Set

---

# 1. Core Principle

The Android application is a presentation layer only.

The application SHALL NOT:

* Perform AI reasoning
* Run LLMs
* Execute classification logic
* Execute financial calculations
* Generate facts
* Generate todos
* Generate daily briefs

The application SHALL:

* Read data from Supabase
* Display data professionally
* Allow user actions
* Sync changes back to Supabase

---

# 2. Source Of Truth

Single source of truth:

```text
SUPABASE
```

Android is a consumer.

Streamlit is a consumer.

Future web portal is a consumer.

All clients must read from the same backend tables.

No local business logic duplication is allowed.

---

# 3. Jarvis Intelligence Pipeline

The following pipeline exists outside Android:

```text
Consumer
→ Qualification
→ Signal Understanding
→ Financial Agent
→ Aggregation Service
→ Fact Agent
→ Todo Agent
→ FYI Agent
→ Daily Brief Agent
```

Android must never recreate this pipeline.

Android only consumes outputs.

---

# 4. Design Philosophy

The application must feel like:

```text
Microsoft Copilot
Microsoft Fabric
Modern Banking App
Google Discover
```

The application must NOT feel like:

```text
Student Project
CRUD Application
Admin Portal
Database Viewer
```

---

# 5. Navigation Standards

Every screen must support returning to Home.

Mandatory Home access:

```text
Home Icon
```

visible on every page.

Preferred navigation:

```text
Bottom Navigation Bar
```

Items:

```text
Home
Brief
Tasks
FYI
Profile
```

Secondary navigation:

```text
Finance
Facts
Settings
```

---

# 6. Home Screen

Home screen is the primary experience.

Display:

```text
Good Morning <User>

Pending Actions
FYI Updates
Net Cashflow
Today's Summary
```

Use cards.

Never use tables.

---

# 7. Daily Brief Experience

Daily Brief is the flagship feature.

---

## Morning Modal

On first open each day:

Display modal popup:

```text
Good Morning

Today's Priorities

Important Actions

Financial Highlights

Key Updates
```

Actions:

```text
View Full Brief
Dismiss
```

---

## Full Brief Layout

Sections:

```text
Priority Actions

Financial Snapshot

Family Updates

Important Updates

Insights
```

Use scrollable cards.

---

# 8. Notification Standards

Notifications are mandatory.

---

## Daily Brief Notification

Time:

```text
08:00 AM
```

Example:

```text
You have 3 pending actions today.
```

Opening notification launches Daily Brief.

---

## Critical Todo Notification

Examples:

```text
Insurance Expiry

Appointment Tomorrow

School Event
```

Push immediately.

---

## FYI Notifications

Disabled by default.

User configurable.

---

# 9. Todo Management

Todos must appear as cards.

Each card contains:

```text
Title

Description

Due Date

Priority
```

Actions:

```text
Complete

Delete

Edit
```

---

## Completion

Completing a todo updates Supabase immediately.

No local-only state allowed.

---

# 10. FYI Experience

FYI is informational only.

Categories:

```text
Financial

Family

Personal

Travel

General
```

Display:

```text
Card Layout
```

No action buttons.

---

# 11. Fact Store

Facts represent Jarvis memory.

Display categories:

```text
Family

Financial

Vehicle

Contacts

Insurance

Accounts
```

Display as cards.

Never display raw JSON.

Never expose internal identifiers.

---

# 12. Finance Experience

Finance page is executive level.

Landing KPIs:

```text
Money In

Money Out

Net Position

Refunds

Recurring Payments
```

---

## Finance Drilldown

Allow navigation:

```text
Category
→ Merchant
→ Transactions
```

Raw transactions must be deepest level.

Never show them first.

---

# 13. Supabase Rules

All data must come from Supabase.

No SQLite business storage.

No local financial aggregation.

No local fact generation.

Android acts as:

```text
Reader
Updater
```

only.

---

# 14. Offline Behaviour

When offline:

Show cached data.

Mark clearly:

```text
Last Synced
```

Upon reconnection:

Auto refresh.

Supabase remains authoritative.

---

# 15. Security Rules

Android must never store:

```text
API Keys

LLM Keys

Supabase Service Keys
```

Use authenticated APIs only.

No secrets inside application code.

---

# 16. User Feedback Loop

Users must be able to correct Jarvis.

Examples:

```text
Wrong Merchant

Wrong Category

Wrong Todo

Wrong Fact
```

Feedback stored in Supabase.

Future AI pipeline consumes corrections.

---

# 17. Performance Standards

App launch:

```text
< 2 seconds
```

Home load:

```text
< 3 seconds
```

Navigation:

```text
Instant
```

Scrolling:

```text
60 FPS
```

---

# 18. Technology Standards

Mandatory:

```text
Kotlin

Jetpack Compose

Material 3

Navigation Compose

ViewModel

Repository Pattern

Supabase SDK

WorkManager

Firebase Cloud Messaging
```

Avoid:

```text
XML UI

Legacy Fragments

Business Logic in UI
```

---

# 19. Version 1 Scope

Must ship:

```text
Authentication

Home

Daily Brief

Todo

FYI

Facts

Finance Summary

Notifications

Supabase Sync
```

Do not build:

```text
Chat Interface

Voice Assistant

On-device AI

Complex Analytics
```

---

# 20. Success Criteria

The Android app is successful when:

```text
A user can open the app
and within 10 seconds understand:

What needs action
What changed
How finances look
What is important today
```

without navigating through multiple screens.
