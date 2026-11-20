# Contract Versioning & Testing

Status: owner-confirmed design direction; candidate, not executable contract.

Source: ADR-V3-014/015; contract-change-protocol.md

Semantic versioning; REST major URI if REST selected. Additive optional fields only when semantics compatible; enum expansion requires consumer assessment. Breaking removal/rename/type/default/meaning/authorization changes require governed review. Provider conformance, compatibility, risk-driven consumer and actual-path integration, manual review for unsupported constructs; no test execution. Record coexistence/retirement and approved consumers; public selection OD-007 OPEN.

| Decision | Confirmed design direction |
|---|---|
| K09-D01 | ADR-V3-014 versioning |
| K09-D02 | Independent contract version |
| K09-D03 | Version history |
| K09-D04 | Structural and semantic compatibility |
| K09-D05 | Governed breaking change |
| K09-D06 | Consumer inventory |
| K09-D07 | Deprecation and migration |
| K09-D08 | ADR-V3-015 testing |
| K09-D09 | Provider conformance gate |
| K09-D10 | Risk-based consumer tests |
| K09-D11 | Risk-based real integration |
| K09-D12 | Positive/negative/fault tests |
| K09-D13 | Tenant/disclosure tests |
| K09-D14 | Concurrency/idempotency tests |
| K09-D15 | Traceable evidence |
| K09-D16 | No unexecuted PASS |
| K09-D17 | Tool selection separate |
| K09-D18 | No publication without gate/authority |

