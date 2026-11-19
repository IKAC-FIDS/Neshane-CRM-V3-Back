package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

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
import static ir.neshane.customer.domain.DocumentVersionHistory.Authority.*;
import static ir.neshane.customer.domain.CustomerDocument.State.*;
import static ir.neshane.customer.domain.DocumentEventDraft.Type.*;

final class DocumentHistorySpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var catalog = DocumentTypeCatalog.empty(tenant,1000).addCandidate(new DocumentTypeDefinition(tenant,"type","TYPE",
            new LocalizedText(Map.of("fa","مدرک")),new LocalizedText(Map.of()),DocumentTypeDefinition.Applicability.BOTH,
            DocumentTypeDefinition.RequirementStage.OPTIONAL,DocumentTypeDefinition.FileMultiplicity.SINGLE,Set.of("application/pdf"),
            100,true,DocumentTypeDefinition.Status.ACTIVE,0),DocumentTypeCatalog.Authority.MANAGE);
        var first = doc(person,"d1",1);
        reject("DOCUMENT_UPLOAD_AUTHORITY_REQUIRED",() -> DocumentHistory.upload(first,catalog,NONE,null,context("h1","actor",2)));
        reject("DOCUMENT_HISTORY_CONTEXT_MISMATCH",() -> DocumentHistory.upload(first,catalog,UPLOAD,null,context("h1","other",2)));
        reject("DOCUMENT_HISTORY_TIME_INVALID",() -> DocumentHistory.upload(first,catalog,UPLOAD,null,context("h1","actor",0)));
        var initial = DocumentHistory.upload(first,catalog,UPLOAD,null,context("h1","actor",2));
        var upload = initial.entries().getFirst();
        expect(upload.sequence()==1);
        expect(upload.action()==DocumentHistory.Action.UPLOAD);
        expect(upload.previous().isEmpty());
        expect(upload.proposed()==first);
        expect(upload.reason().isEmpty());
        expect(upload.auditRequired());
        expect(upload.effectiveAt().equals(time(1)));
        expect(upload.context().occurredAt().equals(time(2)));
        var event = DocumentEventDraft.fromCandidate(upload,"e1",Optional.empty());
        expect(event.eventType()==CustomerDocumentUploaded);
        expect(event.documentVersion()==1);
        expect(event.historySequence()==1);
        expect(event.tenantId().equals(tenant));
        expect(event.customerId().equals(person.id()));
        expect(event.documentId().equals(first.id()));
        expect(event.proposedState()==PROVIDED);
        expect(event.eventVersion()==1);
        expect(event.causationId().isEmpty());
        expect(event.effectiveAt().equals(time(1)));
        expect(event.recordedAt().equals(time(2)));
        expect(event.correlationId().equals("corr"));
        expect(event.eventId().equals("e1"));
        var second = doc(person,"d1",3);
        reject("HISTORY_ID_DUPLICATE",() -> initial.replace(1,second,catalog,REPLACE,null,context("h1","actor",4)));
        reject("HISTORY_TIME_DISCONTINUITY",() -> initial.replace(1,second,catalog,REPLACE,null,context("h2","actor",1)));
        reject("DOCUMENT_VERSION_STALE",() -> initial.replace(2,second,catalog,REPLACE,null,context("h2","actor",4)));
        reject("DOCUMENT_REPLACEMENT_AUTHORITY_REQUIRED",() -> initial.replace(1,second,catalog,UPLOAD,null,context("h2","actor",4)));
        reject("DOCUMENT_HISTORY_CONTEXT_MISMATCH",() -> initial.replace(1,second,catalog,REPLACE,null,context("h2","other",4)));
        reject("DOCUMENT_VERSION_SCOPE_MISMATCH",() -> initial.replace(1,doc(person,"d2",3),catalog,REPLACE,null,context("h2","actor",4)));
        var foreign = Person.createCandidate(person.id(),new TenantId("foreign"),"A","B",null);
        reject("DOCUMENT_VERSION_SCOPE_MISMATCH",() -> initial.replace(1,doc(foreign,"d1",3),catalog,REPLACE,null,context("h2","actor",4)));
        var replaced = initial.replace(1,second,catalog,REPLACE,"private-replacement",context("h2","actor",4));
        var replace = replaced.entries().getLast();
        expect(replace.action()==DocumentHistory.Action.REPLACE);
        expect(replace.sequence()==2);
        expect(replace.previous().orElseThrow().state()==PROVIDED);
        expect(replace.proposed().version()==2);
        expect(replace.reason().orElseThrow().equals("private-replacement"));
        expect(replaced.versions().versions().getFirst().state()==SUPERSEDED);
        expect(replaced.versions().versions().getFirst().files().equals(first.files()));
        expect(initial.current().version()==1);
        expect(initial.entries().size()==1);
        var replacedEvent = DocumentEventDraft.fromCandidate(replace,"e2",Optional.of("e1"));
        expect(replacedEvent.eventType()==CustomerDocumentReplaced);
        expect(replacedEvent.documentVersion()==2);
        expect(replacedEvent.causationId().orElseThrow().equals("e1"));
        var decision = new DocumentVersionHistory.Invalidation(first.owner(),first.id(),2,"revoker","private-reason",time(5),time(6),"corr");
        reject("DOCUMENT_INVALIDATION_AUTHORITY_REQUIRED",() -> replaced.invalidate(decision,REPLACE,context("h3","revoker",6)));
        reject("DOCUMENT_HISTORY_CONTEXT_MISMATCH",() -> replaced.invalidate(decision,INVALIDATE,context("h3","other",6)));
        reject("DOCUMENT_HISTORY_CONTEXT_MISMATCH",() -> replaced.invalidate(decision,INVALIDATE,context("h3","revoker",7)));
        reject("DOCUMENT_HISTORY_CONTEXT_MISMATCH",() -> replaced.invalidate(decision,INVALIDATE,new LifecycleHistory.Context("h3","revoker",time(6),"other")));
        var invalidated = replaced.invalidate(decision,INVALIDATE,context("h3","revoker",6));
        var invalid = invalidated.entries().getLast();
        expect(invalid.sequence()==3);
        expect(invalid.action()==DocumentHistory.Action.INVALIDATE);
        expect(invalid.reason().orElseThrow().equals("private-reason"));
        expect(invalid.previous().orElseThrow().version()==2);
        expect(invalid.proposed().version()==2);
        expect(invalid.proposed().state()==INVALIDATED);
        expect(invalid.effectiveAt().equals(time(5)));
        expect(invalidated.versions().invalidations().equals(List.of(decision)));
        expect(invalidated.current().files().equals(second.files()));
        expect(replaced.current().state()==PROVIDED);
        expect(invalidated.entries().getFirst()==upload);
        var invalidEvent = DocumentEventDraft.fromCandidate(invalid,"e3",Optional.empty());
        expect(invalidEvent.eventType()==CustomerDocumentInvalidated);
        expect(invalidEvent.proposedState()==INVALIDATED);
        expect(invalidEvent.historySequence()==3);
        expect(invalidEvent.effectiveAt().equals(time(5)));
        expect(invalidEvent.recordedAt().equals(time(6)));
        expect(!invalidEvent.toString().contains("private"));
        expect(!invalid.toString().contains("private"));
        reject("DOCUMENT_INVALIDATION_STATE_INVALID",() -> invalidated.invalidate(decision,INVALIDATE,context("h4","revoker",6)));
        var third = invalidated.replace(2,doc(person,"d1",7),catalog,REPLACE,null,context("h4","actor",8));
        expect(third.current().version()==3);
        expect(third.entries().size()==4);
        expect(third.entries().get(2).proposed().state()==INVALIDATED);
        expect(third.versions().versions().get(1).state()==SUPERSEDED);
        expect(third.versions().invalidations().equals(List.of(decision)));
        expect(DocumentValidity.assess(third.current(),LocalDate.of(2027,1,1),DocumentValidity.ExpiryBoundary.UNRESOLVED).state().orElseThrow()==EXPIRED);
        expect(third.entries().size()==4);
        expect(person.verificationStatus()==VerificationStatus.NOT_VERIFIED);
        try { third.entries().clear(); throw new AssertionError("Mutable journal"); } catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("PASS: "+checks+" isolated document history and event assertions");
    }
    private static LifecycleHistory.Context context(String id,String actor,long time) { return new LifecycleHistory.Context(id,actor,time(time),"corr"); }
    private static Instant time(long time) { return Instant.ofEpochSecond(time); }
    private static CustomerDocument doc(Customer customer,String id,long time) {
        return CustomerDocument.createCandidate(customer,new CustomerDocumentId(id),"type",List.of(new DocumentFileReference(customer.tenantId(),"private-ref-"+time,"application/pdf",100)),
            Optional.empty(),Optional.empty(),Optional.of(LocalDate.of(2026,12,1)),Optional.empty(),Optional.empty(),"actor",time(time));
    }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected "+code);
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Document history assertion "+(checks+1));
        checks++;
    }
}
