package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.identity.IdentityOwner;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class CustomerDocumentSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var company = Company.createCandidate(new CustomerId("company"),tenant,"Company",null);
        var file = new DocumentFileReference(tenant,"private-ref","application/pdf",100);
        var second = new DocumentFileReference(tenant,"private-ref2","image/png",200);
        var id = new CustomerDocumentId("private-document");
        var date = LocalDate.of(2030,1,1);
        var instant = Instant.ofEpochSecond(100);
        var files = new ArrayList<>(List.of(file,second));
        var doc = CustomerDocument.createCandidate(person,id,"passport",files,Optional.of("private-number"),
            Optional.of(date),Optional.of(date.plusYears(1)),Optional.of("private-issuer"),Optional.of(" private-notes "),"private-uploader",instant);
        files.clear();
        expect(doc.id().equals(id));
        expect(doc.owner().equals(person.identityInfo().owner()));
        expect(doc.typeId().equals("passport"));
        expect(doc.files().size() == 2);
        expect(doc.files().getFirst().equals(file));
        expect(doc.documentNumber().orElseThrow().equals("private-number"));
        expect(doc.issueDate().orElseThrow().equals(date));
        expect(doc.expiryDate().orElseThrow().equals(date.plusYears(1)));
        expect(doc.issuer().orElseThrow().equals("private-issuer"));
        expect(doc.notes().orElseThrow().equals("private-notes"));
        expect(doc.uploader().equals("private-uploader"));
        expect(doc.uploadedAt().equals(instant));
        expect(doc.version() == 1);
        expect(doc.state() == CustomerDocument.State.PROVIDED);
        expect(file.tenantId().equals(tenant));
        expect(file.referenceId().equals("private-ref"));
        expect(file.format().equals("application/pdf"));
        expect(file.sizeBytes() == 100);
        expect(!doc.toString().contains("private"));
        expect(!file.toString().contains("private"));
        expect(!id.toString().contains("private"));
        var companyDoc = minimal(company,id,List.of(file));
        expect(companyDoc.owner().kind() == IdentityOwner.Kind.COMPANY);
        expect(companyDoc.owner().customerId().equals(company.id()));
        expect(companyDoc.documentNumber().isEmpty());
        expect(companyDoc.issueDate().isEmpty());
        expect(companyDoc.expiryDate().isEmpty());
        expect(companyDoc.issuer().isEmpty());
        expect(companyDoc.notes().isEmpty());
        expect(company.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        expect(person.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        reject("DOCUMENT_ID_REQUIRED",() -> new CustomerDocumentId(" "));
        reject("DOCUMENT_FILE_SIZE_INVALID",() -> new DocumentFileReference(tenant,"ref","pdf",0));
        reject("DOCUMENT_FILE_REFERENCE_REQUIRED",() -> new DocumentFileReference(tenant," ","pdf",1));
        reject("DOCUMENT_FILE_FORMAT_REQUIRED",() -> new DocumentFileReference(tenant,"ref"," ",1));
        reject("DOCUMENT_FILES_REQUIRED",() -> minimal(person,id,List.of()));
        reject("DOCUMENT_FILE_REFERENCE_DUPLICATE",() -> minimal(person,id,List.of(file,file)));
        reject("DOCUMENT_FILE_TENANT_MISMATCH",() -> minimal(person,id,List.of(new DocumentFileReference(new TenantId("foreign"),"ref","pdf",1))));
        reject("DOCUMENT_DATE_ORDER_INVALID",() -> CustomerDocument.createCandidate(person,id,"passport",List.of(file),Optional.empty(),Optional.of(date),Optional.of(date.minusDays(1)),Optional.empty(),Optional.empty(),"actor",instant));
        reject("DOCUMENT_NOTES_TOO_LONG",() -> CustomerDocument.createCandidate(person,id,"passport",List.of(file),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),Optional.of("x".repeat(4001)),"actor",instant));
        reject("DOCUMENT_TYPE_ID_REQUIRED",() -> CustomerDocument.createCandidate(person,id," ",List.of(file),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),"actor",instant));
        reject("DOCUMENT_UPLOADER_REQUIRED",() -> CustomerDocument.createCandidate(person,id,"passport",List.of(file),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty()," ",instant));
        try { doc.files().clear(); throw new AssertionError("Mutable files"); }
        catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("PASS: " + checks + " isolated customer document foundation assertions");
    }
    private static CustomerDocument minimal(Customer c,CustomerDocumentId id,List<DocumentFileReference> files) {
        return CustomerDocument.createCandidate(c,id,"type",files,Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),"actor",Instant.EPOCH);
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Customer document assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected " + code);
    }
}
