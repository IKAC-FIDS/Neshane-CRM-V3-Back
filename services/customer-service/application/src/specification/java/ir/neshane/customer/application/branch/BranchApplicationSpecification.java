package ir.neshane.customer.application.branch;

import ir.neshane.customer.application.branch.registration.command.RegisterBranchCandidateCommand;
import ir.neshane.customer.application.branch.registration.command.RegisterBranchCandidateResult;
import ir.neshane.customer.application.branch.registration.usecase.RegisterBranchCandidate;
import ir.neshane.customer.application.branch.update.command.UpdateBranchCandidateCommand;
import ir.neshane.customer.application.branch.update.command.UpdateBranchCandidateResult;
import ir.neshane.customer.application.branch.update.usecase.UpdateBranchCandidate;
import ir.neshane.customer.application.port.in.RegisterBranchCandidateUseCase;
import ir.neshane.customer.application.port.in.UpdateBranchCandidateUseCase;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchRecordVersion;
import ir.neshane.customer.domain.branch.model.BranchStatus;
import ir.neshane.customer.domain.branch.state.BranchRegistration;
import ir.neshane.customer.domain.branch.state.BranchUpdatePolicy;
import ir.neshane.customer.domain.branch.state.CompanyBranches;
import ir.neshane.customer.domain.branch.type.BranchTypeCatalog;
import ir.neshane.customer.domain.customer.lifecycle.CustomerLifecycleSnapshot;
import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.customer.model.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.model.CustomerStatus;
import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.IdentityRevision;
import ir.neshane.customer.domain.verification.model.VerificationStatus;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.contact.ContactTypeDefinition;
import ir.neshane.customer.domain.contact.ContactTypeRef;
import ir.neshane.customer.domain.contact.ContactOwner;
import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.contact.ContactValue;
import ir.neshane.customer.domain.contact.SystemContactVocabulary;
import ir.neshane.customer.domain.shared.localization.LocalizedText;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;
import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.HashMap;

public final class BranchApplicationSpecification {

    private static final TenantId TENANT =
            new TenantId("synthetic-tenant");

    private static final Company COMPANY =
            Company.createCandidate(
                    new CustomerId("company"),
                    TENANT, "Company", null);

    private static final Company OTHER =
            Company.createCandidate(
                    new CustomerId("other"),
                    TENANT, "Other", null);

    private static final BranchTypeCatalog TYPES =
            BranchTypeCatalog.initial();

    private static final BranchTypeCatalog.Ref TYPE =
            new BranchTypeCatalog.Ref("BRANCH", Optional.empty());

    private static final ContactTypeCatalog CONTACTS =
            ContactTypeCatalog.empty().withCandidateDefinition(
                    new ContactTypeDefinition(
                            ContactTypeRef.system("MOBILE"),
                            "MOBILE",
                            new LocalizedText(Map.of("fa", "Mobile")),
                            new LocalizedText(Map.of()),
                            SystemContactVocabulary.MOBILE.valueType(),
                            Set.of(ContactOwner.Kind.BRANCH),
                            true, Optional.of("p"),
                            ContactTypeDefinition.Status.ACTIVE, 0));

    private static final ValidationProfile PROFILE =
            new ValidationProfile(
                    "p", Optional.empty(),
                    List.of(new ValidationProfile.Length(
                            ValidationProfile.Field.VALUE, 3, 10)));

    private static final RegisterBranchCandidateUseCase REGISTER =
            new RegisterBranchCandidate();

    private static final UpdateBranchCandidateUseCase UPDATE =
            new UpdateBranchCandidate();

    private static int checks;

    private BranchApplicationSpecification() {}

    public static void run() {
        checks = 0;

        verifyRegistration();
        verifyUpdate();

        System.out.println(
                "AP-06.6: " + checks
                + " branch application checks PASS");
    }

    private static void verifyRegistration() {

        var parent = parent(COMPANY, CustomerStatus.ACTIVE);
        var existing = CompanyBranches.empty(COMPANY);

        var branch = Branch.createCandidate(
                new BranchId("b1"), TENANT, COMPANY,
                "one", "Original", null, null,
                TYPES, Optional.of(TYPE));

        var context = new BranchRegistration.Context(
                TENANT, BranchRegistration.Authority.REGISTER);

        var valid = registration(
                branch, parent, existing,
                BranchRegistration.Coverage.COMPLETE_CANDIDATE,
                context, TYPES, Map.of());

        var result = REGISTER.execute(valid);

        expectRegistration(result,
                RegisterBranchCandidateResult.Status.ELIGIBLE_CANDIDATE,
                "BRANCH_REGISTRATION_CANDIDATE", true);

        require(result.candidateDetails().orElseThrow()
                .proposedVersion() == 1, "Initial version");

        rejectRegistration(registration(
                branch, parent(COMPANY, CustomerStatus.INACTIVE),
                existing, BranchRegistration.Coverage.COMPLETE_CANDIDATE,
                context, TYPES, Map.of()),
                "BRANCH_PARENT_NOT_ACTIVE", false);

        rejectRegistration(registration(
                branch, parent(COMPANY, CustomerStatus.ARCHIVED),
                existing, BranchRegistration.Coverage.COMPLETE_CANDIDATE,
                context, TYPES, Map.of()),
                "BRANCH_PARENT_NOT_ACTIVE", false);

        rejectRegistration(registration(
                branch, null, existing,
                BranchRegistration.Coverage.COMPLETE_CANDIDATE,
                context, TYPES, Map.of()),
                "BRANCH_PARENT_REQUIRED", false);

        rejectRegistration(registration(
                branch, parent(OTHER, CustomerStatus.ACTIVE),
                existing, BranchRegistration.Coverage.COMPLETE_CANDIDATE,
                context, TYPES, Map.of()),
                "BRANCH_PARENT_SCOPE_MISMATCH", false);

        rejectRegistration(registration(
                branch, parent, existing,
                BranchRegistration.Coverage.COMPLETE_CANDIDATE,
                new BranchRegistration.Context(
                        new TenantId("foreign"),
                        BranchRegistration.Authority.REGISTER),
                TYPES, Map.of()),
                "BRANCH_PARENT_SCOPE_MISMATCH", false);

        rejectRegistration(registration(
                branch, parent, existing,
                BranchRegistration.Coverage.COMPLETE_CANDIDATE,
                new BranchRegistration.Context(
                        TENANT, BranchRegistration.Authority.NONE),
                TYPES, Map.of()),
                "BRANCH_REGISTRATION_AUTHORITY_REQUIRED", true);

        rejectRegistration(registration(
                branch, parent, existing,
                BranchRegistration.Coverage.UNKNOWN,
                context, TYPES, Map.of()),
                "BRANCH_REGISTRATION_COVERAGE_UNRESOLVED", false);

        rejectRegistration(registration(
                branch, parent, existing.appendCandidate(branch),
                BranchRegistration.Coverage.COMPLETE_CANDIDATE,
                context, TYPES, Map.of()),
                "BRANCH_REGISTRATION_ASSESSMENT_BLOCKED", false);

        rejectRegistration(registration(
                branch, parent, existing,
                BranchRegistration.Coverage.COMPLETE_CANDIDATE,
                context, TYPES.withHidden(TENANT, TYPE, true),
                Map.of()),
                "BRANCH_REGISTRATION_ASSESSMENT_BLOCKED", false);

        var point = new ContactPoint(
                "phone", branch.contactInfo().owner(),
                ContactTypeRef.system("MOBILE"),
                new ContactValue.Phone("123", Optional.empty()));

        var contactInfo = CONTACTS.selectPrimaryCandidate(
                CONTACTS.appendCandidate(
                        branch.contactInfo(), point), "phone");

        var withPhone = branch.withCandidateContactInfo(contactInfo);

        rejectRegistration(registration(
                withPhone, parent, existing,
                BranchRegistration.Coverage.COMPLETE_CANDIDATE,
                context, TYPES, Map.of()),
                "BRANCH_CONTACT_VALIDATION_BLOCKED", false);

        expectRegistration(
                REGISTER.execute(registration(
                        withPhone, parent, existing,
                        BranchRegistration.Coverage.COMPLETE_CANDIDATE,
                        context, TYPES, Map.of("phone", PROFILE))),
                RegisterBranchCandidateResult.Status.ELIGIBLE_CANDIDATE,
                "BRANCH_REGISTRATION_CANDIDATE", true);

        require(existing.branches().isEmpty(),
                "Registration source collection unchanged");

        require(result.equals(REGISTER.execute(valid)),
                "Deterministic registration");

        require(valid.toString().equals(
                "RegisterBranchCandidateCommand[redacted]"),
                "Registration command redaction");

        require(result.toString().equals(
                "RegisterBranchCandidateResult[redacted]"),
                "Registration result redaction");

        var mutable = new HashMap<String, ValidationProfile>();
        mutable.put("phone", PROFILE);

        var captured = registration(
                withPhone, parent, existing,
                BranchRegistration.Coverage.COMPLETE_CANDIDATE,
                context, TYPES, mutable);

        mutable.clear();

        require(captured.validation().profiles().containsKey("phone"),
                "Registration defensive copy");
    }

    private static void verifyUpdate() {

        var branch = branch(COMPANY, "b1", "one", "Original");
        var other = branch(COMPANY, "b2", "two", "Other");

        var existing = CompanyBranches.empty(COMPANY)
                .appendCandidate(branch).appendCandidate(other);

        var version = BranchRecordVersion.initial(branch);

        var current = new BranchUpdatePolicy.Current(
                branch, BranchStatus.ACTIVE, version);

        var parent = parent(COMPANY, CustomerStatus.ACTIVE);

        var context = new BranchUpdatePolicy.Context(
                TENANT, BranchUpdatePolicy.Authority.UPDATE,
                BranchStatus.ACTIVE);

        var edited = Branch.createCandidate(
                branch.id(), TENANT, COMPANY,
                "new", "Changed", "English", "Description");

        var valid = update(
                current, version, edited, parent,
                existing, context, TYPES, CONTACTS, Map.of());

        var result = UPDATE.execute(valid);

        expectUpdate(result,
                UpdateBranchCandidateResult.Status.ELIGIBLE_CANDIDATE,
                "BRANCH_UPDATE_CANDIDATE", true);

        var details = result.candidateDetails().orElseThrow();

        require(details.proposedVersion() == 2,
                "Proposed update version");

        require(details.changedFields().equals(Set.of(
                UpdateBranchCandidateResult.ChangedField.NAME,
                UpdateBranchCandidateResult.ChangedField.ENGLISH_NAME,
                UpdateBranchCandidateResult.ChangedField.CODE,
                UpdateBranchCandidateResult.ChangedField.DESCRIPTION)),
                "Changed field mapping");

        expectUpdate(UPDATE.execute(update(
                current, version, branch, parent,
                existing, context, TYPES, CONTACTS, Map.of())),
                UpdateBranchCandidateResult.Status.UNCHANGED,
                "BRANCH_UPDATE_UNCHANGED", false);

        rejectUpdate(update(
                current, null, edited, parent, existing,
                context, TYPES, CONTACTS, Map.of()),
                "BRANCH_RECORD_VERSION_REQUIRED", false);

        rejectUpdate(update(
                current, new BranchRecordVersion(version.owner(), 2),
                edited, parent, existing,
                context, TYPES, CONTACTS, Map.of()),
                "BRANCH_RECORD_VERSION_STALE", false);

        rejectUpdate(update(
                current, version,
                branch(OTHER, "b1", "one", "Changed"),
                parent, existing, context, TYPES, CONTACTS, Map.of()),
                "BRANCH_RECORD_SCOPE_MISMATCH", false);

        rejectUpdate(update(
                current, version, edited,
                parent(COMPANY, CustomerStatus.ARCHIVED),
                existing, context, TYPES, CONTACTS, Map.of()),
                "BRANCH_PARENT_ARCHIVED", false);

        rejectUpdate(update(
                current, version, edited, parent, existing,
                new BranchUpdatePolicy.Context(
                        TENANT, BranchUpdatePolicy.Authority.NONE,
                        BranchStatus.ACTIVE),
                TYPES, CONTACTS, Map.of()),
                "BRANCH_UPDATE_AUTHORITY_REQUIRED", true);

        rejectUpdate(update(
                current, version, edited, parent, existing,
                new BranchUpdatePolicy.Context(
                        TENANT, BranchUpdatePolicy.Authority.UPDATE,
                        BranchStatus.INACTIVE),
                TYPES, CONTACTS, Map.of()),
                "BRANCH_UPDATE_LIFECYCLE_CHANGE_FORBIDDEN", false);

        rejectUpdate(update(
                current, version, edited, parent, null,
                context, TYPES, CONTACTS, Map.of()),
                "BRANCH_UPDATE_COVERAGE_UNRESOLVED", false);

        rejectUpdate(update(
                current, version,
                branch(COMPANY, "b1", "two", "Changed"),
                parent, existing, context, TYPES, CONTACTS, Map.of()),
                "BRANCH_UPDATE_ASSESSMENT_BLOCKED", false);

        var typed = Branch.createCandidate(
                branch.id(), TENANT, COMPANY,
                "one", "Original", null, null,
                TYPES, Optional.of(TYPE));

        rejectUpdate(update(
                current, version, typed, parent, existing,
                context, null, CONTACTS, Map.of()),
                "BRANCH_UPDATE_TYPE_CONTACT_VALIDATION_REQUIRED", false);

        var typedResult = UPDATE.execute(update(
                current, version, typed, parent, existing,
                context, TYPES, CONTACTS, Map.of()));

        expectUpdate(typedResult,
                UpdateBranchCandidateResult.Status.ELIGIBLE_CANDIDATE,
                "BRANCH_UPDATE_CANDIDATE", true);

        require(typedResult.candidateDetails().orElseThrow()
                .changedFields().equals(Set.of(
                        UpdateBranchCandidateResult.ChangedField.TYPE)),
                "Type change mapping");

        var phone = new ContactPoint(
                "phone", branch.contactInfo().owner(),
                ContactTypeRef.system("MOBILE"),
                new ContactValue.Phone("123", Optional.empty()));

        var withPhone = branch.withCandidateContactInfo(
                branch.contactInfo().withCandidatePoint(phone));

        rejectUpdate(update(
                current, version, withPhone, parent, existing,
                context, TYPES, null, null),
                "BRANCH_UPDATE_TYPE_CONTACT_VALIDATION_REQUIRED", false);

        var contactResult = UPDATE.execute(update(
                current, version, withPhone, parent, existing,
                context, TYPES, CONTACTS, Map.of("phone", PROFILE)));

        expectUpdate(contactResult,
                UpdateBranchCandidateResult.Status.ELIGIBLE_CANDIDATE,
                "BRANCH_UPDATE_CANDIDATE", true);

        require(contactResult.candidateDetails().orElseThrow()
                .changedFields().equals(Set.of(
                        UpdateBranchCandidateResult.ChangedField.CONTACT_INFO)),
                "Contact change mapping");

        require(current.version().value() == 1,
                "Original version unchanged");

        require(existing.branches().equals(List.of(branch, other)),
                "Original collection unchanged");

        require(result.equals(UPDATE.execute(valid)),
                "Deterministic update");

        require(valid.toString().equals(
                "UpdateBranchCandidateCommand[redacted]"),
                "Update command redaction");

        require(result.toString().equals(
                "UpdateBranchCandidateResult[redacted]"),
                "Update result redaction");

        try {
            details.changedFields().clear();
            throw new AssertionError("Mutable changed fields");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }

        try {
            UPDATE.execute(null);
            throw new AssertionError("Expected null rejection");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    private static Branch branch(
            Company owner, String id, String code, String name) {

        return Branch.createCandidate(
                new BranchId(id), owner.tenantId(), owner,
                code, name, null, null);
    }

    private static CustomerLifecycleSnapshot parent(
            Company company, CustomerStatus status) {

        return CustomerUpdatePolicy.lifecycleSnapshot(
                new CustomerUpdatePolicy.Current(
                        IdentityRevision.snapshot(
                                company, "r1", VerificationStatus.VERIFIED),
                        status, CustomerRecordVersion.initial(company)));
    }

    private static RegisterBranchCandidateCommand registration(
            Branch branch,
            CustomerLifecycleSnapshot parent,
            CompanyBranches existing,
            BranchRegistration.Coverage coverage,
            BranchRegistration.Context context,
            BranchTypeCatalog types,
            Map<String, ValidationProfile> profiles) {

        return new RegisterBranchCandidateCommand(
                branch, parent, existing, coverage, context,
                new RegisterBranchCandidateCommand.ValidationFacts(
                        types, CONTACTS, profiles));
    }

    private static UpdateBranchCandidateCommand update(
            BranchUpdatePolicy.Current current,
            BranchRecordVersion version,
            Branch proposed,
            CustomerLifecycleSnapshot parent,
            CompanyBranches existing,
            BranchUpdatePolicy.Context context,
            BranchTypeCatalog types,
            ContactTypeCatalog contacts,
            Map<String, ValidationProfile> profiles) {

        return new UpdateBranchCandidateCommand(
                current, version, proposed, parent, existing,
                BranchRegistration.Coverage.COMPLETE_CANDIDATE,
                context,
                new UpdateBranchCandidateCommand.ValidationFacts(
                        types, contacts, profiles));
    }

    private static void rejectRegistration(
            RegisterBranchCandidateCommand command,
            String code, boolean audit) {

        expectRegistration(
                REGISTER.execute(command),
                RegisterBranchCandidateResult.Status.BLOCKED,
                code, audit);
    }

    private static void rejectUpdate(
            UpdateBranchCandidateCommand command,
            String code, boolean audit) {

        expectUpdate(
                UPDATE.execute(command),
                UpdateBranchCandidateResult.Status.BLOCKED,
                code, audit);
    }

    private static void expectRegistration(
            RegisterBranchCandidateResult result,
            RegisterBranchCandidateResult.Status status,
            String code, boolean audit) {

        require(result.status() == status, "Registration status");
        require(result.code().equals(code),
                "Registration code: " + result.code());
        require(result.auditRequired() == audit, "Registration audit");
        require(result.candidateDetails().isPresent()
                == (status == RegisterBranchCandidateResult.Status.ELIGIBLE_CANDIDATE),
                "Registration details");
    }

    private static void expectUpdate(
            UpdateBranchCandidateResult result,
            UpdateBranchCandidateResult.Status status,
            String code, boolean audit) {

        require(result.status() == status, "Update status");
        require(result.code().equals(code),
                "Update code: " + result.code());
        require(result.auditRequired() == audit, "Update audit");
        require(result.candidateDetails().isPresent()
                == (status == UpdateBranchCandidateResult.Status.ELIGIBLE_CANDIDATE),
                "Update details");
    }

    private static void require(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
        checks++;
    }
}
