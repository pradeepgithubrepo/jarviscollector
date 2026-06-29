# JARVIS PLATFORM ANCHOR DOCUMENT v1.0

## Purpose

This document defines the current state of the Jarvis platform.

All future development, bug fixes, Android implementation, Streamlit implementation, and agent enhancements must align with this document.

This document serves as the authoritative baseline for the platform.

---

# 1. Core Mission

Jarvis is a personal intelligence platform that converts raw consumer signals into actionable intelligence.

The objective is not signal storage.

The objective is:

* Understand
* Classify
* Learn
* Remember
* Prioritize
* Notify

for the user.

---

# 2. Canonical Processing Pipeline

The approved pipeline is:

Consumer
→ Qualification
→ Signal Understanding
→ Financial Agent
→ Aggregation Service
→ Fact Agent
→ Todo Agent
→ FYI Agent
→ Daily Brief Agent

No future implementation shall bypass this sequence without explicit architectural approval.

---

# 3. Agent Status

Consumer
Status: LOCKED

Qualification Agent
Status: LOCKED

Signal Understanding Agent
Status: LOCKED

Financial Agent
Status: LOCKED

Aggregation Service
Status: LOCKED

Fact Agent
Status: LOCKED

Todo Agent
Status: LOCKED

FYI Agent
Status: LOCKED

Daily Brief Agent
Status: LOCKED

---

# 4. Qualification Rules

Qualification Agent shall not use LLMs.

Qualification Agent shall operate using deterministic logic only.

Output Categories:

* QUALIFIED
* REVIEW
* REJECTED

No future implementation shall introduce LLM calls into Qualification without explicit approval.

---

# 5. Signal Understanding Rules

Signal Understanding Agent is responsible for:

* Domain detection
* Intent classification
* Amount extraction
* Merchant extraction
* Entity normalization
* Contract generation

Mandatory coverage:

* Amount Extraction = 100%
* Merchant Extraction = 100%

Signal Understanding is the canonical contract producer.

---

# 6. Financial Rules

Financial classification rule:

# Money moved

Financial

# No money moved

Not Financial

Financial Agent owns:

* Debit
* Credit
* Refund

classification.

No downstream agent shall modify financial direction.

---

# 7. Fact Rules

Fact Agent owns long-term memory.

Examples:

* Family members
* Insurance policies
* Accounts
* Vehicles
* Contacts

Facts must be durable.

Facts must not be duplicated.

Facts must be normalized.

Fact Agent is the only owner of memory creation.

---

# 8. Todo Rules

Todo Agent does not determine meaning.

Todo Agent determines actionability.

Golden Rule:

If the user ignores this signal, will something bad happen?

YES
→ TODO

NO
→ NOT TODO

Examples:

TODO

* Insurance renewal
* Appointment reminder
* School event

NOT TODO

* Salary credit
* Refund received
* Successful payment
* Balance update
* OTP

---

# 9. FYI Rules

FYI contains information worth knowing but not requiring action.

FYI must never contain:

* Todos
* Facts
* Financial calculations

FYI is informational only.

---

# 10. Daily Brief Rules

Daily Brief is the executive summary layer.

Daily Brief consumes:

* Facts
* Todos
* FYI
* Financial Summary

Daily Brief does not create new intelligence.

It summarizes existing intelligence.

---

# 11. Source Of Truth

Remote Supabase is the authoritative source.

All clients shall consume from Supabase.

Examples:

* Android
* Streamlit
* Future Web Portal

No client shall become a source of truth.

---

# 12. User Feedback Loop

Users must be able to:

* Correct merchants
* Correct categories
* Correct todos
* Correct facts

Corrections must be stored centrally.

Future intelligence improvements must consume correction history.

---

# 13. Engineering Principles

Reuse before create.

Before creating any new component, script, validator, utility, test, migration, or service:

Step 1:
Search existing implementation.

Step 2:
Evaluate suitability.

Step 3:
Extend if possible.

Step 4:
Create new only if no reusable implementation exists.

Duplication is prohibited unless justified.

---

# 14. Test Asset Governance

The platform shall maintain a minimal and curated test suite.

Rules:

Do not create duplicate test scripts.

Do not create versioned copies of tests.

Do not create:

test_v2.py
test_new.py
test_final.py
test_latest.py
test_fixed.py

If a test is obsolete:

* Remove it
* Replace it
* Update references

Do not retain unused tests.

---

# 15. Script Governance

The platform shall not accumulate dead scripts.

Every script must have a purpose.

Every script must have an owner.

Every script must have a known execution path.

Unused scripts shall be removed.

Deprecated scripts shall be archived or deleted.

Repository hygiene is mandatory.

---

# 16. Validation Philosophy

Every enhancement must satisfy:

* Accuracy
* Determinism
* Traceability
* Explainability

Validation must reuse existing datasets whenever possible.

Creation of new datasets requires justification.

---

# 17. Android Platform Rules

Android is a thin client.

Android shall:

* Read
* Display
* Update

Android shall not:

* Run AI
* Run LLMs
* Recreate agent logic
* Recalculate intelligence

Supabase remains authoritative.

---

# 18. Success Definition

Jarvis succeeds when a user can determine:

* What requires action
* What changed
* What should be remembered
* How finances look today

within seconds of opening the application.

This remains the primary objective of the platform.
