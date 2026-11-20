# DTO, Request & Response

Status: owner-confirmed design direction; candidate, not executable contract.

Source: A-002 field dictionary and A-003 searchable field catalog; ADR-V3-002/008/013/014

Candidate names: CreatePersonCustomerRequest, CreateCompanyCustomerRequest, UpdatePersonCustomerRequest, UpdateCompanyCustomerRequest, CustomerSearchRequest, CustomerSummaryResponse, CustomerDetailResponse, CustomerReferenceResponse. No invented fields or requiredness; per-field schema pending source mapping.

| Decision | Confirmed design direction |
|---|---|
| K02-D01 | Operation-specific DTO |
| K02-D02 | Immutable DTO, Java records where appropriate |
| K02-D03 | Separate request and response |
| K02-D04 | Domain isolation |
| K02-D05 | Entity isolation |
| K02-D06 | Boundary structural and domain business validation |
| K02-D07 | Explicit mapping |
| K02-D08 | Operation-purpose naming |
| K02-D09 | Serialization isolation |
| K02-D10 | Explicit required/optional/nullable semantics |
| K02-D11 | Allowlisted input against overposting |
| K02-D12 | Policy-based response |
| K02-D13 | Separate Person/Company where rules differ |
| K02-D14 | Shared transport-only DTO |
| K02-D15 | ADR-V3-014 compatibility |

