package ir.neshane.customer.domain;

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
import static ir.neshane.customer.domain.DocumentCompleteness.CountingConvention.ANY_CURRENT_PROVIDED;
import static ir.neshane.customer.domain.DocumentValidity.ExpiryBoundary.*;
import static ir.neshane.customer.domain.CustomerDocument.State.*;
import static ir.neshane.customer.domain.DocumentTypeDefinition.Applicability.*;
import static ir.neshane.customer.domain.DocumentTypeDefinition.RequirementStage.*;

final class DocumentCompletenessSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var company = Company.createCandidate(new CustomerId("company"),tenant,"C",null);
        var date = LocalDate.of(2026,11,19);
        var catalog = DocumentTypeCatalog.empty(tenant,1000);
        for (var type : List.of(type(tenant,"identity",PERSON,VERIFICATION),type(tenant,"shared",BOTH,VERIFICATION),
                type(tenant,"company",COMPANY,VERIFICATION),type(tenant,"registration",BOTH,REGISTRATION),type(tenant,"optional",BOTH,OPTIONAL)))
            catalog = catalog.addCandidate(type,DocumentTypeCatalog.Authority.MANAGE);
        var policy = DocumentRequirementPolicy.initial(catalog,"r1");
        var operation = DocumentRequirementPolicy.Operation.VERIFICATION;
        var snapshot = policy.forOperation(person,operation,"request");
        var missing = assess(person,policy,snapshot,List.of(),date).summary().orElseThrow();
        expect(missing.totalRequired()==2);
        expect(missing.completedRequired().orElseThrow()==0);
        expect(missing.items().stream().allMatch(i -> i.observedStates().equals(Set.of(NOT_PROVIDED))));
        var identity = history(person,catalog,"identity","d1",Optional.empty(),1);
        var otherIdentity = history(person,catalog,"identity","d2",Optional.empty(),1);
        var expired = history(person,catalog,"shared","d3",Optional.of(date.minusDays(1)),1);
        var optional = history(person,catalog,"optional","d4",Optional.empty(),1);
        var partial = assess(person,policy,snapshot,List.of(identity,otherIdentity,expired,optional),date).summary().orElseThrow();
        expect(partial.totalRequired()==2);
        expect(partial.completedRequired().orElseThrow()==1);
        expect(partial.items().getFirst().observedStates().equals(Set.of(PROVIDED)));
        expect(partial.items().getLast().observedStates().equals(Set.of(EXPIRED)));
        var replacement = doc(person,"shared","d3",Optional.empty(),2);
        var replaced = expired.replace(1,replacement,catalog,DocumentVersionHistory.Authority.REPLACE).proposed().orElseThrow();
        var full = assess(person,policy,snapshot,List.of(identity,replaced),date).summary().orElseThrow();
        expect(full.completedRequired().orElseThrow()==2);
        expect(full.items().getLast().observedStates().equals(Set.of(PROVIDED)));
        var invalid = replaced.invalidate(new DocumentVersionHistory.Invalidation(replacement.owner(),replacement.id(),2,"actor","reason",
            Instant.ofEpochSecond(3),Instant.ofEpochSecond(3),"corr"),DocumentVersionHistory.Authority.INVALIDATE).proposed().orElseThrow();
        var incomplete = assess(person,policy,snapshot,List.of(identity,invalid),date).summary().orElseThrow();
        expect(incomplete.completedRequired().orElseThrow()==1);
        expect(incomplete.items().getLast().observedStates().equals(Set.of(INVALIDATED)));
        var boundary = history(person,catalog,"shared","d5",Optional.of(date),1);
        var unresolved = assess(person,policy,snapshot,List.of(identity,boundary),date);
        expect(unresolved.outcome()==DocumentCompleteness.Outcome.UNRESOLVED);
        expect(unresolved.summary().orElseThrow().completedRequired().isEmpty());
        expect(unresolved.summary().orElseThrow().items().getLast().unresolvedValidity());
        expect(DocumentCompleteness.assess(person,policy,operation,"request",snapshot,List.of(identity,boundary),date,LAST_VALID_DAY,ANY_CURRENT_PROVIDED)
            .summary().orElseThrow().completedRequired().orElseThrow()==2);
        expect(DocumentCompleteness.assess(person,policy,operation,"request",snapshot,List.of(identity,boundary),date,FIRST_INVALID_DAY,ANY_CURRENT_PROVIDED)
            .summary().orElseThrow().completedRequired().orElseThrow()==1);
        var mixed = assess(person,policy,snapshot,List.of(identity,replaced,boundary),date).summary().orElseThrow();
        expect(mixed.completedRequired().orElseThrow()==2);
        expect(mixed.items().getLast().unresolvedValidity());
        var mixedStates = assess(person,policy,snapshot,List.of(identity,expired,history(person,catalog,"shared","d6",Optional.empty(),1)),date).summary().orElseThrow();
        expect(mixedStates.items().getLast().observedStates().equals(Set.of(EXPIRED,PROVIDED)));
        expect(mixedStates.completedRequired().orElseThrow()==2);
        expect(assess(person,policy,snapshot,List.of(expired,replaced),date).code().equals("DOCUMENT_COMPLETENESS_DUPLICATE_HISTORY"));
        expect(assess(person,policy,snapshot,List.of(history(company,catalog,"shared","d7",Optional.empty(),1)),date).code().equals("DOCUMENT_COMPLETENESS_SCOPE_MISMATCH"));
        var foreignTenant = new TenantId("foreign");
        var foreign = Person.createCandidate(person.id(),foreignTenant,"A","B",null);
        var foreignCatalog = DocumentTypeCatalog.empty(foreignTenant,1000).addCandidate(type(foreignTenant,"identity",PERSON,VERIFICATION),DocumentTypeCatalog.Authority.MANAGE);
        expect(assess(person,policy,snapshot,List.of(history(foreign,foreignCatalog,"identity","d1",Optional.empty(),1)),date).outcome()==DocumentCompleteness.Outcome.BLOCKED);
        var newPolicy = policy.reviseType("r1","r2",type(tenant,"identity",PERSON,OPTIONAL),DocumentTypeCatalog.Authority.MANAGE).proposed().orElseThrow();
        expect(assess(person,newPolicy,snapshot,List.of(identity),date).code().equals("DOCUMENT_POLICY_SNAPSHOT_STALE"));
        expect(DocumentCompleteness.assess(person,policy,operation,"different",snapshot,List.of(),date,UNRESOLVED,ANY_CURRENT_PROVIDED).summary().isEmpty());
        expect(DocumentCompleteness.assess(person,policy,DocumentRequirementPolicy.Operation.REGISTRATION,"request",snapshot,List.of(),date,UNRESOLVED,ANY_CURRENT_PROVIDED).summary().isEmpty());
        expect(assess(person,policy,null,List.of(),date).summary().isEmpty());
        var companySnapshot = policy.forOperation(company,operation,"request");
        expect(assess(company,policy,companySnapshot,List.of(),date).summary().orElseThrow().items().stream().map(DocumentCompleteness.Item::typeId).toList().equals(List.of("company","shared")));
        var reg = DocumentRequirementPolicy.Operation.REGISTRATION;
        var regSnapshot = policy.forOperation(person,reg,"request");
        expect(DocumentCompleteness.assess(person,policy,reg,"request",regSnapshot,List.of(identity),date,UNRESOLVED,ANY_CURRENT_PROVIDED).summary().orElseThrow().totalRequired()==1);
        var empty = DocumentRequirementPolicy.initial(DocumentTypeCatalog.empty(tenant,1000),"empty");
        var zero = assess(person,empty,empty.forOperation(person,operation,"request"),List.of(),date).summary().orElseThrow();
        expect(zero.totalRequired()==0);
        expect(zero.completedRequired().orElseThrow()==0);
        expect(zero.items().isEmpty());
        expect(DocumentCompleteness.assess(person,policy,operation,"request",snapshot,List.of(identity),date,UNRESOLVED,DocumentCompleteness.CountingConvention.UNRESOLVED).outcome()==DocumentCompleteness.Outcome.UNRESOLVED);
        expect(person.verificationStatus()==VerificationStatus.NOT_VERIFIED);
        expect(!full.toString().contains("identity"));
        try { full.items().clear(); throw new AssertionError("Mutable items"); } catch (UnsupportedOperationException expected) { checks++; }
        try { full.items().getFirst().observedStates().clear(); throw new AssertionError("Mutable states"); } catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("PASS: "+checks+" isolated document completeness assertions");
    }
    private static DocumentCompleteness.Assessment assess(Customer customer,DocumentRequirementPolicy policy,DocumentRequirementPolicy.Snapshot snapshot,List<DocumentVersionHistory> histories,LocalDate date) {
        return DocumentCompleteness.assess(customer,policy,DocumentRequirementPolicy.Operation.VERIFICATION,"request",snapshot,histories,date,UNRESOLVED,ANY_CURRENT_PROVIDED);
    }
    private static DocumentTypeDefinition type(TenantId tenant,String id,DocumentTypeDefinition.Applicability scope,DocumentTypeDefinition.RequirementStage stage) {
        return new DocumentTypeDefinition(tenant,id,id,new LocalizedText(Map.of("fa","مدرک")),new LocalizedText(Map.of()),scope,stage,
            DocumentTypeDefinition.FileMultiplicity.SINGLE,Set.of("application/pdf"),100,true,DocumentTypeDefinition.Status.ACTIVE,0);
    }
    private static CustomerDocument doc(Customer customer,String type,String id,Optional<LocalDate> expiry,long time) {
        return CustomerDocument.createCandidate(customer,new CustomerDocumentId(id),type,List.of(new DocumentFileReference(customer.tenantId(),id+"-"+time,"application/pdf",100)),
            Optional.empty(),Optional.empty(),expiry,Optional.empty(),Optional.empty(),"uploader",Instant.ofEpochSecond(time));
    }
    private static DocumentVersionHistory history(Customer customer,DocumentTypeCatalog catalog,String type,String id,Optional<LocalDate> expiry,long time) {
        return DocumentVersionHistory.start(doc(customer,type,id,expiry,time),catalog,DocumentVersionHistory.Authority.UPLOAD).proposed().orElseThrow();
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Document completeness assertion "+(checks+1));
        checks++;
    }
}
