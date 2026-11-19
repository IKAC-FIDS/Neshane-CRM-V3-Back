package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.DocumentVersionHistory.Authority.*;
import static ir.neshane.customer.domain.DocumentVersionHistory.Outcome.*;
import static ir.neshane.customer.domain.CustomerDocument.State.*;

final class DocumentVersionHistorySpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var id = new CustomerDocumentId("document");
        var first = doc(person,id,"type","private-first",100,1);
        var second = doc(person,id,"type","private-second",200,2);
        var catalog = DocumentTypeCatalog.empty(tenant,1000).addCandidate(type(tenant,300,DocumentTypeDefinition.Status.ACTIVE),DocumentTypeCatalog.Authority.MANAGE);
        var denied = DocumentVersionHistory.start(first,catalog,NONE);
        expect(denied.outcome() == BLOCKED);
        expect(denied.auditRequired());
        expect(denied.proposed().isEmpty());
        expect(denied.code().equals("DOCUMENT_UPLOAD_AUTHORITY_REQUIRED"));
        var started = DocumentVersionHistory.start(first,catalog,UPLOAD);
        expect(started.outcome() == ELIGIBLE_CANDIDATE);
        expect(started.auditRequired());
        var history = started.proposed().orElseThrow();
        expect(history.current() == first);
        expect(history.versions().size() == 1);
        expect(history.current().version() == 1);
        expect(history.current().state() == PROVIDED);
        var replaced = history.replace(1,second,catalog,REPLACE);
        expect(replaced.outcome() == ELIGIBLE_CANDIDATE);
        expect(replaced.auditRequired());
        var next = replaced.proposed().orElseThrow();
        expect(next.versions().size() == 2);
        expect(next.current().version() == 2);
        expect(next.current().state() == PROVIDED);
        expect(next.current().id().equals(id));
        expect(next.current().owner().equals(first.owner()));
        expect(next.current().typeId().equals(first.typeId()));
        expect(next.current().files().equals(second.files()));
        expect(next.current().notes().equals(second.notes()));
        expect(next.current().uploadedAt().equals(second.uploadedAt()));
        var previous = next.versions().getFirst();
        expect(previous.state() == SUPERSEDED);
        expect(previous.version() == 1);
        expect(previous.files().equals(first.files()));
        expect(previous.notes().equals(first.notes()));
        expect(previous.uploadedAt().equals(first.uploadedAt()));
        expect(history.current().state() == PROVIDED);
        expect(history.versions().size() == 1);
        expect(second.version() == 1);
        expect(next.replace(1,second,catalog,REPLACE).code().equals("DOCUMENT_VERSION_STALE"));
        var noAuthority = next.replace(2,second,catalog,UPLOAD);
        expect(noAuthority.code().equals("DOCUMENT_REPLACEMENT_AUTHORITY_REQUIRED"));
        expect(noAuthority.auditRequired());
        expect(next.replace(2,first,catalog,REPLACE).code().equals("DOCUMENT_VERSION_TIME_DISCONTINUITY"));
        expect(next.replace(2,next.current(),catalog,REPLACE).code().equals("DOCUMENT_REPLACEMENT_CANDIDATE_REQUIRED"));
        expect(DocumentVersionHistory.start(next.current(),catalog,UPLOAD).code().equals("DOCUMENT_INITIAL_VERSION_REQUIRED"));
        expect(next.replace(2,doc(person,new CustomerDocumentId("other"),"type","ref",100,3),catalog,REPLACE).code().equals("DOCUMENT_VERSION_SCOPE_MISMATCH"));
        expect(next.replace(2,doc(person,id,"other","ref",100,3),catalog,REPLACE).code().equals("DOCUMENT_VERSION_SCOPE_MISMATCH"));
        var foreign = Person.createCandidate(person.id(),new TenantId("foreign"),"A","B",null);
        expect(next.replace(2,doc(foreign,id,"type","ref",100,3),catalog,REPLACE).code().equals("DOCUMENT_VERSION_SCOPE_MISMATCH"));
        expect(next.replace(2,doc(person,id,"type","private-first",110,3),catalog,REPLACE).code().equals("DOCUMENT_FILE_REFERENCE_METADATA_MISMATCH"));
        var inactive = catalog.reviseCandidate(type(tenant,300,DocumentTypeDefinition.Status.INACTIVE),DocumentTypeCatalog.Authority.MANAGE);
        expect(next.replace(2,doc(person,id,"type","ref",100,3),inactive,REPLACE).code().equals("DOCUMENT_TYPE_NOT_SELECTABLE"));
        var smaller = catalog.reviseCandidate(type(tenant,150,DocumentTypeDefinition.Status.ACTIVE),DocumentTypeCatalog.Authority.MANAGE);
        expect(next.replace(2,doc(person,id,"type","ref",200,3),smaller,REPLACE).code().equals("DOCUMENT_TYPE_FILE_SIZE_EXCEEDED"));
        var third = next.replace(2,doc(person,id,"type","ref3",100,3),smaller,REPLACE).proposed().orElseThrow();
        expect(third.versions().size() == 3);
        expect(third.current().version() == 3);
        expect(third.versions().stream().filter(d -> d.state() == PROVIDED).count() == 1);
        expect(third.versions().stream().filter(d -> d.state() == SUPERSEDED).count() == 2);
        expect(third.versions().getFirst().files().equals(first.files()));
        expect(next.current().state() == PROVIDED);
        expect(!third.toString().contains("private"));
        expect(person.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        try { third.versions().clear(); throw new AssertionError("Mutable version history"); }
        catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("PASS: " + checks + " isolated document versioning assertions");
    }
    private static DocumentTypeDefinition type(TenantId tenant,long max,DocumentTypeDefinition.Status status) {
        return new DocumentTypeDefinition(tenant,"type","TYPE",new LocalizedText(Map.of("fa","مدرک")),new LocalizedText(Map.of()),
            DocumentTypeDefinition.Applicability.BOTH,DocumentTypeDefinition.RequirementStage.OPTIONAL,DocumentTypeDefinition.FileMultiplicity.SINGLE,Set.of("application/pdf"),max,false,status,0);
    }
    private static CustomerDocument doc(Customer customer,CustomerDocumentId id,String type,String ref,long size,long time) {
        return CustomerDocument.createCandidate(customer,id,type,List.of(new DocumentFileReference(customer.tenantId(),ref,"application/pdf",size)),
            Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),Optional.of("private-note-"+time),"actor",Instant.ofEpochSecond(time));
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Document version assertion " + (checks + 1));
        checks++;
    }
}
