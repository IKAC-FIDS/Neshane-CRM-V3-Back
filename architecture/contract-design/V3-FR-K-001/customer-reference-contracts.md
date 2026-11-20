# Customer Reference Contracts

Status: owner-confirmed design direction; candidate, not executable contract.

Source: ADR-V3-002/005/008; A-002 lifecycle/relationship rules; A-003 customer-360-requirements.md

CustomerReference, BranchReference, CustomerRelationshipReference, CustomerReferenceSummary, CustomerReferenceValidation, CustomerDependencyCheck. Candidate fields customerId, customerType, displayName, lifecycleStatus subject to authorization and purpose. ResolveCustomerReference/BranchReference/RelationshipReference, ValidateCustomerReference/BranchReference, ResolveCustomerReferences. Active may permit new use; inactive restricts new use; archived historical only. Unknown/unavailable fails closed on new use. Batch limit, freshness, event/cache and race-safe dependency clearance OPEN.

| Decision | Confirmed design direction |
|---|---|
| K06-D01 | Customer authoritative owner |
| K06-D02 | Stable Customer ID |
| K06-D03 | No direct database access |
| K06-D04 | Separate reference/detail |
| K06-D05 | Minimum consumer data |
| K06-D06 | Independent consumer authorization |
| K06-D07 | Validated Tenant Context |
| K06-D08 | Customer/Branch/Relationship references |
| K06-D09 | Bounded batch resolution |
| K06-D10 | No unnecessary duplication |
| K06-D11 | Policy-governed historical snapshots |
| K06-D12 | Preserve related history |
| K06-D13 | Validate new use |
| K06-D14 | No existence disclosure |
| K06-D15 | Unknown validity cannot authorize |
| K06-D16 | Transport/topology independence |
| K06-D17 | Versioning/testing |
| K06-D18 | No consumer implementation authorization |

