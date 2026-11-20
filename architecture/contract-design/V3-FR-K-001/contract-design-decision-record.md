# V3-FR-K-001 decision record

Farzad confirmed the ten groups in conversation on 2026-11-20 (Asia/Tehran); issuance time not supplied. Owner confirmation is bounded design direction, not effective architecture modification, IAM grant, or publication authority.

| ID | Direction | Source group |
|---|---|---|
| K01-D01 | Contract ownership by provider domain | ADR-V3-001/002/005/008/013/014/015; A-002/A-003 |
| K01-D02 | Contract-first design | ADR-V3-001/002/005/008/013/014/015; A-002/A-003 |
| K01-D03 | DTO isolation from domain and entity | ADR-V3-001/002/005/008/013/014/015; A-002/A-003 |
| K01-D04 | Operation-specific contracts | ADR-V3-001/002/005/008/013/014/015; A-002/A-003 |
| K01-D05 | Separate read/write contracts | ADR-V3-001/002/005/008/013/014/015; A-002/A-003 |
| K01-D06 | ADR-V3-014 versioning | ADR-V3-001/002/005/008/013/014/015; A-002/A-003 |
| K01-D07 | Domain-owned authorization | ADR-V3-001/002/005/008/013/014/015; A-002/A-003 |
| K01-D08 | Validated Tenant Context | ADR-V3-001/002/005/008/013/014/015; A-002/A-003 |
| K01-D09 | Minimum authorized disclosure | ADR-V3-001/002/005/008/013/014/015; A-002/A-003 |
| K01-D10 | Contract-only integration | ADR-V3-001/002/005/008/013/014/015; A-002/A-003 |
| K01-D11 | ADR-V3-015 testing | ADR-V3-001/002/005/008/013/014/015; A-002/A-003 |
| K01-D12 | Public API selection remains OD-007 OPEN | ADR-V3-001/002/005/008/013/014/015; A-002/A-003 |
| K02-D01 | Operation-specific DTO | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D02 | Immutable DTO, Java records where appropriate | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D03 | Separate request and response | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D04 | Domain isolation | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D05 | Entity isolation | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D06 | Boundary structural and domain business validation | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D07 | Explicit mapping | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D08 | Operation-purpose naming | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D09 | Serialization isolation | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D10 | Explicit required/optional/nullable semantics | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D11 | Allowlisted input against overposting | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D12 | Policy-based response | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D13 | Separate Person/Company where rules differ | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D14 | Shared transport-only DTO | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K02-D15 | ADR-V3-014 compatibility | A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014 |
| K03-D01 | Use-case contracts | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D02 | Separate Person/Company creation and update | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D03 | Separate lifecycle operations | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D04 | No general update of sensitive status | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D05 | Independent contacts/addresses | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D06 | Independent branches | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D07 | Independent relationships/roles | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D08 | Independent verification | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D09 | Independent documents/types | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D10 | Preserve approved history | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D11 | No physical delete or merge | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D12 | Operation-specific Audit | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D13 | Independent operation authorization | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D14 | No generic unrestricted CRUD | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K03-D15 | Preserve four CUS source IDs without new permission IDs | A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md |
| K04-D01 | Authorized field allowlist | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D02 | Exact identifier/phone/email | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D03 | Exact/prefix/contains name | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D04 | AND between fields OR within values | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D05 | ACTIVE default | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D06 | Persian/English normalization | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D07 | Preserve original data | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D08 | Last-change default sort | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D09 | Customer ID tie-break | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D10 | Cursor pagination | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D11 | 20/50/100 default 20 | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D12 | Authorized matches/count only | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D13 | Context-bound validated cursor | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D14 | No fuzzy/intelligent search | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K04-D15 | Engine independence; OD-018 OPEN | A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md |
| K05-D01 | Multilevel access | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D02 | Backend enforcement | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D03 | FULL/MASKED/HIDDEN | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D04 | Per-field policy | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D05 | Deny by default | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D06 | Never send unauthorized data to frontend | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D07 | Separate search and display grants | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D08 | No count/filter/sort leakage | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D09 | Independent linked resource authorization | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D10 | Independent history | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D11 | Independent documents/evidence | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D12 | Separate business history and Audit | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D13 | Minimize sensitive logs | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D14 | No existence disclosure | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D15 | Customer owns disclosure | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K05-D16 | No IAM grants | A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md |
| K06-D01 | Customer authoritative owner | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D02 | Stable Customer ID | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D03 | No direct database access | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D04 | Separate reference/detail | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D05 | Minimum consumer data | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D06 | Independent consumer authorization | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D07 | Validated Tenant Context | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D08 | Customer/Branch/Relationship references | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D09 | Bounded batch resolution | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D10 | No unnecessary duplication | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D11 | Policy-governed historical snapshots | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D12 | Preserve related history | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D13 | Validate new use | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D14 | No existence disclosure | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D15 | Unknown validity cannot authorize | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D16 | Transport/topology independence | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D17 | Versioning/testing | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K06-D18 | No consumer implementation authorization | ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md |
| K07-D01 | Multilayer validation | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D02 | Backend mandatory | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D03 | Structural versus business validation | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D04 | Standard error structure | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D05 | Stable codes | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D06 | RFC 9457 conditional REST | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D07 | Field-level validation errors | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D08 | i18n | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D09 | No stack/internal details | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D10 | Correlation ID | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D11 | Warnings distinct from errors | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D12 | Enumeration safety | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D13 | No sensitive values in errors | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D14 | Temporary versus definitive errors | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D15 | No override of invariants | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D16 | No definitive success for unknown outcome/Audit | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K07-D17 | Transport-independent logical errors | A-002 operation catalog and failure policies; A-003 audit-failure-policies.md; ADR-V3-008/014 |
| K08-D01 | Optimistic concurrency default | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D02 | Version mutable resources | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D03 | Prevent lost updates | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D04 | No automatic overwrite | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D05 | Revalidate sensitive rules at final commit | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D06 | Idempotency key selected mutations | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D07 | Prevent repeated business effects | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D08 | Same key different payload conflict | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D09 | Tenant/actor/operation binding | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D10 | Reauthorize replay | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D11 | No blind retry unknown result | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D12 | No duplicate business Audit | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D13 | Atomicity within domain boundary | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D14 | Storage independence | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D15 | Preserve Audit policy | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D16 | No exactly-once delivery claim | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D17 | TTL/retry/recovery OPEN | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K08-D18 | Transport independence | A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008 |
| K09-D01 | ADR-V3-014 versioning | ADR-V3-014/015; contract-change-protocol.md |
| K09-D02 | Independent contract version | ADR-V3-014/015; contract-change-protocol.md |
| K09-D03 | Version history | ADR-V3-014/015; contract-change-protocol.md |
| K09-D04 | Structural and semantic compatibility | ADR-V3-014/015; contract-change-protocol.md |
| K09-D05 | Governed breaking change | ADR-V3-014/015; contract-change-protocol.md |
| K09-D06 | Consumer inventory | ADR-V3-014/015; contract-change-protocol.md |
| K09-D07 | Deprecation and migration | ADR-V3-014/015; contract-change-protocol.md |
| K09-D08 | ADR-V3-015 testing | ADR-V3-014/015; contract-change-protocol.md |
| K09-D09 | Provider conformance gate | ADR-V3-014/015; contract-change-protocol.md |
| K09-D10 | Risk-based consumer tests | ADR-V3-014/015; contract-change-protocol.md |
| K09-D11 | Risk-based real integration | ADR-V3-014/015; contract-change-protocol.md |
| K09-D12 | Positive/negative/fault tests | ADR-V3-014/015; contract-change-protocol.md |
| K09-D13 | Tenant/disclosure tests | ADR-V3-014/015; contract-change-protocol.md |
| K09-D14 | Concurrency/idempotency tests | ADR-V3-014/015; contract-change-protocol.md |
| K09-D15 | Traceable evidence | ADR-V3-014/015; contract-change-protocol.md |
| K09-D16 | No unexecuted PASS | ADR-V3-014/015; contract-change-protocol.md |
| K09-D17 | Tool selection separate | ADR-V3-014/015; contract-change-protocol.md |
| K09-D18 | No publication without gate/authority | ADR-V3-014/015; contract-change-protocol.md |
| K10-D01 | Record all confirmed decisions | Roadmap K-001 AC-01–05; task-governance.md |
| K10-D02 | Trace to A-002/A-003 | Roadmap K-001 AC-01–05; task-governance.md |
| K10-D03 | Trace to approved ADRs | Roadmap K-001 AC-01–05; task-governance.md |
| K10-D04 | Preserve OPEN and limits | Roadmap K-001 AC-01–05; task-governance.md |
| K10-D05 | Provider/consumer risk matrix | Roadmap K-001 AC-01–05; task-governance.md |
| K10-D06 | Acceptance matrix | Roadmap K-001 AC-01–05; task-governance.md |
| K10-D07 | Actual evidence status | Roadmap K-001 AC-01–05; task-governance.md |
| K10-D08 | SHA-256 manifest | Roadmap K-001 AC-01–05; task-governance.md |
| K10-D09 | Backend registration only after separate authorization | Roadmap K-001 AC-01–05; task-governance.md |
| K10-D10 | Separate design and runtime acceptance | Roadmap K-001 AC-01–05; task-governance.md |

