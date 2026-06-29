# Workspace Rules: JARVIS Android App & Platform v1.0

All agents working on this workspace MUST adhere to:
1. The Android specification in [ANDROID_MASTER_INSTRUCTIONS.md](file:///c:/jarvis/jarviscollector/ANDROID_MASTER_INSTRUCTIONS.md)
2. The platform architecture baseline in [JARVIS_PLATFORM_ANCHOR_DOCUMENT.md](file:///c:/jarvis/jarviscollector/JARVIS_PLATFORM_ANCHOR_DOCUMENT.md)

## Core Directives

1. **Presentation Layer Only**: The Android application SHALL NOT perform AI reasoning, LLM execution, classification logic, financial calculations, or generate facts/todos/briefs. It is strictly a reader/updater for Supabase.
2. **Single Source of Truth**: All data and logic configurations are authoritative on remote Supabase.
3. **Design Standard**: The UI must emulate premium experiences like Microsoft Copilot, Microsoft Fabric, Modern Banking Apps, or Google Discover. Avoid looking like a basic CRUD database viewer.
4. **Navigation**: Maintain a standard bottom navigation bar (Home, Brief, Tasks, FYI, Profile) with home access on every screen.
5. **No Local aggregation/storage**: Use cached data strictly for offline reading, syncing changes back to Supabase.
6. **No API Secrets**: Never store keys/credentials in application code.

## Platform Governance & Engineering Rules

1. **Pipeline Integrity**: Never bypass the canonical sequence (Consumer → Qualification → Signal Understanding → Financial Agent → Aggregation Service → Fact Agent → Todo Agent → FYI Agent → Daily Brief Agent).
2. **Reuse Before Create**: Search existing codebase/scripts before creating new items. Extend existing functions/classes.
3. **Test Governance**: Maintain a minimal, curated test suite. Do not accumulate versioned or duplicate test files (e.g. `_v2.py`, `_new.py`, `_final.py`).
4. **Script Governance**: No dead or ownerless scripts. Remove or archive unused/deprecated scripts.

