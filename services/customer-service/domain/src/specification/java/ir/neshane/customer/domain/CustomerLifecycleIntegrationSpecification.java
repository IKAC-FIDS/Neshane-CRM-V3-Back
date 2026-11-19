package ir.neshane.customer.domain;

import ir.neshane.customer.domain.lifecycle.ArchiveDependencyAssessment;
import ir.neshane.customer.domain.lifecycle.ArchiveDependencyCatalog;
import ir.neshane.customer.domain.lifecycle.LifecycleReasonCatalog;

import ir.neshane.customer.domain.document.DocumentCompleteness;
import ir.neshane.customer.domain.document.DocumentRequirementPolicy;
import ir.neshane.customer.domain.document.DocumentTypeCatalog;
import ir.neshane.customer.domain.document.DocumentValidity;

import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy;

import ir.neshane.customer.domain.customer.state.CustomerChangeHistory;
import ir.neshane.customer.domain.customer.state.CustomerLifecycleIntegration;
import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.coordination.registration.CustomerRegistration;
import ir.neshane.customer.domain.customer.event.CustomerChangeEventDraft;
import ir.neshane.customer.domain.customer.event.CustomerEventDraft;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.CustomerLifecycle;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static ir.neshane.customer.domain.customer.CustomerLifecycle.Transition.*;
import static ir.neshane.customer.domain.customer.state.CustomerLifecycleIntegration.Authority.*;

final class CustomerLifecycleIntegrationSpecification {
    private static int checks;
    private static final TenantId TENANT=new TenantId("tenant");
    private static final LifecycleReasonCatalog REASONS=LifecycleReasonCatalog.initial();
    private static final ArchiveDependencyCatalog DEPS=ArchiveDependencyCatalog.initial();
    static void run() {
        var company=Company.createCandidate(new CustomerId("company"),TENANT,"Company",null);
        var policy=DocumentRequirementPolicy.initial(DocumentTypeCatalog.empty(TENANT,1000),"p1");
        var reg=CustomerRegistration.assess(company,new CustomerRegistration.Context(TENANT,"request",DuplicateExceptionPolicy.Authority.REGISTER),
            ContactTypeCatalog.empty(),IdentifierTypeCatalog.empty(),Map.of(),Map.of(),DuplicateDetection.Request.of(company,DuplicateDetection.Operation.CREATE,"request"),
            DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),"e1",null,null,policy,policy.forOperation(company,DocumentRequirementPolicy.Operation.REGISTRATION,"request"),List.of(),
            LocalDate.of(2026,11,19),DocumentValidity.ExpiryBoundary.UNRESOLVED,DocumentCompleteness.CountingConvention.ANY_CURRENT_PROVIDED).proposed().orElseThrow();
        var facts=CustomerChangeHistory.start(reg,"r1",new LifecycleHistory.Context("registration","actor",Instant.EPOCH,"registration"));
        var current=facts.current(); var history=LifecycleHistory.empty(company); var version=current.version();
        var ctx=context(TENANT,TRANSITION,DEACTIVATE,"CUSTOMER_REQUEST");
        var result=assess(current,version,ctx,history,List.of());
        expect(result.outcome()==CustomerLifecycleIntegration.Outcome.ELIGIBLE_CANDIDATE); expect(result.auditRequired());
        var candidate=result.proposed().orElseThrow();
        expect(candidate.previous()==current); expect(candidate.record().status()==CustomerStatus.INACTIVE);
        expect(candidate.record().version().value()==2); expect(candidate.record().identity()==current.identity());
        expect(candidate.record().identity().verificationStatus()==VerificationStatus.NOT_VERIFIED);
        expect(history.entries().isEmpty()); expect(candidate.history().entries().size()==1);
        var entry=candidate.history().entries().getFirst(); var event=candidate.event();
        expect(entry.previousStatus()==CustomerStatus.ACTIVE); expect(entry.newStatus()==CustomerStatus.INACTIVE);
        expect(entry.reason().reasonId().equals("CUSTOMER_REQUEST")); expect(event.eventType()==CustomerEventDraft.Type.CustomerDeactivated);
        expect(event.aggregateVersion()==2); expect(event.aggregateId().equals(company.id())); expect(event.tenantId().equals(TENANT));
        expect(event.correlationId().equals("request")); expect(event.occurredAt().equals(entry.context().occurredAt()));
        expect(event.payload().previousStatus().orElseThrow()==CustomerStatus.ACTIVE); expect(event.payload().newStatus()==CustomerStatus.INACTIVE);
        var advanced=facts.advanceLifecycle(candidate);
        expect(advanced.current()==candidate.record()); expect(advanced.entries().size()==1);
        expect(CustomerChangeEventDraft.fromHistory(advanced).size()==1); expect(facts.current().version().value()==1);
        blocked(assess(current,null,ctx,history,List.of()),"CUSTOMER_RECORD_VERSION_REQUIRED");
        blocked(assess(current,candidate.record().version(),ctx,history,List.of()),"CUSTOMER_RECORD_VERSION_STALE");
        blocked(assess(current,version,context(new TenantId("foreign"),TRANSITION,DEACTIVATE,"CUSTOMER_REQUEST"),history,List.of()),"CUSTOMER_RECORD_SCOPE_MISMATCH");
        var denied=assess(current,version,context(TENANT,NONE,DEACTIVATE,"CUSTOMER_REQUEST"),history,List.of());
        blocked(denied,"CUSTOMER_LIFECYCLE_AUTHORITY_REQUIRED"); expect(denied.auditRequired());
        blocked(assess(current,version,context(TENANT,TRANSITION,DEACTIVATE,"OTHER"),history,List.of()),"LIFECYCLE_REASON_COMMENT_REQUIRED");
        blocked(assess(candidate.record(),candidate.record().version(),ctx,candidate.history(),List.of()),"CUSTOMER_TRANSITION_NOT_ALLOWED");
        blocked(assess(candidate.record(),candidate.record().version(),context(TENANT,TRANSITION,REACTIVATE,"COOPERATION_RESUMED"),candidate.history(),List.of()),"CUSTOMER_REVALIDATION_UNRESOLVED");
        var archive=context(TENANT,TRANSITION,ARCHIVE,"CUSTOMER_REQUEST");
        blocked(assess(current,version,archive,history,List.of()),"CUSTOMER_ARCHIVE_DEPENDENCIES_BLOCKED");
        var request=new ArchiveDependencyAssessment.Request(TENANT,company.id(),"request");
        var cleared=DEPS.definitions().values().stream().map(d -> new ArchiveDependencyAssessment.Evidence(request,d.code(),d.ownerService(),ArchiveDependencyAssessment.Status.CLEARED,"e-"+d.code(),Instant.EPOCH)).toList();
        blocked(assess(current,version,archive,history,cleared),"CUSTOMER_ARCHIVE_CLEARANCE_UNRESOLVED");
        var archived=new CustomerUpdatePolicy.Current(current.identity(),CustomerStatus.ARCHIVED,version);
        blocked(assess(archived,version,context(TENANT,TRANSITION,RESTORE,"ARCHIVE_CORRECTION"),history,List.of()),"CUSTOMER_RESTORATION_CONTRACT_UNRESOLVED");
        expect(current.status()==CustomerStatus.ACTIVE); expect(current.version().value()==1); expect(history.entries().isEmpty());
        System.out.println("PASS: "+checks+" isolated customer lifecycle integration assertions");
    }
    private static CustomerLifecycleIntegration.Context context(TenantId tenant,CustomerLifecycleIntegration.Authority authority,CustomerLifecycle.Transition transition,String reason) {
        return new CustomerLifecycleIntegration.Context(tenant,authority,transition,true,reason,null,new LifecycleHistory.Context("h1","actor",Instant.ofEpochSecond(1),"request"),"event");
    }
    private static CustomerLifecycleIntegration.Assessment assess(CustomerUpdatePolicy.Current c,CustomerRecordVersion version,CustomerLifecycleIntegration.Context context,LifecycleHistory history,List<ArchiveDependencyAssessment.Evidence> evidence) {
        return CustomerLifecycleIntegration.assess(c,version,context,REASONS,history,DEPS,evidence);
    }
    private static void blocked(CustomerLifecycleIntegration.Assessment result,String code) { expect(result.outcome()==CustomerLifecycleIntegration.Outcome.BLOCKED); expect(result.code().equals(code)); expect(result.proposed().isEmpty()); }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Lifecycle integration assertion "+(checks+1)); checks++; }
}
