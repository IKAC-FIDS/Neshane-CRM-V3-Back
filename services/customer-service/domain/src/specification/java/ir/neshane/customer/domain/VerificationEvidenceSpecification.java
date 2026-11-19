package ir.neshane.customer.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static ir.neshane.customer.domain.VerificationEvidence.Method.*;
import static ir.neshane.customer.domain.VerificationEvidence.Status.PROVIDED;
import static ir.neshane.customer.domain.VerificationEvidence.Availability.*;

final class VerificationEvidenceSpecification {
    private static int checks;
    private static final Instant START = Instant.ofEpochSecond(100);
    private static final Instant END = Instant.ofEpochSecond(200);
    static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var owner = person.identityInfo().owner();
        var item = evidence(owner,"e1","r1",1,MANUAL,PROVIDED,Optional.of(END));
        expect(item.evidenceId().equals("e1"));
        expect(item.owner().equals(owner));
        expect(item.identityRevision().equals("r1"));
        expect(item.evidenceVersion() == 1);
        expect(item.method() == MANUAL);
        expect(item.source().equals("private-source"));
        expect(item.issuer().equals("private-issuer"));
        expect(item.protectedReference().equals("private-reference"));
        expect(item.recordedBy().equals("private-actor"));
        expect(item.recordedAt().equals(START));
        expect(item.effectiveFrom().equals(START));
        expect(item.validUntilExclusive().orElseThrow().equals(END));
        expect(item.availabilityAt(START.minusSeconds(1)) == NOT_YET_EFFECTIVE);
        expect(item.availabilityAt(START) == AVAILABLE_CANDIDATE);
        expect(item.availabilityAt(END.minusSeconds(1)) == AVAILABLE_CANDIDATE);
        expect(item.availabilityAt(END) == EXPIRED);
        expect(evidence(owner,"e2","r1",1,MANUAL,PROVIDED,Optional.empty()).availabilityAt(END.plusSeconds(100000)) == AVAILABLE_CANDIDATE);
        expect(evidence(owner,"e2","r1",2,MANUAL,VerificationEvidence.Status.SUPERSEDED,Optional.of(END)).availabilityAt(START) == SUPERSEDED);
        expect(evidence(owner,"e2","r1",2,MANUAL,VerificationEvidence.Status.INVALIDATED,Optional.of(END)).availabilityAt(START) == INVALIDATED);
        reject("EVIDENCE_VALIDITY_INTERVAL_INVALID",() -> evidence(owner,"e1","r1",1,MANUAL,PROVIDED,Optional.of(START)));
        reject("EVIDENCE_VERSION_INVALID",() -> evidence(owner,"e1","r1",0,MANUAL,PROVIDED,Optional.empty()));
        reject("VERIFICATION_EVIDENCE_ID_REQUIRED",() -> evidence(owner," ","r1",1,MANUAL,PROVIDED,Optional.empty()));
        reject("IDENTITY_REVISION_REQUIRED",() -> evidence(owner,"e1"," ",1,MANUAL,PROVIDED,Optional.empty()));
        var list = new ArrayList<>(List.of(item));
        var set = new VerificationEvidenceSet(owner,"r1",list);
        list.clear();
        expect(set.evidence().size() == 1);
        expect(set.owner().equals(owner));
        expect(set.identityRevision().equals("r1"));
        expect(set.assess(person,"r1",MANUAL,START).outcome() == VerificationEvidenceSet.Outcome.AVAILABLE_CANDIDATE);
        expect(set.assess(person,"r2",MANUAL,START).code().equals("VERIFICATION_EVIDENCE_REVISION_MISMATCH"));
        expect(set.assess(person,"r1",SYSTEM,START).code().equals("VERIFICATION_EVIDENCE_METHOD_MISMATCH"));
        expect(set.assess(person,"r1",MANUAL,END).code().equals("VERIFICATION_EVIDENCE_UNAVAILABLE"));
        expect(set.assess(person,"r1",MANUAL,START.minusSeconds(1)).code().equals("VERIFICATION_EVIDENCE_UNAVAILABLE"));
        var foreign = Person.createCandidate(person.id(),new TenantId("other"),"A","B",null);
        var other = Person.createCandidate(new CustomerId("other"),tenant,"A","B",null);
        expect(set.assess(foreign,"r1",MANUAL,START).code().equals("VERIFICATION_EVIDENCE_SCOPE_MISMATCH"));
        expect(set.assess(other,"r1",MANUAL,START).code().equals("VERIFICATION_EVIDENCE_SCOPE_MISMATCH"));
        reject("VERIFICATION_EVIDENCE_SCOPE_MISMATCH",() -> new VerificationEvidenceSet(owner,"r1",List.of(evidence(foreign.identityInfo().owner(),"e2","r1",1,MANUAL,PROVIDED,Optional.empty()))));
        reject("VERIFICATION_EVIDENCE_REVISION_MISMATCH",() -> new VerificationEvidenceSet(owner,"r2",List.of(item)));
        reject("VERIFICATION_EVIDENCE_ID_DUPLICATE",() -> new VerificationEvidenceSet(owner,"r1",List.of(item,item)));
        expect(new VerificationEvidenceSet(owner,"r1",List.of()).assess(person,"r1",MANUAL,START).code().equals("VERIFICATION_EVIDENCE_REQUIRED"));
        var expired = evidence(owner,"e2","r1",1,MANUAL,PROVIDED,Optional.of(START.plusSeconds(1)));
        expect(new VerificationEvidenceSet(owner,"r1",List.of(item,expired)).assess(person,"r1",MANUAL,START.plusSeconds(1)).code().equals("VERIFICATION_EVIDENCE_UNAVAILABLE"));
        var system = evidence(owner,"e2","r1",1,SYSTEM,PROVIDED,Optional.empty());
        expect(new VerificationEvidenceSet(owner,"r1",List.of(system)).assess(person,"r1",SYSTEM,START).outcome() == VerificationEvidenceSet.Outcome.AVAILABLE_CANDIDATE);
        expect(!item.toString().contains("private"));
        expect(!set.toString().contains("private"));
        expect(person.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        try { set.evidence().clear(); throw new AssertionError("Mutable evidence"); }
        catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("PASS: " + checks + " isolated verification evidence assertions");
    }
    private static VerificationEvidence evidence(IdentityOwner owner,String id,String revision,long version,
            VerificationEvidence.Method method,VerificationEvidence.Status status,Optional<Instant> end) {
        return new VerificationEvidence(id,owner,revision,version,method,"private-source","private-issuer",
            "private-reference","private-actor",START,START,end,status);
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Verification evidence assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected " + code);
    }
}
