package ir.neshane.customer.domain.access;

import ir.neshane.customer.domain.document.CustomerDocument;

import ir.neshane.customer.domain.verification.VerificationEvidenceSet;

import ir.neshane.customer.domain.duplicate.DuplicateDetection;

import ir.neshane.customer.domain.customer.model.Customer;

import static ir.neshane.customer.domain.access.LogicalAccessPolicy.*;

/** Candidate access to protected facts; returns no resource values or references. */
public final class SensitiveAccessPolicy {
    private SensitiveAccessPolicy() {}

    public static Assessment assessIdentity(ActorContext actor,Customer customer,Operation operation) {
        if (customer==null) return denied("ACCESS_CUSTOMER_REQUIRED");
        if (operation!=Operation.IDENTITY_SENSITIVE_READ && operation!=Operation.IDENTITY_CORRECT)
            return denied("ACCESS_RESOURCE_OPERATION_MISMATCH");
        return assess(actor,new Request(customer.tenantId(),operation));
    }

    public static Assessment assessDocument(ActorContext actor,Customer owner,CustomerDocument document,Operation operation) {
        if (owner==null || document==null) return denied("ACCESS_DOCUMENT_SCOPE_REQUIRED");
        if (!owner.identityInfo().owner().equals(document.owner())) return denied("ACCESS_DOCUMENT_SCOPE_MISMATCH");
        if (operation!=Operation.DOCUMENT_READ && operation!=Operation.DOCUMENT_CONTENT_READ &&
            operation!=Operation.DOCUMENT_UPLOAD && operation!=Operation.DOCUMENT_REPLACE && operation!=Operation.DOCUMENT_INVALIDATE)
            return denied("ACCESS_RESOURCE_OPERATION_MISMATCH");
        return assess(actor,new Request(document.owner().tenantId(),operation));
    }

    /** Evidence scope is independent of revision freshness and qualification, checked by verification rules. */
    public static Assessment assessEvidence(ActorContext actor,Customer owner,VerificationEvidenceSet evidence,Operation operation) {
        if (owner==null || evidence==null) return denied("ACCESS_EVIDENCE_SCOPE_REQUIRED");
        if (!owner.identityInfo().owner().equals(evidence.owner())) return denied("ACCESS_EVIDENCE_SCOPE_MISMATCH");
        if (operation!=Operation.VERIFICATION_EVIDENCE_READ && operation!=Operation.VERIFY_MANUAL && operation!=Operation.VERIFICATION_REVOKE)
            return denied("ACCESS_RESOURCE_OPERATION_MISMATCH");
        return assess(actor,new Request(evidence.owner().tenantId(),operation));
    }

    public static Assessment assessDuplicateException(ActorContext actor,Customer customer,DuplicateDetection.Request request) {
        if (customer==null || request==null) return denied("ACCESS_DUPLICATE_SCOPE_REQUIRED");
        if (!DuplicateDetection.Request.of(customer,request.operation(),request.requestId()).equals(request))
            return denied("ACCESS_DUPLICATE_SCOPE_MISMATCH");
        return assess(actor,new Request(customer.tenantId(),Operation.DUPLICATE_EXCEPTION));
    }

    private static Assessment denied(String code) { return new Assessment(Outcome.DENIED,code); }
}
