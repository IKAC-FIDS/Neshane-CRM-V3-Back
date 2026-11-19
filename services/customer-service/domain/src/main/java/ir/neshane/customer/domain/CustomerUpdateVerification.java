package ir.neshane.customer.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Composes update eligibility and evidence impact without committing a customer record. */
public final class CustomerUpdateVerification {
    public enum Outcome { ELIGIBLE_CANDIDATE, UNCHANGED, BLOCKED }
    public static final class Candidate {
        private final CustomerUpdatePolicy.Current previous;
        private final CustomerUpdatePolicy.Current record;
        private final VerificationHistory history;
        private final Set<CriticalFieldChangeAssessment.Field> fields;
        private final boolean differentVerifierRequired;
        private Candidate(CustomerUpdatePolicy.Current previous,CustomerUpdatePolicy.Current record,VerificationHistory history,
                Set<CriticalFieldChangeAssessment.Field> fields,boolean differentVerifierRequired) {
            this.previous = previous;
            this.record = record;
            this.history = history;
            this.fields = Set.copyOf(fields);
            this.differentVerifierRequired = differentVerifierRequired;
        }
        public CustomerUpdatePolicy.Current previous() { return previous; }
        public CustomerUpdatePolicy.Current record() { return record; }
        public VerificationHistory history() { return history; }
        public Set<CriticalFieldChangeAssessment.Field> fields() { return fields; }
        public boolean differentVerifierRequired() { return differentVerifierRequired; }
        @Override public String toString() { return "CustomerUpdateVerification.Candidate[redacted]"; }
    }
    public record Assessment(Outcome outcome,String code,Optional<Candidate> proposed,boolean auditRequired) {}
    private CustomerUpdateVerification() {}

    public static Assessment assess(CustomerUpdatePolicy.Current current,CustomerRecordVersion expected,Customer proposed,
            CustomerUpdatePolicy.Context context, ContactTypeCatalog contacts,IdentifierTypeCatalog identifiers,
            Map<String,ValidationProfile> contactProfiles,Map<String,ValidationProfile> identifierProfiles,
            DuplicateDetection.Request request,DuplicateDetection.Coverage coverage,List<DuplicateDetection.Comparison> comparisons,
            VerificationHistory history,String expectedIdentityRevision,String nextIdentityRevision,
            CriticalFieldChangeAssessment.ExceptionScope exceptionScope,String reason,IdentityRevisionAssessment.Impact impact,
            IdentityRevisionAssessment.IdentifierCorrection correction,LifecycleHistory.Context historyContext) {
        var update = CustomerUpdatePolicy.assess(current,expected,proposed,context,contacts,identifiers,
            contactProfiles,identifierProfiles,request,coverage,comparisons);
        if (update.outcome() == CustomerUpdatePolicy.Outcome.BLOCKED) return blocked(update.code(),update.auditRequired());
        if (history == null || history.current() != current.identity()) return blocked("CUSTOMER_UPDATE_HISTORY_MISMATCH",false);
        if (!current.identity().value().equals(expectedIdentityRevision)) return blocked("IDENTITY_REVISION_STALE",false);
        if (update.outcome() == CustomerUpdatePolicy.Outcome.UNCHANGED)
            return new Assessment(Outcome.UNCHANGED,"CUSTOMER_UPDATE_UNCHANGED",Optional.empty(),false);
        var preliminary = update.proposed().orElseThrow();
        if (preliminary.duplicateAssessment().outcome() != DuplicateDetection.Outcome.NO_MATCH)
            return blocked("CUSTOMER_UPDATE_DUPLICATE_WARNING_UNRESOLVED",true);
        if (exceptionScope == null || impact == null || correction == null || historyContext == null)
            return blocked("CUSTOMER_UPDATE_CONTEXT_REQUIRED",false);
        var fields = CriticalFieldChangeAssessment.assess(current.identity().candidate(),proposed,exceptionScope,reason);
        if (fields.outcome() != CriticalFieldChangeAssessment.Outcome.ELIGIBLE_CANDIDATE)
            return blocked(fields.code(),fields.auditRequired());
        try {
            var revision = IdentityRevisionAssessment.assess(current.identity(),expectedIdentityRevision,proposed,nextIdentityRevision,
                IdentityRevisionAssessment.Authority.UPDATE,impact,correction,reason);
            if (revision.outcome() != IdentityRevisionAssessment.Outcome.ELIGIBLE_CANDIDATE)
                return blocked(revision.code(),revision.auditRequired());
            var next = history.appendIdentityChange(expectedIdentityRevision,proposed,nextIdentityRevision,
                IdentityRevisionAssessment.Authority.UPDATE,impact,correction,reason,historyContext);
            var record = new CustomerUpdatePolicy.Current(next.current(),preliminary.preservedStatus(),preliminary.nextVersion());
            return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"CUSTOMER_UPDATE_VERIFICATION_CANDIDATE",
                Optional.of(new Candidate(current,record,next,fields.fields(),revision.differentVerifierRequired())),true);
        } catch (IllegalArgumentException ex) {
            return blocked(ex.getMessage(),false);
        }
    }
    private static Assessment blocked(String code,boolean audit) {
        return new Assessment(Outcome.BLOCKED,code,Optional.empty(),audit);
    }
}
