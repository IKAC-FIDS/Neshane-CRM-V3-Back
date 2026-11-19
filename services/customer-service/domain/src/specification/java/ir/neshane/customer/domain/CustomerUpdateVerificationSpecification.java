package ir.neshane.customer.domain;

import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.identity.IdentityIdentifier;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static ir.neshane.customer.domain.IdentityRevisionAssessment.Impact.*;
import static ir.neshane.customer.domain.IdentityRevisionAssessment.IdentifierCorrection.NO_VERIFIED_IDENTIFIER_CORRECTION;
import static ir.neshane.customer.domain.CustomerUpdateVerification.Outcome.*;

final class CustomerUpdateVerificationSpecification {
    private static int checks;
    private static final TenantId TENANT = new TenantId("tenant");
    private static final Person ORIGINAL = Person.createCandidate(new CustomerId("person"),TENANT,"A","B",null);
    private static final Person RENAMED = Person.createCandidate(ORIGINAL.id(),TENANT,"Changed","B",null);
    private static final Person ENGLISH = Person.createCandidate(ORIGINAL.id(),TENANT,"A","B","Example");
    private static final VerificationHistory HISTORY = VerificationHistory.empty(IdentityRevision.snapshot(ORIGINAL,"r1",VerificationStatus.VERIFIED));
    private static final CustomerRecordVersion VERSION = CustomerRecordVersion.initial(ORIGINAL);
    private static final CustomerUpdatePolicy.Current CURRENT = new CustomerUpdatePolicy.Current(HISTORY.current(),CustomerStatus.ACTIVE,VERSION);
    private static final LifecycleHistory.Context META = new LifecycleHistory.Context("h1","editor",Instant.ofEpochSecond(1),"corr");
    static void run() {
        var ordinary = assess(ENGLISH,"r1","r1",NOT_MATERIAL,null);
        expect(ordinary.outcome()==ELIGIBLE_CANDIDATE);
        expect(ordinary.auditRequired());
        var candidate = ordinary.proposed().orElseThrow();
        expect(candidate.record().version().value()==2);
        expect(candidate.record().identity().value().equals("r1"));
        expect(candidate.record().identity().verificationStatus()==VerificationStatus.VERIFIED);
        expect(candidate.record().status()==CustomerStatus.ACTIVE);
        expect(candidate.fields().equals(Set.of(CriticalFieldChangeAssessment.Field.ENGLISH_NAME)));
        expect(candidate.history().entries().size()==1);
        expect(candidate.history().current()==candidate.record().identity());
        expect(!candidate.differentVerifierRequired());
        expect(!candidate.toString().contains("Example"));
        var material = assess(RENAMED,"r1","r2",MATERIAL,"private reason");
        expect(material.outcome()==ELIGIBLE_CANDIDATE);
        var next = material.proposed().orElseThrow();
        expect(next.record().identity().value().equals("r2"));
        expect(next.record().identity().verificationStatus()==VerificationStatus.REQUIRES_REVERIFICATION);
        expect(next.record().version().value()==2);
        expect(next.history().entries().size()==1);
        expect(next.fields().contains(CriticalFieldChangeAssessment.Field.GIVEN_NAME));
        expect(!next.toString().contains("private reason"));
        expect(HISTORY.entries().isEmpty());
        expect(CURRENT.version().value()==1);
        expect(CURRENT.identity().verificationStatus()==VerificationStatus.VERIFIED);
        expect(assess(RENAMED,"r1","r2",MATERIAL,null).code().equals("CUSTOMER_CRITICAL_CHANGE_REASON_REQUIRED"));
        expect(assess(RENAMED,"r1","r2",UNKNOWN,"reason").code().equals("IDENTITY_EVIDENCE_IMPACT_UNRESOLVED"));
        expect(assess(RENAMED,"r1","r1",MATERIAL,"reason").code().equals("IDENTITY_REVISION_ROTATION_MISMATCH"));
        expect(assess(ENGLISH,"r1","r2",NOT_MATERIAL,null).code().equals("IDENTITY_REVISION_ROTATION_MISMATCH"));
        expect(assess(ENGLISH,"old","r1",NOT_MATERIAL,null).code().equals("IDENTITY_REVISION_STALE"));
        var noop = assess(ORIGINAL,"r1","r1",NOT_MATERIAL,null);
        expect(noop.outcome()==UNCHANGED);
        expect(noop.proposed().isEmpty());
        expect(!noop.auditRequired());
        expect(assess(ORIGINAL,"old","r1",NOT_MATERIAL,null).outcome()==BLOCKED);
        expect(assess(ENGLISH,"r1","r2",MATERIAL,null).proposed().orElseThrow().record().identity().verificationStatus()==VerificationStatus.REQUIRES_REVERIFICATION);
        expect(assess(RENAMED,"r1","r2",NOT_MATERIAL,"reason").proposed().orElseThrow().record().identity().verificationStatus()==VerificationStatus.VERIFIED);
        var context = context(CURRENT,CustomerUpdatePolicy.Authority.UPDATE,VerificationStatus.VERIFIED);
        expect(full(CURRENT,new CustomerRecordVersion(VERSION.owner(),2),RENAMED,context,HISTORY,List.of()).code().equals("CUSTOMER_RECORD_VERSION_STALE"));
        expect(full(CURRENT,VERSION,RENAMED,context(CURRENT,CustomerUpdatePolicy.Authority.NONE,VerificationStatus.VERIFIED),HISTORY,List.of()).code().equals("CUSTOMER_UPDATE_AUTHORITY_REQUIRED"));
        expect(full(CURRENT,VERSION,RENAMED,context(CURRENT,CustomerUpdatePolicy.Authority.UPDATE,VerificationStatus.NOT_VERIFIED),HISTORY,List.of()).code().equals("CUSTOMER_UPDATE_VERIFICATION_CHANGE_FORBIDDEN"));
        expect(full(CURRENT,VERSION,RENAMED,context,VerificationHistory.empty(IdentityRevision.snapshot(ORIGINAL,"r1",VerificationStatus.VERIFIED)),List.of()).code().equals("CUSTOMER_UPDATE_HISTORY_MISMATCH"));
        var warning = new DuplicateDetection.Comparison(req(RENAMED),TENANT,new CustomerId("other"),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.NAME));
        expect(full(CURRENT,VERSION,RENAMED,context,HISTORY,List.of(warning)).code().equals("CUSTOMER_UPDATE_DUPLICATE_WARNING_UNRESOLVED"));
        var conflict = new DuplicateDetection.Comparison(req(RENAMED),TENANT,new CustomerId("other"),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER));
        expect(full(CURRENT,VERSION,RENAMED,context,HISTORY,List.of(conflict)).code().equals("DUPLICATE_DEFINITE_CONFLICT"));
        var oldId = new IdentityIdentifier("id",ORIGINAL.identityInfo().owner(),"passport","abc",java.util.Optional.empty(),java.util.Optional.empty(),java.util.Optional.empty(),java.util.Optional.empty(),IdentityIdentifier.Status.ACTIVE);
        var newId = new IdentityIdentifier("id",ORIGINAL.identityInfo().owner(),"passport","def",java.util.Optional.empty(),java.util.Optional.empty(),java.util.Optional.empty(),java.util.Optional.empty(),IdentityIdentifier.Status.ACTIVE);
        var before = Person.createCandidate(ORIGINAL.id(),TENANT,"A","B",null,ORIGINAL.contactInfo(),new IdentityInfo(ORIGINAL.identityInfo().owner(),List.of(oldId)));
        var after = Person.createCandidate(ORIGINAL.id(),TENANT,"A","B",null,ORIGINAL.contactInfo(),new IdentityInfo(ORIGINAL.identityInfo().owner(),List.of(newId)));
        var h = VerificationHistory.empty(IdentityRevision.snapshot(before,"r1",VerificationStatus.VERIFIED));
        var cur = new CustomerUpdatePolicy.Current(h.current(),CustomerStatus.ACTIVE,VERSION);
        var catalog = IdentifierTypeCatalog.empty().withSystemCandidate(new IdentifierTypeDefinition("passport","PASSPORT",SystemIdentifierVocabulary.PASSPORT.title(),Set.of(IdentityOwner.Kind.PERSON),Set.of(),java.util.Optional.of("p"),true,java.util.Optional.empty(),IdentifierTypeDefinition.Status.ACTIVE));
        var profile = new ValidationProfile("p",java.util.Optional.empty(),List.of(new ValidationProfile.Length(ValidationProfile.Field.VALUE,3,10)));
        var corrected = CustomerUpdateVerification.assess(cur,VERSION,after,context,ContactTypeCatalog.empty(),catalog,Map.of(),Map.of("id",profile),req(after),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),h,"r1","r2",CriticalFieldChangeAssessment.ExceptionScope.ORDINARY,"reason",MATERIAL,IdentityRevisionAssessment.IdentifierCorrection.VERIFIED_IDENTIFIER_CORRECTED,META);
        expect(corrected.outcome()==ELIGIBLE_CANDIDATE);
        expect(corrected.proposed().orElseThrow().differentVerifierRequired());
        expect(corrected.proposed().orElseThrow().history().excludedVerifiers().equals(Set.of("editor")));
        expect(corrected.proposed().orElseThrow().record().identity().verificationStatus()==VerificationStatus.REQUIRES_REVERIFICATION);
        expect(h.excludedVerifiers().isEmpty());
        System.out.println("PASS: "+checks+" isolated customer update verification assertions");
    }
    private static CustomerUpdatePolicy.Context context(CustomerUpdatePolicy.Current c,CustomerUpdatePolicy.Authority a,VerificationStatus v) {
        return new CustomerUpdatePolicy.Context(TENANT,"request",a,c.status(),v);
    }
    private static DuplicateDetection.Request req(Customer c) { return DuplicateDetection.Request.of(c,DuplicateDetection.Operation.UPDATE,"request"); }
    private static CustomerUpdateVerification.Assessment assess(Customer p,String expected,String next,IdentityRevisionAssessment.Impact impact,String reason) {
        return CustomerUpdateVerification.assess(CURRENT,VERSION,p,context(CURRENT,CustomerUpdatePolicy.Authority.UPDATE,VerificationStatus.VERIFIED),
            ContactTypeCatalog.empty(),IdentifierTypeCatalog.empty(),Map.of(),Map.of(),req(p),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),HISTORY,expected,next,
            CriticalFieldChangeAssessment.ExceptionScope.ORDINARY,reason,impact,NO_VERIFIED_IDENTIFIER_CORRECTION,META);
    }
    private static CustomerUpdateVerification.Assessment full(CustomerUpdatePolicy.Current c,CustomerRecordVersion v,Customer p,CustomerUpdatePolicy.Context ctx,VerificationHistory h,List<DuplicateDetection.Comparison> comparisons) {
        return CustomerUpdateVerification.assess(c,v,p,ctx,ContactTypeCatalog.empty(),IdentifierTypeCatalog.empty(),Map.of(),Map.of(),req(p),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,comparisons,h,"r1","r2",
            CriticalFieldChangeAssessment.ExceptionScope.ORDINARY,"reason",MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,META);
    }
    private static void expect(boolean condition) { if (!condition) throw new AssertionError("Customer update verification assertion "+(checks+1)); checks++; }
}
