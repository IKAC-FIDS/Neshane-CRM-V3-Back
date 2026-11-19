package ir.neshane.customer.domain.document;

import ir.neshane.customer.domain.customer.state.IdentityRevision;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.List;
import java.util.Map;
import java.util.Set;
import static ir.neshane.customer.domain.document.DocumentTypeDefinition.Applicability.*;
import static ir.neshane.customer.domain.document.DocumentTypeDefinition.RequirementStage.*;
import static ir.neshane.customer.domain.document.DocumentTypeDefinition.Status.*;
import static ir.neshane.customer.domain.document.DocumentTypeCatalog.Authority.*;
import static ir.neshane.customer.domain.document.DocumentRequirementPolicy.Outcome.*;

public final class DocumentRequirementPolicySpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var company = Company.createCandidate(new CustomerId("company"),tenant,"C",null);
        var registration = DocumentRequirementPolicy.Operation.REGISTRATION;
        var verification = DocumentRequirementPolicy.Operation.VERIFICATION;
        var base = DocumentRequirementPolicy.initial(DocumentTypeCatalog.empty(tenant,1000),"r1");
        expect(base.forOperation(person,registration,"request").requiredTypes().isEmpty());
        expect(base.forOperation(company,verification,"request").requiredTypes().isEmpty());
        var optional = def(tenant,"passport",PERSON,OPTIONAL,ACTIVE);
        var added = base.addType("r1","r2",optional,MANAGE);
        expect(added.outcome() == ELIGIBLE_CANDIDATE);
        expect(added.auditRequired());
        var p2 = added.proposed().orElseThrow();
        expect(p2.revision().equals("r2"));
        expect(p2.forOperation(person,registration,"request").requiredTypes().isEmpty());
        var snapshot = p2.forOperation(person,registration,"request");
        expect(snapshot.owner().equals(person.identityInfo().owner()));
        expect(snapshot.operation() == registration);
        expect(snapshot.revision().equals("r2"));
        expect(snapshot.requestId().equals("request"));
        expect(p2.assessSnapshot(person,registration,"request",snapshot).outcome() == ELIGIBLE_CANDIDATE);
        var mandatory = def(tenant,"passport",PERSON,REGISTRATION,ACTIVE);
        var p3 = p2.reviseType("r2","r3",mandatory,MANAGE).proposed().orElseThrow();
        expect(p3.forOperation(person,registration,"request2").requiredTypes().equals(List.of(mandatory)));
        expect(p3.forOperation(person,verification,"request2").requiredTypes().isEmpty());
        expect(p3.forOperation(company,registration,"request2").requiredTypes().isEmpty());
        expect(snapshot.requiredTypes().isEmpty());
        expect(p3.assessSnapshot(person,registration,"request",snapshot).code().equals("DOCUMENT_POLICY_SNAPSHOT_STALE"));
        expect(p2.assessSnapshot(person,verification,"request",snapshot).code().equals("DOCUMENT_POLICY_SNAPSHOT_SCOPE_MISMATCH"));
        expect(p2.assessSnapshot(person,registration,"other",snapshot).code().equals("DOCUMENT_POLICY_SNAPSHOT_SCOPE_MISMATCH"));
        expect(p2.assessSnapshot(company,registration,"request",snapshot).code().equals("DOCUMENT_POLICY_SNAPSHOT_SCOPE_MISMATCH"));
        expect(p2.assessSnapshot(person,registration,"request",null).code().equals("DOCUMENT_POLICY_SNAPSHOT_REQUIRED"));
        var companyType = def(tenant,"registration",COMPANY,VERIFICATION,ACTIVE);
        var p4 = p3.addType("r3","r4",companyType,MANAGE).proposed().orElseThrow();
        expect(p4.forOperation(company,verification,"next").requiredTypes().equals(List.of(companyType)));
        expect(p4.forOperation(company,registration,"next").requiredTypes().isEmpty());
        expect(p4.forOperation(person,verification,"next").requiredTypes().isEmpty());
        var both = def(tenant,"shared",BOTH,VERIFICATION,ACTIVE);
        var p5 = p4.addType("r4","r5",both,MANAGE).proposed().orElseThrow();
        expect(p5.forOperation(person,verification,"next").requiredTypes().equals(List.of(both)));
        expect(p5.forOperation(company,verification,"next").requiredTypes().equals(List.of(companyType,both)));
        var p6 = p5.reviseType("r5","r6",optional,MANAGE).proposed().orElseThrow();
        expect(p6.forOperation(person,registration,"next").requiredTypes().isEmpty());
        var inactive = def(tenant,"registration",COMPANY,VERIFICATION,INACTIVE);
        var p7 = p6.reviseType("r6","r7",inactive,MANAGE).proposed().orElseThrow();
        expect(p7.forOperation(company,verification,"next").requiredTypes().equals(List.of(both)));
        expect(p7.catalog().resolveReference(tenant,"registration").equals(inactive));
        expect(p5.catalog().resolveReference(tenant,"registration").equals(companyType));
        var unauthorized = p7.reviseType("r7","r8",mandatory,NONE);
        expect(unauthorized.code().equals("DOCUMENT_TYPE_MANAGEMENT_AUTHORITY_REQUIRED"));
        expect(unauthorized.auditRequired());
        expect(unauthorized.proposed().isEmpty());
        expect(p7.reviseType("stale","r8",mandatory,MANAGE).code().equals("DOCUMENT_POLICY_REVISION_STALE"));
        expect(p7.reviseType("r7","r7",mandatory,MANAGE).code().equals("DOCUMENT_POLICY_NEXT_REVISION_REQUIRED"));
        expect(p7.addType("r7","r8",mandatory,MANAGE).code().equals("DOCUMENT_TYPE_ID_DUPLICATE"));
        var foreign = new TenantId("foreign");
        var foreignPerson = Person.createCandidate(person.id(),foreign,"A","B",null);
        expect(p7.addType("r7","r8",def(foreign,"passport",PERSON,OPTIONAL,ACTIVE),MANAGE).code().equals("DOCUMENT_POLICY_SCOPE_MISMATCH"));
        expect(p7.assessSnapshot(foreignPerson,registration,"request",snapshot).code().equals("DOCUMENT_POLICY_SCOPE_MISMATCH"));
        reject("DOCUMENT_POLICY_SCOPE_MISMATCH",() -> p7.forOperation(foreignPerson,registration,"request"));
        expect(person.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        var verified = IdentityRevision.snapshot(person,"identity-r1",VerificationStatus.VERIFIED);
        expect(verified.verificationStatus() == VerificationStatus.VERIFIED);
        expect(!snapshot.toString().contains("request"));
        try { p5.forOperation(company,verification,"next").requiredTypes().clear(); throw new AssertionError("Mutable policy snapshot"); }
        catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("PASS: " + checks + " isolated document requirement policy assertions");
    }
    private static DocumentTypeDefinition def(TenantId tenant,String id,DocumentTypeDefinition.Applicability scope,DocumentTypeDefinition.RequirementStage stage,DocumentTypeDefinition.Status status) {
        return new DocumentTypeDefinition(tenant,id,id,new LocalizedText(Map.of("fa","مدرک")),new LocalizedText(Map.of()),scope,stage,DocumentTypeDefinition.FileMultiplicity.SINGLE,Set.of("application/pdf"),100,false,status,0);
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Document requirement assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected " + code);
    }
}
