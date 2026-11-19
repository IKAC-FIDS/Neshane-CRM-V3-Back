package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.LogicalAccessPolicy.*;
import static ir.neshane.customer.domain.LogicalAccessPolicy.Operation.*;
import static ir.neshane.customer.domain.LogicalAccessPolicy.Outcome.*;

final class SensitiveAccessPolicySpecification {
    private static int checks;
    static void run() {
        var tenant=new TenantId("tenant");
        var foreign=new TenantId("foreign");
        var person=Person.createCandidate(new CustomerId("person"),tenant,"Private","Name",null);
        var other=Person.createCandidate(new CustomerId("other"),tenant,"A","B",null);
        var company=Company.createCandidate(person.id(),tenant,"Company",null);
        var outsider=Person.createCandidate(person.id(),foreign,"A","B",null);
        var document=doc(person);
        var evidence=evidence(person);
        var duplicate=DuplicateDetection.Request.of(person,DuplicateDetection.Operation.UPDATE,"private-request");
        var identityOps=Set.of(IDENTITY_SENSITIVE_READ,IDENTITY_CORRECT);
        var docOps=Set.of(DOCUMENT_READ,DOCUMENT_CONTENT_READ,DOCUMENT_UPLOAD,DOCUMENT_REPLACE,DOCUMENT_INVALIDATE);
        var evidenceOps=Set.of(VERIFICATION_EVIDENCE_READ,VERIFY_MANUAL,VERIFICATION_REVOKE);
        for (var op : Operation.values()) {
            var actor=actor(tenant,op);
            expect(SensitiveAccessPolicy.assessIdentity(actor,person,op).outcome()==(identityOps.contains(op)?ELIGIBLE_CANDIDATE:DENIED));
            expect(SensitiveAccessPolicy.assessDocument(actor,person,document,op).outcome()==(docOps.contains(op)?ELIGIBLE_CANDIDATE:DENIED));
            expect(SensitiveAccessPolicy.assessEvidence(actor,person,evidence,op).outcome()==(evidenceOps.contains(op)?ELIGIBLE_CANDIDATE:DENIED));
            expect(SensitiveAccessPolicy.assessDuplicateException(actor,person,duplicate).outcome()==(op==DUPLICATE_EXCEPTION?ELIGIBLE_CANDIDATE:DENIED));
            for (var target : identityOps)
                if (target!=op) expect(SensitiveAccessPolicy.assessIdentity(actor,person,target).outcome()==DENIED);
            for (var target : docOps)
                if (target!=op) expect(SensitiveAccessPolicy.assessDocument(actor,person,document,target).outcome()==DENIED);
            for (var target : evidenceOps)
                if (target!=op) expect(SensitiveAccessPolicy.assessEvidence(actor,person,evidence,target).outcome()==DENIED);
        }
        for (var op : identityOps) {
            var actor=actor(tenant,op);
            expect(SensitiveAccessPolicy.assessIdentity(actor,outsider,op).code().equals("ACCESS_TENANT_MISMATCH"));
            expect(SensitiveAccessPolicy.assessIdentity(null,person,op).outcome()==DENIED);
        }
        for (var op : docOps) {
            var actor=actor(tenant,op);
            for (var wrong : List.of(other,company,outsider))
                expect(SensitiveAccessPolicy.assessDocument(actor,wrong,document,op).code().equals("ACCESS_DOCUMENT_SCOPE_MISMATCH"));
            expect(SensitiveAccessPolicy.assessDocument(actor,outsider,doc(outsider),op).code().equals("ACCESS_TENANT_MISMATCH"));
            expect(SensitiveAccessPolicy.assessDocument(null,person,document,op).outcome()==DENIED);
            expect(SensitiveAccessPolicy.assessDocument(actor,person,null,op).outcome()==DENIED);
        }
        for (var op : evidenceOps) {
            var actor=actor(tenant,op);
            for (var wrong : List.of(other,company,outsider))
                expect(SensitiveAccessPolicy.assessEvidence(actor,wrong,evidence,op).code().equals("ACCESS_EVIDENCE_SCOPE_MISMATCH"));
            expect(SensitiveAccessPolicy.assessEvidence(actor,outsider,evidence(outsider),op).code().equals("ACCESS_TENANT_MISMATCH"));
            expect(SensitiveAccessPolicy.assessEvidence(null,person,evidence,op).outcome()==DENIED);
            expect(SensitiveAccessPolicy.assessEvidence(actor,person,null,op).outcome()==DENIED);
        }
        var actor=actor(tenant,DUPLICATE_EXCEPTION);
        for (var wrong : List.of(other,company,outsider))
            expect(SensitiveAccessPolicy.assessDuplicateException(actor,wrong,duplicate).code().equals("ACCESS_DUPLICATE_SCOPE_MISMATCH"));
        expect(SensitiveAccessPolicy.assessDuplicateException(actor,outsider,DuplicateDetection.Request.of(outsider,DuplicateDetection.Operation.CREATE,"req")).code().equals("ACCESS_TENANT_MISMATCH"));
        expect(SensitiveAccessPolicy.assessDuplicateException(null,person,duplicate).outcome()==DENIED);
        expect(SensitiveAccessPolicy.assessDuplicateException(actor,person,null).outcome()==DENIED);
        expect(SensitiveAccessPolicy.assessIdentity(actor,null,IDENTITY_CORRECT).outcome()==DENIED);
        expect(SensitiveAccessPolicy.assessIdentity(actor,person,null).outcome()==DENIED);
        expect(SensitiveAccessPolicy.assessDocument(actor,person,document,null).outcome()==DENIED);
        expect(SensitiveAccessPolicy.assessEvidence(actor,person,evidence,null).outcome()==DENIED);
        for (var result : List.of(SensitiveAccessPolicy.assessDocument(actor(tenant,DOCUMENT_CONTENT_READ),person,document,DOCUMENT_CONTENT_READ),
                SensitiveAccessPolicy.assessEvidence(actor(tenant,VERIFICATION_EVIDENCE_READ),person,evidence,VERIFICATION_EVIDENCE_READ),
                SensitiveAccessPolicy.assessDuplicateException(actor,person,duplicate))) {
            expect(result.outcome()==ELIGIBLE_CANDIDATE);
            expect(!result.toString().toLowerCase(java.util.Locale.ROOT).contains("private"));
        }
        expect(document.state()==CustomerDocument.State.PROVIDED);
        expect(document.version()==1);
        expect(evidence.evidence().getFirst().evidenceVersion()==1);
        expect(person.verificationStatus()==VerificationStatus.NOT_VERIFIED);
        System.out.println("PASS: "+checks+" isolated sensitive access assertions");
    }
    private static ActorContext actor(TenantId tenant,Operation op) { return new ActorContext("actor",tenant,Set.of(new Grant(tenant,op))); }
    private static CustomerDocument doc(Customer owner) {
        return CustomerDocument.createCandidate(owner,new CustomerDocumentId("doc"),"type",List.of(new DocumentFileReference(owner.tenantId(),"private-file","application/pdf",100)),Optional.of("private-number"),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),"actor",Instant.EPOCH);
    }
    private static VerificationEvidenceSet evidence(Customer owner) {
        var o=owner.identityInfo().owner();
        return new VerificationEvidenceSet(o,"rev",List.of(new VerificationEvidence("e",o,"rev",1,VerificationEvidence.Method.MANUAL,"source","issuer","private-proof","actor",Instant.EPOCH,Instant.EPOCH,Optional.empty(),VerificationEvidence.Status.PROVIDED)));
    }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Sensitive access assertion "+(checks+1)); checks++; }
}
