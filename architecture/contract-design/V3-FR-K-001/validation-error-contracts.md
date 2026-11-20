# Validation & Error Contracts

Status: owner-confirmed design direction; candidate, not executable contract.

Source: A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014

Logical categories VALIDATION_ERROR, AUTHENTICATION_ERROR, AUTHORIZATION_ERROR, RESOURCE_NOT_FOUND, BUSINESS_RULE_VIOLATION, RESOURCE_CONFLICT, CONCURRENCY_CONFLICT, DEPENDENCY_UNAVAILABLE, INTERNAL_ERROR. Conditional REST mapping 400 malformed, 401 unauthenticated, 403 safe denial, 404 absent/undisclosable, 409 conflict, 412 failed version precondition, 422 semantic input, 429 limit, 500 unexpected, 503 unavailable. Exact precedence and safe response equivalence OPEN. Stable code and field path; translated client-safe message; correlation ID; no raw values. Warning only permits approved exception with authority/reason; definitive conflict cannot override.

| Decision | Confirmed design direction |
|---|---|
| K07-D01 | Multilayer validation |
| K07-D02 | Backend mandatory |
| K07-D03 | Structural versus business validation |
| K07-D04 | Standard error structure |
| K07-D05 | Stable codes |
| K07-D06 | RFC 9457 conditional REST |
| K07-D07 | Field-level validation errors |
| K07-D08 | i18n |
| K07-D09 | No stack/internal details |
| K07-D10 | Correlation ID |
| K07-D11 | Warnings distinct from errors |
| K07-D12 | Enumeration safety |
| K07-D13 | No sensitive values in errors |
| K07-D14 | Temporary versus definitive errors |
| K07-D15 | No override of invariants |
| K07-D16 | No definitive success for unknown outcome/Audit |
| K07-D17 | Transport-independent logical errors |

