# API Architecture & Conventions

Status: owner-confirmed design direction; candidate, not executable contract.

Source: ADR-V3-001/002/005/008/013/014/015; A-002/A-003

Logical adapter → DTO → application use case → domain policy; persistence via port. Reference contracts are application boundary outputs. No new topology or public route.

| Decision | Confirmed design direction |
|---|---|
| K01-D01 | Contract ownership by provider domain |
| K01-D02 | Contract-first design |
| K01-D03 | DTO isolation from domain and entity |
| K01-D04 | Operation-specific contracts |
| K01-D05 | Separate read/write contracts |
| K01-D06 | ADR-V3-014 versioning |
| K01-D07 | Domain-owned authorization |
| K01-D08 | Validated Tenant Context |
| K01-D09 | Minimum authorized disclosure |
| K01-D10 | Contract-only integration |
| K01-D11 | ADR-V3-015 testing |
| K01-D12 | Public API selection remains OD-007 OPEN |

