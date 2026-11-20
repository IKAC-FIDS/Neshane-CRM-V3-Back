# Customer Application Module

## Scope

AP-01 and AP-02 establish an isolated Customer Application
foundation using existing Customer Domain candidate rules.

## Components

- AssessContactCandidateUseCase: internal input port.
- AssessContactCandidateQuery: immutable assessment input.
- AssessContactCandidate: isolated use case implementation.
- AssessContactCandidateResult: immutable application result.

## Dependencies

Application -> Customer Domain

No reverse dependency is permitted.

## Output ports

No output port is currently required.

Persistence, IAM, Audit, event publishing and external
validation ports are deferred until a separately authorized
use case requires them.

## Result semantics

- PASS: supplied declarative rules are satisfied.
- FAIL: supplied declarative rules are not satisfied.
- BLOCKED: a supported assessment cannot be established.

PASS is not identity verification, authorization, or
permission to register or modify a customer.

## Verification

Executable synthetic application specifications cover
result mapping, negative inputs, immutability, redaction,
scope mismatch and deterministic behavior.

## Limitations

No REST API, transport DTO, database, Spring runtime,
trusted IAM, production data, external adapter or
runtime acceptance is established.

Existing governance gates and open decisions remain unchanged.

## AP-03 — Customer Registration Candidate

Components:

- RegisterCustomerCandidateCommand
- RegisterCustomerCandidateUseCase
- RegisterCustomerCandidate
- RegisterCustomerCandidateResult

The use case coordinates existing CustomerRegistration
domain rules without changing domain behavior.

Supported outcomes:

- ELIGIBLE_CANDIDATE
- BLOCKED

Audit requirements are propagated to the application result.
Known result codes are retained; unrecognized codes are
mapped to a generic blocked result.

No durable customer registration, real IAM, persistence,
external validation or audit execution is implemented.

Registration inputs and test scenarios are synthetic.

## AP-04 - Customer Update Candidate

Components:

- UpdateCustomerCandidateCommand
- UpdateCustomerCandidateUseCase
- UpdateCustomerCandidate
- UpdateCustomerCandidateResult

The application coordinates existing CustomerUpdateVerification
domain rules without modifying domain behavior.

Supported outcomes:

- ELIGIBLE_CANDIDATE
- UNCHANGED
- BLOCKED

Supported assessment:

- Record version and identity revision checks.
- Critical field change assessment.
- Duplicate conflict assessment.
- Reverification requirements.
- Different verifier requirements.
- Audit requirement propagation.
- Safe result code mapping.

No durable customer update, trusted runtime authorization,
database transaction, audit execution or external event
publication is implemented.
