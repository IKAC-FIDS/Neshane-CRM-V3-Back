package ir.neshane.customer.domain.document;

import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

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
import static ir.neshane.customer.domain.document.DocumentTypeDefinition.Applicability.*;
import static ir.neshane.customer.domain.document.DocumentTypeDefinition.RequirementStage.*;
import static ir.neshane.customer.domain.document.DocumentTypeDefinition.FileMultiplicity.*;
import static ir.neshane.customer.domain.document.DocumentTypeDefinition.Status.*;
import static ir.neshane.customer.domain.document.DocumentTypeCatalog.Authority.*;

public final class DocumentTypeCatalogSpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("tenant");
        var other = new TenantId("foreign");
        var base = DocumentTypeCatalog.empty(tenant,1000);
        var passport = def(tenant,"p"," passport ","گذرنامه",PERSON,OPTIONAL,SINGLE,100,true,ACTIVE,2);
        var companyType = def(tenant,"c","company","ثبت شرکت",COMPANY,VERIFICATION,MULTIPLE,500,false,ACTIVE,1);
        var common = def(tenant,"a","common","مدرک",BOTH,REGISTRATION,MULTIPLE,200,false,ACTIVE,0);
        expect(passport.code().equals("PASSPORT"));
        expect(passport.title().exact("fa").orElseThrow().equals("گذرنامه"));
        expect(passport.title().exact("en").isEmpty());
        expect(passport.applicability().appliesTo(IdentityOwner.Kind.PERSON));
        expect(!passport.applicability().appliesTo(IdentityOwner.Kind.COMPANY));
        expect(common.applicability().appliesTo(IdentityOwner.Kind.COMPANY));
        expect(common.requirementStage() == REGISTRATION);
        expect(passport.multiplicity() == SINGLE);
        expect(passport.expiryApplicable());
        expect(passport.maximumFileSize() == 100);
        expect(passport.allowedFormats().equals(Set.of("application/pdf")));
        expect(base.selectable(tenant,IdentityOwner.Kind.PERSON).isEmpty());
        var catalog = base.addCandidate(passport,MANAGE).addCandidate(companyType,MANAGE).addCandidate(common,MANAGE);
        expect(base.selectable(tenant,IdentityOwner.Kind.PERSON).isEmpty());
        expect(catalog.tenantId().equals(tenant));
        expect(catalog.selectable(tenant,IdentityOwner.Kind.PERSON).equals(List.of(common,passport)));
        expect(catalog.selectable(tenant,IdentityOwner.Kind.COMPANY).equals(List.of(common,companyType)));
        reject("DOCUMENT_TYPE_ID_DUPLICATE",() -> catalog.addCandidate(passport,MANAGE));
        reject("DOCUMENT_TYPE_CODE_RESERVED",() -> catalog.addCandidate(def(tenant,"p2","Passport","مدرک",BOTH,OPTIONAL,SINGLE,100,true,ACTIVE,1),MANAGE));
        reject("DOCUMENT_TYPE_MANAGEMENT_AUTHORITY_REQUIRED",() -> base.addCandidate(passport,NONE));
        reject("DOCUMENT_TYPE_PLATFORM_SIZE_EXCEEDED",() -> base.addCandidate(def(tenant,"big","BIG","مدرک",BOTH,OPTIONAL,SINGLE,1001,false,ACTIVE,1),MANAGE));
        reject("DOCUMENT_PLATFORM_SIZE_UNRESOLVED",() -> DocumentTypeCatalog.empty(tenant,0));
        reject("DOCUMENT_TYPE_SCOPE_MISMATCH",() -> catalog.selectable(other,IdentityOwner.Kind.PERSON));
        reject("DOCUMENT_TYPE_SCOPE_MISMATCH",() -> catalog.resolveReference(other,"p"));
        var foreign = def(other,"p","passport","مدرک",PERSON,OPTIONAL,SINGLE,100,true,ACTIVE,1);
        reject("DOCUMENT_TYPE_SCOPE_MISMATCH",() -> catalog.addCandidate(foreign,MANAGE));
        expect(DocumentTypeCatalog.empty(other,1000).addCandidate(foreign,MANAGE).resolveReference(other,"p").equals(foreign));
        reject("DOCUMENT_TYPE_CODE_IMMUTABLE",() -> catalog.reviseCandidate(def(tenant,"p","CHANGED","جدید",PERSON,OPTIONAL,SINGLE,100,true,ACTIVE,1),MANAGE));
        reject("DOCUMENT_TYPE_UNKNOWN",() -> catalog.reviseCandidate(def(tenant,"unknown","NEW","جدید",PERSON,OPTIONAL,SINGLE,100,true,ACTIVE,1),MANAGE));
        var revised = def(tenant,"p","passport","عنوان جدید",PERSON,VERIFICATION,SINGLE,100,true,INACTIVE,2);
        var inactive = catalog.reviseCandidate(revised,MANAGE);
        expect(inactive.resolveReference(tenant,"p").equals(revised));
        expect(catalog.resolveReference(tenant,"p").equals(passport));
        expect(inactive.selectable(tenant,IdentityOwner.Kind.PERSON).equals(List.of(common)));
        reject("DOCUMENT_TYPE_CODE_RESERVED",() -> inactive.addCandidate(def(tenant,"p2","passport","مدرک",PERSON,OPTIONAL,SINGLE,100,true,ACTIVE,1),MANAGE));
        var person = Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var company = Company.createCandidate(new CustomerId("company"),tenant,"C",null);
        var f1 = new DocumentFileReference(tenant,"f1","application/pdf",100);
        var f2 = new DocumentFileReference(tenant,"f2","application/pdf",50);
        var doc = document(person,"p",List.of(f1),Optional.empty());
        expect(catalog.assessUpload(doc).outcome() == DocumentTypeCatalog.Outcome.ELIGIBLE_CANDIDATE);
        expect(catalog.assessUpload(doc).auditRequired());
        expect(inactive.assessUpload(doc).code().equals("DOCUMENT_TYPE_NOT_SELECTABLE"));
        expect(catalog.assessUpload(document(company,"p",List.of(f1),Optional.empty())).code().equals("DOCUMENT_TYPE_NOT_APPLICABLE"));
        expect(catalog.assessUpload(document(person,"p",List.of(f1,f2),Optional.empty())).code().equals("DOCUMENT_TYPE_SINGLE_FILE_REQUIRED"));
        expect(catalog.assessUpload(document(company,"c",List.of(f1,f2),Optional.empty())).outcome() == DocumentTypeCatalog.Outcome.ELIGIBLE_CANDIDATE);
        expect(catalog.assessUpload(document(person,"p",List.of(new DocumentFileReference(tenant,"f","image/png",50)),Optional.empty())).code().equals("DOCUMENT_TYPE_FILE_FORMAT_NOT_ALLOWED"));
        expect(catalog.assessUpload(document(person,"p",List.of(new DocumentFileReference(tenant,"f","application/pdf",101)),Optional.empty())).code().equals("DOCUMENT_TYPE_FILE_SIZE_EXCEEDED"));
        expect(catalog.assessUpload(document(company,"c",List.of(f1),Optional.of(LocalDate.of(2030,1,1)))).code().equals("DOCUMENT_TYPE_EXPIRY_NOT_APPLICABLE"));
        expect(catalog.assessUpload(document(person,"p",List.of(f1),Optional.of(LocalDate.of(2030,1,1)))).outcome() == DocumentTypeCatalog.Outcome.ELIGIBLE_CANDIDATE);
        expect(catalog.assessUpload(document(person,"missing",List.of(f1),Optional.empty())).code().equals("DOCUMENT_TYPE_UNKNOWN"));
        var foreignPerson = Person.createCandidate(person.id(),other,"A","B",null);
        expect(catalog.assessUpload(document(foreignPerson,"p",List.of(new DocumentFileReference(other,"f","application/pdf",100)),Optional.empty())).code().equals("DOCUMENT_TYPE_SCOPE_MISMATCH"));
        expect(person.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        reject("DOCUMENT_TYPE_PERSIAN_TITLE_REQUIRED",() -> new DocumentTypeDefinition(tenant,"x","X",new LocalizedText(Map.of("en","Title")),new LocalizedText(Map.of()),BOTH,OPTIONAL,SINGLE,Set.of("pdf"),10,false,ACTIVE,0));
        System.out.println("PASS: " + checks + " isolated document type catalog assertions");
    }
    private static DocumentTypeDefinition def(TenantId tenant,String id,String code,String title,DocumentTypeDefinition.Applicability scope,
            DocumentTypeDefinition.RequirementStage stage,DocumentTypeDefinition.FileMultiplicity files,long max,boolean expiry,DocumentTypeDefinition.Status status,int order) {
        return new DocumentTypeDefinition(tenant,id,code,new LocalizedText(Map.of("fa",title)),new LocalizedText(Map.of()),scope,stage,files,Set.of("application/pdf"),max,expiry,status,order);
    }
    private static CustomerDocument document(Customer customer,String type,List<DocumentFileReference> files,Optional<LocalDate> expiry) {
        return CustomerDocument.createCandidate(customer,new CustomerDocumentId("document"),type,files,Optional.empty(),Optional.empty(),expiry,Optional.empty(),Optional.empty(),"actor",Instant.EPOCH);
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Document type catalog assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected " + code);
    }
}
