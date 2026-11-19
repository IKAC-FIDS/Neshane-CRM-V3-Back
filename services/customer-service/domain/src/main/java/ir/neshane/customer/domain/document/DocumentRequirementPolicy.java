package ir.neshane.customer.domain.document;

import ir.neshane.customer.domain.customer.Customer;

import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable synthetic policy revision. Does not decide document completeness or legal prerequisites. */
public final class DocumentRequirementPolicy {
    public enum Operation { REGISTRATION, VERIFICATION }
    public enum Outcome { ELIGIBLE_CANDIDATE, BLOCKED }
    public record Change(Outcome outcome,String code,Optional<DocumentRequirementPolicy> proposed,boolean auditRequired) {}
    public record Assessment(Outcome outcome,String code) {}
    public static final class Snapshot {
        private final DocumentRequirementPolicy policy;
        private final IdentityOwner owner;
        private final Operation operation;
        private final String requestId;
        private final List<DocumentTypeDefinition> requiredTypes;
        private Snapshot(DocumentRequirementPolicy policy,IdentityOwner owner,Operation operation,String requestId,List<DocumentTypeDefinition> requiredTypes) {
            this.policy = policy;
            this.owner = owner;
            this.operation = operation;
            this.requestId = requestId;
            this.requiredTypes = List.copyOf(requiredTypes);
        }
        public IdentityOwner owner() { return owner; }
        public Operation operation() { return operation; }
        public String requestId() { return requestId; }
        public String revision() { return policy.revision; }
        public List<DocumentTypeDefinition> requiredTypes() { return requiredTypes; }
        @Override public String toString() { return "DocumentRequirementPolicy.Snapshot[protected, redacted]"; }
    }
    private final DocumentTypeCatalog catalog;
    private final String revision;
    private DocumentRequirementPolicy(DocumentTypeCatalog catalog,String revision) {
        this.catalog = Objects.requireNonNull(catalog,"DOCUMENT_CATALOG_REQUIRED");
        IdentifierText.requireNonBlank(revision,"DOCUMENT_POLICY_REVISION_REQUIRED");
        this.revision = revision;
    }
    public static DocumentRequirementPolicy initial(DocumentTypeCatalog catalog,String revision) {
        return new DocumentRequirementPolicy(catalog,revision);
    }
    public DocumentTypeCatalog catalog() { return catalog; }
    public String revision() { return revision; }
    public Snapshot forOperation(Customer customer,Operation operation,String requestId) {
        Objects.requireNonNull(customer,"CUSTOMER_REQUIRED");
        Objects.requireNonNull(operation,"DOCUMENT_OPERATION_REQUIRED");
        IdentifierText.requireNonBlank(requestId,"DOCUMENT_REQUEST_ID_REQUIRED");
        var owner = customer.identityInfo().owner();
        if (!catalog.tenantId().equals(owner.tenantId())) throw new IllegalArgumentException("DOCUMENT_POLICY_SCOPE_MISMATCH");
        var stage = operation == Operation.REGISTRATION ? DocumentTypeDefinition.RequirementStage.REGISTRATION : DocumentTypeDefinition.RequirementStage.VERIFICATION;
        var required = catalog.selectable(owner.tenantId(),owner.kind()).stream().filter(d -> d.requirementStage() == stage).toList();
        return new Snapshot(this,owner,operation,requestId,required);
    }
    public Assessment assessSnapshot(Customer customer,Operation operation,String requestId,Snapshot snapshot) {
        Objects.requireNonNull(customer,"CUSTOMER_REQUIRED");
        Objects.requireNonNull(operation,"DOCUMENT_OPERATION_REQUIRED");
        IdentifierText.requireNonBlank(requestId,"DOCUMENT_REQUEST_ID_REQUIRED");
        if (!catalog.tenantId().equals(customer.tenantId())) return blocked("DOCUMENT_POLICY_SCOPE_MISMATCH");
        if (snapshot == null) return blocked("DOCUMENT_POLICY_SNAPSHOT_REQUIRED");
        if (!snapshot.owner.equals(customer.identityInfo().owner()) || snapshot.operation != operation || !snapshot.requestId.equals(requestId))
            return blocked("DOCUMENT_POLICY_SNAPSHOT_SCOPE_MISMATCH");
        if (snapshot.policy != this) return blocked("DOCUMENT_POLICY_SNAPSHOT_STALE");
        return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"DOCUMENT_POLICY_SNAPSHOT_CANDIDATE");
    }
    public Change addType(String expectedRevision,String nextRevision,DocumentTypeDefinition definition,DocumentTypeCatalog.Authority authority) {
        return change(expectedRevision,nextRevision,definition,authority,true);
    }
    public Change reviseType(String expectedRevision,String nextRevision,DocumentTypeDefinition definition,DocumentTypeCatalog.Authority authority) {
        return change(expectedRevision,nextRevision,definition,authority,false);
    }
    private Change change(String expected,String next,DocumentTypeDefinition def,DocumentTypeCatalog.Authority authority,boolean add) {
        IdentifierText.requireNonBlank(expected,"DOCUMENT_POLICY_REVISION_REQUIRED");
        IdentifierText.requireNonBlank(next,"DOCUMENT_POLICY_REVISION_REQUIRED");
        Objects.requireNonNull(def,"DOCUMENT_TYPE_REQUIRED");
        if (!catalog.tenantId().equals(def.tenantId())) return denied("DOCUMENT_POLICY_SCOPE_MISMATCH",false);
        if (authority != DocumentTypeCatalog.Authority.MANAGE) return denied("DOCUMENT_TYPE_MANAGEMENT_AUTHORITY_REQUIRED",true);
        if (!revision.equals(expected)) return denied("DOCUMENT_POLICY_REVISION_STALE",false);
        if (revision.equals(next)) return denied("DOCUMENT_POLICY_NEXT_REVISION_REQUIRED",false);
        try {
            var updated = add ? catalog.addCandidate(def,authority) : catalog.reviseCandidate(def,authority);
            return new Change(Outcome.ELIGIBLE_CANDIDATE,"DOCUMENT_POLICY_CHANGE_CANDIDATE",Optional.of(new DocumentRequirementPolicy(updated,next)),true);
        } catch (IllegalArgumentException ex) {
            return denied(ex.getMessage(),false);
        }
    }
    private static Change denied(String code,boolean audit) { return new Change(Outcome.BLOCKED,code,Optional.empty(),audit); }
    private static Assessment blocked(String code) { return new Assessment(Outcome.BLOCKED,code); }
    @Override public String toString() { return "DocumentRequirementPolicy[protected, redacted]"; }
}
