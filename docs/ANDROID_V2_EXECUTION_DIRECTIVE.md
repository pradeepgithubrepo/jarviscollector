# JARVIS ANDROID V2 EXECUTION DIRECTIVE

## Architectural Laws

1. **Law #1**: Supabase is the only business source of truth. Never violate this rule. Authoritative ownership is on Supabase; Android is non-authoritative.
2. **Law #2**: Android is a Signal Collector + Thin Client + Notification Surface. It is NOT a Fact, Financial, Todo, FYI, or Daily Brief Generator. No intelligence originates on Android.
3. **Law #3**: Only SMS Capture, Notification Capture, WhatsApp Capture, Upload Queue, and Sync State may originate locally. Everything else originates remotely.
4. **Law #4**: Room is not a source of truth. Room is strictly a CACHE, QUEUE, or SYNC STATE.
5. **Law #5**: REUSE FIRST, EXTEND SECOND, CREATE THIRD. Check existing files before creating any new components, ViewModels, or Utilities. Do not use suffix duplicates (e.g. `_v2`, `_new`).
6. **Law #6**: Delete dead code. Do not archive or leave commented-out obsolete code blocks.
7. **Law #7**: Home is the command center. Every user journey starts and ends at Home. Every screen must support returning Home in one tap.
8. **Law #8**: Standardized navigation tab bar: Home, Brief, Tasks, FYI, Profile. Facts and Finance are secondary targets accessed from Home.
9. **Law #9**: Screen consolidation is mandatory. Do not create separate category files (e.g. FamilyScreen, SchoolScreen). Implement a unified `FyiCategoryScreen` parameterized by category.
10. **Law #10**: Facts are first-class citizens. They are not optional and must be implemented as a core feature.
11. **Law #11**: Notification scheduling, delivery, and deep-link routing belong to Android. Notification content, prioritizing, and intelligence generation belong to Supabase.
12. **Law #12**: Todo updates must be Remote First (Update Supabase → Success → Update Room). If remote fails, Room must not become authoritative.

## Build Sequence

1. **Phase 1 (Navigation Foundation)**: Navigation Compose, Bottom Navigation bar, Route Registry, Back stack, and Home routing.
2. **Phase 2 (Screen Consolidation)**: Remove duplicate category screens (Family, School, Travel, Health, Shopping) and build the single parameterized `FyiCategoryScreen`.
3. **Phase 3 (Facts Foundation)**: Facts DAO, Facts Repository, Facts sync, and Facts Screen.
4. **Phase 4 (Finance Redesign)**: Implement Overview $\rightarrow$ Category $\rightarrow$ Transactions drilldown.
5. **Phase 5 (Notification Modernization)**: Integrate Daily Brief notifications, Todo reminders, and deep-link routing.
6. **Phase 6 (Hardening)**: Validate offline queue, sync consistency, and verify thin client compliance exceeds 95%.
