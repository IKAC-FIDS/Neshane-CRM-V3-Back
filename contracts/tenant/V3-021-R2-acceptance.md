\## V3-021 — R2 Architecture and Contract Design Acceptance Record



**Task:** V3-021 — Tenant Model and Stable Identity

**Phase / Gate:** P02 / R2

**Architecture Baseline:** NESHANE-V3-ARCH-P00-005

**Decision:** ACCEPTED — LIMITED R2 ARCHITECTURE AND CONTRACT DESIGN BASELINE

**Acceptance Authority:** Technical Architecture Owner

**Acceptance Source:** Explicit Technical Architecture Owner approval recorded in the architecture review conversation

**Implementation Commit:** `cec3bc4505087a9d07532cb3ca71946fa0dfdbfe`

**Engineering Evidence Commit:** `18843e7`

**Hosted CI Run:** https://github.com/IKAC-FIDS/Neshane-CRM-V3-Back/actions/runs/42118709151



\### 1. Accepted Design Scope



The Technical Architecture Owner accepts the bounded V3-021 architecture and contract design, aligned with the accepted V3-020 control-plane ownership matrix and applicable ADRs.



The accepted scope includes:



1\. Immutable, canonical lowercase UUIDv4 Tenant identity independent of deployment topology.

2\. Registry collision rejection, tombstone retention and non-reuse invariants.

3\. Five-state Tenant lifecycle with governed transitions, optimistic concurrency and idempotent command handling.

4\. Separation of authoritative placement generation from local activation version and placement history.

5\. Explicit Registry, Lifecycle and Placement Authority boundaries across Shared SaaS, Dedicated Managed and Dedicated Sovereign.

6\. Preservation of Sovereign-local authority without implicit Global Control Plane administrative rights.

7\. Technical suspension semantics with fail-closed ordinary tenant-scoped operations and separately authorized exceptions.

8\. Versioned tenant contract definitions and deterministic positive and negative policy tests.



\### 2. Engineering Verification



Local verification completed successfully:



\- Tenant tests: 35 passed; zero failures, errors or skips.

\- Backend build and check: PASS.

\- Existing HTTP and Event compatibility verification: PASS.

\- Tenant candidate contract validation: PASS.

\- Architecture boundary and security-policy verification: PASS.



The referenced implementation commit successfully completed all four mandatory hosted Pull Request CI checks.



\### 3. Acceptance Boundary



This decision accepts the V3-021 design baseline within the stated scope.



It does not establish:



\- Runtime Tenant Context enforcement or PostgreSQL RLS compliance.

\- Production token validation, authentication or Domain authorization.

\- Durable Registry persistence or distributed collision coordination.

\- Cross-cell migration, handover, dual-running or rollback implementation.

\- Runtime suspension propagation or numeric freshness guarantees.

\- Independent security approval.

\- V3-016 repository governance completion.

\- Production readiness, deployment, promotion or merge authorization.



Existing Open Decisions remain open and unchanged.



\### 4. Contract Governance



The reviewed V3-021 contract content constitutes the accepted R2 **design** baseline.



The repository's `proposed-r2` manifest remains a candidate implementation artifact until a separately governed baseline-publication decision updates its status and applicable compatibility controls.



No contract may be silently rewritten or promoted to a runtime-consumable accepted baseline solely because of this R2 decision.



Any future change affecting Tenant identity, lifecycle, authority, placement or suspension semantics requires compatibility assessment and the applicable architecture review.



\### 5. Remaining Governance Conditions



V3-016 independent reviewer and security-authority requirements remain unresolved.



No independent approval or separation-of-duties compliance is claimed.



Hosted CI success does not replace independent review, security sign-off or repository protection requirements.



\### 6. R2 Disposition



**V3-021 — CLOSED — R2 ARCHITECTURE/CONTRACT DESIGN BASELINE ACCEPTED**



**Implementation and release gates:** PENDING.



This acceptance is limited to the specified architecture and contract design scope and does not authorize merging into the protected main branch or promoting artifacts to production.
