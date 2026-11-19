package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.identity.IdentityOwner;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.DocumentValidity.ExpiryBoundary.*;
import static ir.neshane.customer.domain.CustomerDocument.State.*;
import static ir.neshane.customer.domain.DocumentVersionHistory.Authority.*;

final class DocumentValiditySpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var id = new CustomerDocumentId("document");
        var issue = LocalDate.of(2026,1,1);
        var expiry = issue.plusDays(10);
        var first = doc(person,id,Optional.of(issue),Optional.of(expiry),1);
        expect(DocumentValidity.assess(first,issue.minusDays(1),UNRESOLVED).state().isEmpty());
        expect(DocumentValidity.assess(first,issue,UNRESOLVED).state().orElseThrow() == PROVIDED);
        for (var boundary : DocumentValidity.ExpiryBoundary.values()) {
            expect(DocumentValidity.assess(first,expiry.minusDays(1),boundary).state().orElseThrow() == PROVIDED);
            expect(DocumentValidity.assess(first,expiry.plusDays(1),boundary).state().orElseThrow() == EXPIRED);
        }
        expect(DocumentValidity.assess(first,expiry,UNRESOLVED).code().equals("DOCUMENT_EXPIRY_BOUNDARY_UNRESOLVED"));
        expect(DocumentValidity.assess(first,expiry,UNRESOLVED).state().isEmpty());
        expect(DocumentValidity.assess(first,expiry,LAST_VALID_DAY).state().orElseThrow() == PROVIDED);
        expect(DocumentValidity.assess(first,expiry,FIRST_INVALID_DAY).state().orElseThrow() == EXPIRED);
        expect(first.state() == PROVIDED);
        expect(DocumentValidity.assess(doc(person,id,Optional.empty(),Optional.empty(),1),expiry,UNRESOLVED).state().orElseThrow() == PROVIDED);
        var type = new DocumentTypeDefinition(tenant,"type","TYPE",new LocalizedText(Map.of("fa","مدرک")),new LocalizedText(Map.of()),
            DocumentTypeDefinition.Applicability.BOTH,DocumentTypeDefinition.RequirementStage.OPTIONAL,DocumentTypeDefinition.FileMultiplicity.SINGLE,
            Set.of("application/pdf"),1000,true,DocumentTypeDefinition.Status.ACTIVE,0);
        var catalog = DocumentTypeCatalog.empty(tenant,1000).addCandidate(type,DocumentTypeCatalog.Authority.MANAGE);
        var history = DocumentVersionHistory.start(first,catalog,UPLOAD).proposed().orElseThrow();
        var request = decision(first.owner(),id,1,"private-reason",2,3);
        for (var authority : List.of(NONE,UPLOAD,REPLACE)) {
            var denied = history.invalidate(request,authority);
            expect(denied.proposed().isEmpty());
            expect(denied.auditRequired());
            expect(denied.code().equals("DOCUMENT_INVALIDATION_AUTHORITY_REQUIRED"));
        }
        var foreign = new IdentityOwner(new TenantId("foreign"),person.id(),first.owner().kind());
        expect(history.invalidate(decision(foreign,id,1,"reason",2,3),INVALIDATE).code().equals("DOCUMENT_VERSION_SCOPE_MISMATCH"));
        var other = new IdentityOwner(tenant,new CustomerId("other"),first.owner().kind());
        expect(history.invalidate(decision(other,id,1,"reason",2,3),INVALIDATE).proposed().isEmpty());
        expect(history.invalidate(decision(first.owner(),new CustomerDocumentId("other"),1,"reason",2,3),INVALIDATE).proposed().isEmpty());
        expect(history.invalidate(decision(first.owner(),id,2,"reason",2,3),INVALIDATE).code().equals("DOCUMENT_VERSION_STALE"));
        for (String reason : new String[]{null,""," ","\u00a0"}) {
            expect(history.invalidate(decision(first.owner(),id,1,reason,2,3),INVALIDATE).code().equals("DOCUMENT_INVALIDATION_REASON_REQUIRED"));
        }
        expect(history.invalidate(decision(first.owner(),id,1,"reason",0,3),INVALIDATE).proposed().isEmpty());
        expect(history.invalidate(decision(first.owner(),id,1,"reason",4,3),INVALIDATE).proposed().isEmpty());
        var result = history.invalidate(request,INVALIDATE);
        expect(result.auditRequired());
        var invalid = result.proposed().orElseThrow();
        expect(invalid.current().state() == INVALIDATED);
        expect(invalid.current().version() == 1);
        expect(invalid.current().files().equals(first.files()));
        expect(invalid.current().expiryDate().equals(first.expiryDate()));
        expect(invalid.current().uploadedAt().equals(first.uploadedAt()));
        expect(invalid.invalidations().equals(List.of(request)));
        expect(history.invalidations().isEmpty());
        expect(history.current().state() == PROVIDED);
        expect(!request.toString().contains("private-reason"));
        expect(invalid.invalidate(request,INVALIDATE).code().equals("DOCUMENT_INVALIDATION_STATE_INVALID"));
        expect(DocumentValidity.assess(invalid.current(),expiry.plusDays(1),UNRESOLVED).state().orElseThrow() == INVALIDATED);
        var premature = doc(person,id,Optional.of(issue),Optional.of(expiry),2);
        expect(invalid.replace(1,premature,catalog,REPLACE).code().equals("DOCUMENT_VERSION_TIME_DISCONTINUITY"));
        var replacement = doc(person,id,Optional.of(issue),Optional.of(expiry.plusDays(20)),4);
        var next = invalid.replace(1,replacement,catalog,REPLACE).proposed().orElseThrow();
        expect(next.current().version() == 2);
        expect(next.current().state() == PROVIDED);
        expect(next.versions().getFirst().state() == SUPERSEDED);
        expect(next.versions().getFirst().files().equals(first.files()));
        expect(next.invalidations().equals(List.of(request)));
        expect(DocumentValidity.assess(next.versions().getFirst(),expiry.plusDays(1),UNRESOLVED).state().orElseThrow() == SUPERSEDED);
        expect(next.invalidate(request,INVALIDATE).code().equals("DOCUMENT_VERSION_STALE"));
        var finalHistory = next.invalidate(decision(first.owner(),id,2,"second",5,6),INVALIDATE).proposed().orElseThrow();
        expect(finalHistory.invalidations().size() == 2);
        expect(finalHistory.invalidations().getFirst().equals(request));
        expect(person.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        try { finalHistory.invalidations().clear(); throw new AssertionError("Mutable decisions"); }
        catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("PASS: "+checks+" isolated document validity and invalidation assertions");
    }
    private static DocumentVersionHistory.Invalidation decision(IdentityOwner owner,CustomerDocumentId id,long version,String reason,long effective,long recorded) {
        return new DocumentVersionHistory.Invalidation(owner,id,version,"actor",reason,Instant.ofEpochSecond(effective),Instant.ofEpochSecond(recorded),"correlation");
    }
    private static CustomerDocument doc(Customer customer,CustomerDocumentId id,Optional<LocalDate> issue,Optional<LocalDate> expiry,long time) {
        return CustomerDocument.createCandidate(customer,id,"type",List.of(new DocumentFileReference(customer.tenantId(),"reference-"+time,"application/pdf",100)),
            Optional.empty(),issue,expiry,Optional.empty(),Optional.empty(),"uploader",Instant.ofEpochSecond(time));
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Document validity assertion "+(checks+1));
        checks++;
    }
}
