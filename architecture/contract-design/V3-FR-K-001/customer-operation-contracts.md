# Customer Operation Contracts

Status: owner-confirmed design direction; candidate, not executable contract.

Source: A-002 customer-operation-catalog.md, CM1-PD-078–106; A-003 customer-search-requirements.md

Logical candidates: CreatePersonCustomer, CreateCompanyCustomer, UpdatePersonCustomer, UpdateCompanyCustomer, ActivateCustomer, DeactivateCustomer, ArchiveCustomer, RestoreCustomer; SearchCustomers, GetCustomerSummary, GetCustomerDetail, GetCustomerForUpdate, GetCustomerHistory, CheckCustomerDuplicates, GetCustomer360; Add/Update/RemoveCustomerContact, SetPrimaryContact, Add/Update/RemoveCustomerAddress, SetPrimaryAddress; Create/Update/Activate/DeactivateBranch, GetBranch, ListCustomerBranches; Create/Update/EndCustomerRelationship, ListCustomerRelationships, ListSelectableRoles, Create/Update/Activate/DeactivateCustomerRole, Hide/UnhideSystemRole; GetVerificationStatus, VerifyCustomer, ReverifyCustomer, RevokeCustomerVerification, List/Upload/Replace/GetCustomerDocument, List/Create/Update/Activate/DeactivateDocumentType. Names are not executable CUS IDs. Archive/restore conditional; restore INACTIVE only; verification and documents conditional on evidence/secure file; no branch transfer. Mandatory durable Audit for mutations, reason as catalog. CUS-CREATE, CUS-VALIDATE, CUS-GET-FOR-UPDATE, CUS-UPDATE retained.

| Decision | Confirmed design direction |
|---|---|
| K03-D01 | Use-case contracts |
| K03-D02 | Separate Person/Company creation and update |
| K03-D03 | Separate lifecycle operations |
| K03-D04 | No general update of sensitive status |
| K03-D05 | Independent contacts/addresses |
| K03-D06 | Independent branches |
| K03-D07 | Independent relationships/roles |
| K03-D08 | Independent verification |
| K03-D09 | Independent documents/types |
| K03-D10 | Preserve approved history |
| K03-D11 | No physical delete or merge |
| K03-D12 | Operation-specific Audit |
| K03-D13 | Independent operation authorization |
| K03-D14 | No generic unrestricted CRUD |
| K03-D15 | Preserve four CUS source IDs without new permission IDs |

