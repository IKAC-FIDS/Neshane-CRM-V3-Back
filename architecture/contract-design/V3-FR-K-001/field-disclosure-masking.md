# Field-Level Disclosure & Masking

Status: owner-confirmed design direction; candidate, not executable contract.

Source: A-002 CM1-PD-096/103; A-003 D11–16; access-disclosure-matrix.md

Tenant → actor/operation → resource → field → authorized projection. Ordinary authorized read: ID/name/type/lifecycle/verification status; official IDs masked unless independently authorized; contact/address subject to field rules; Branch/Relationship independently authorized; documents/evidence/history/Audit independently gated. Unapproved masking pattern blocks affected disclosure. Hidden attributes do not affect counts, sorting, facets, errors or metadata.

| Decision | Confirmed design direction |
|---|---|
| K05-D01 | Multilevel access |
| K05-D02 | Backend enforcement |
| K05-D03 | FULL/MASKED/HIDDEN |
| K05-D04 | Per-field policy |
| K05-D05 | Deny by default |
| K05-D06 | Never send unauthorized data to frontend |
| K05-D07 | Separate search and display grants |
| K05-D08 | No count/filter/sort leakage |
| K05-D09 | Independent linked resource authorization |
| K05-D10 | Independent history |
| K05-D11 | Independent documents/evidence |
| K05-D12 | Separate business history and Audit |
| K05-D13 | Minimize sensitive logs |
| K05-D14 | No existence disclosure |
| K05-D15 | Customer owns disclosure |
| K05-D16 | No IAM grants |

