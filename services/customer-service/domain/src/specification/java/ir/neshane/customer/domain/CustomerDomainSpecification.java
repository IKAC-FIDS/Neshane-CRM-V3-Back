package ir.neshane.customer.domain;

import ir.neshane.customer.domain.access.AccessAuditIntegrationSpecification;
import ir.neshane.customer.domain.access.AuditRequirementsPolicySpecification;
import ir.neshane.customer.domain.access.CustomerBranchAccessPolicySpecification;
import ir.neshane.customer.domain.access.LogicalAccessPolicySpecification;
import ir.neshane.customer.domain.access.SensitiveAccessPolicySpecification;

import ir.neshane.customer.domain.document.CustomerDocumentSpecification;
import ir.neshane.customer.domain.document.DocumentCompletenessSpecification;
import ir.neshane.customer.domain.document.DocumentHistorySpecification;
import ir.neshane.customer.domain.document.DocumentIntegrationSpecification;
import ir.neshane.customer.domain.document.DocumentRequirementPolicySpecification;
import ir.neshane.customer.domain.document.DocumentTypeCatalogSpecification;
import ir.neshane.customer.domain.document.DocumentValiditySpecification;
import ir.neshane.customer.domain.document.DocumentVerificationSpecification;
import ir.neshane.customer.domain.document.DocumentVerificationTemporalSpecification;
import ir.neshane.customer.domain.document.DocumentVersionHistorySpecification;

import ir.neshane.customer.domain.verification.IdentityRevisionSpecification;
import ir.neshane.customer.domain.verification.ManualVerificationSpecification;
import ir.neshane.customer.domain.verification.VerificationEvidenceSpecification;
import ir.neshane.customer.domain.verification.VerificationHistorySpecification;
import ir.neshane.customer.domain.verification.VerificationIntegrationSpecification;
import ir.neshane.customer.domain.verification.VerificationLifecycleSpecification;
import ir.neshane.customer.domain.verification.VerificationRevocationSpecification;

import ir.neshane.customer.domain.duplicate.BasicSimilaritySpecification;
import ir.neshane.customer.domain.duplicate.DuplicateDecisionHistorySpecification;
import ir.neshane.customer.domain.duplicate.DuplicateDetectionSpecification;
import ir.neshane.customer.domain.duplicate.DuplicateExceptionSpecification;
import ir.neshane.customer.domain.duplicate.DuplicateIntegrationSpecification;
import ir.neshane.customer.domain.duplicate.IdentityConflictSpecification;

import ir.neshane.customer.domain.relationship.BusinessRoleCatalogSpecification;
import ir.neshane.customer.domain.relationship.RelationshipAssignmentSpecification;
import ir.neshane.customer.domain.relationship.RelationshipHistorySpecification;
import ir.neshane.customer.domain.relationship.RelationshipIntegrationSpecification;
import ir.neshane.customer.domain.relationship.RelationshipLifecycleIntegrationSpecification;
import ir.neshane.customer.domain.relationship.RelationshipLifecycleSpecification;
import ir.neshane.customer.domain.relationship.RelationshipSpecification;
import ir.neshane.customer.domain.relationship.RelationshipTemporalPolicySpecification;
import ir.neshane.customer.domain.relationship.RoleGovernanceSpecification;

import ir.neshane.customer.domain.branch.event.BranchChangeEventDraftSpecification;

import ir.neshane.customer.domain.branch.state.BranchChangeHistorySpecification;
import ir.neshane.customer.domain.branch.state.BranchRegistrationSpecification;
import ir.neshane.customer.domain.branch.state.BranchUpdatePolicySpecification;

import ir.neshane.customer.domain.branch.lifecycle.BranchHistorySpecification;
import ir.neshane.customer.domain.branch.lifecycle.BranchIntegrationSpecification;
import ir.neshane.customer.domain.branch.lifecycle.BranchLifecycleIntegrationSpecification;
import ir.neshane.customer.domain.branch.lifecycle.BranchLifecycleSpecification;

import ir.neshane.customer.domain.branch.type.BranchTypeSpecification;

import ir.neshane.customer.domain.branch.model.BranchContactSpecification;
import ir.neshane.customer.domain.branch.model.BranchRecordVersionSpecification;
import ir.neshane.customer.domain.branch.model.BranchSpecification;

import ir.neshane.customer.domain.customer.state.BranchRecordIntegrationSpecification;
import ir.neshane.customer.domain.customer.state.CriticalFieldChangeSpecification;
import ir.neshane.customer.domain.customer.state.FinalDomainIntegrationSpecification;

import ir.neshane.customer.domain.customer.history.LifecycleHistorySpecification;

import ir.neshane.customer.domain.coordination.registration.CustomerRegistrationSpecification;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;
import ir.neshane.customer.domain.customer.CustomerLifecycleSpecification;
import ir.neshane.customer.domain.customer.CustomerRecordVersionSpecification;

import ir.neshane.customer.domain.contact.BranchTypeContactUpdateSpecification;
import ir.neshane.customer.domain.contact.ContactValidationSpecification;
import ir.neshane.customer.domain.contact.ContactTypeCatalogSpecification;
import ir.neshane.customer.domain.contact.ContactInfoSpecification;
import ir.neshane.customer.domain.identity.IdentityInfoSpecification;

import ir.neshane.customer.domain.shared.localization.LocalizationSpecification;
import ir.neshane.customer.domain.shared.temporal.TemporalPolicySpecification;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

/** Executable unit specification with synthetic data only; no external dependencies. */
public final class CustomerDomainSpecification {
    private static final CustomerId ID = new CustomerId("synthetic-customer");
    private static final TenantId TENANT = new TenantId("synthetic-tenant");
    private static int checks;

    private CustomerDomainSpecification() {}

    public static void main(String[] args) {
        Person person = Person.createCandidate(ID, TENANT, "  نمونه  ", "آزمایشی", null);
        Company company = Company.createCandidate(ID, TENANT, "  شرکت نمونه  ", "Example");
        check(person.givenName().equals("نمونه"), "trim given name");
        check(person.familyName().equals("آزمایشی"), "family name");
        check(company.companyName().equals("شرکت نمونه"), "trim company name");
        check(person.englishName().isEmpty(), "optional English name");
        check(company.englishName().orElseThrow().equals("Example"), "English name");
        for (Customer customer : new Customer[] {person, company}) {
            check(customer.id().equals(ID), "stable supplied ID");
            check(customer.tenantId().equals(TENANT), "logical tenant label");
            check(customer.status() == CustomerStatus.ACTIVE, "initial lifecycle");
            check(customer.verificationStatus() == VerificationStatus.NOT_VERIFIED, "initial verification");
        }
        check(Person.createCandidate(ID, new TenantId("synthetic-tenant-two"), "Test", "Name", null)
            .tenantId().value().equals("synthetic-tenant-two"), "separate logical tenant labels");
        check(Company.createCandidate(ID, TENANT, "A".repeat(255), null).companyName().length() == 255,
            "company boundary");
        check(Person.createCandidate(ID, TENANT, "A".repeat(100), "B".repeat(100), null)
            .familyName().length() == 100, "person boundary");
        reject("GIVEN_NAME_REQUIRED", () -> Person.createCandidate(ID, TENANT, null, "Name", null));
        reject("FAMILY_NAME_REQUIRED", () -> Person.createCandidate(ID, TENANT, "Test", "  ", null));
        reject("COMPANY_NAME_REQUIRED", () -> Company.createCandidate(ID, TENANT, "\u00a0", null));
        reject("COMPANY_NAME_TOO_LONG", () -> Company.createCandidate(ID, TENANT, "A".repeat(256), null));
        reject("GIVEN_NAME_TOO_LONG", () ->
            Person.createCandidate(ID, TENANT, "A".repeat(101), "Name", null));
        reject("FAMILY_NAME_TOO_LONG", () ->
            Person.createCandidate(ID, TENANT, "Test", "A".repeat(101), null));
        reject("ENGLISH_NAME_TOO_LONG", () ->
            Company.createCandidate(ID, TENANT, "Example", "A".repeat(256)));
        reject("ENGLISH_NAME_REQUIRED", () -> Company.createCandidate(ID, TENANT, "Example", " "));
        reject("GIVEN_NAME_LENGTH_POLICY_UNRESOLVED", () ->
            Person.createCandidate(ID, TENANT, "😀".repeat(100), "Name", null));
        reject("CUSTOMER_ID_REQUIRED", () -> new CustomerId(""));
        reject("TENANT_ID_REQUIRED", () -> new TenantId(null));
        reject("CUSTOMER_ID_REQUIRED", () -> Company.createCandidate(null, TENANT, "Test", null));
        reject("TENANT_ID_REQUIRED", () -> Person.createCandidate(ID, null, "Test", "Name", null));
        System.out.println("PASS: " + checks + " isolated domain assertions");
        ContactInfoSpecification.run();
        ContactTypeCatalogSpecification.run();
        LocalizationSpecification.run();
        IdentityInfoSpecification.run();
        ContactValidationSpecification.run();
        CustomerLifecycleSpecification.run();
        LifecycleReasonSpecification.run();
        LifecycleHistorySpecification.run();
        ArchiveDependencySpecification.run();
        LifecycleIntegrationSpecification.run();
        BranchSpecification.run();
        BranchTypeSpecification.run();
        BranchContactSpecification.run();
        BranchLifecycleSpecification.run();
        BranchHistorySpecification.run();
        BranchIntegrationSpecification.run();
        RelationshipSpecification.run();
        BusinessRoleCatalogSpecification.run();
        RoleGovernanceSpecification.run();
        RelationshipAssignmentSpecification.run();
        RelationshipLifecycleSpecification.run();
        RelationshipHistorySpecification.run();
        RelationshipIntegrationSpecification.run();
        DuplicateDetectionSpecification.run();
        IdentityConflictSpecification.run();
        BasicSimilaritySpecification.run();
        DuplicateExceptionSpecification.run();
        DuplicateDecisionHistorySpecification.run();
        DuplicateIntegrationSpecification.run();
        VerificationLifecycleSpecification.run();
        VerificationEvidenceSpecification.run();
        ManualVerificationSpecification.run();
        IdentityRevisionSpecification.run();
        VerificationRevocationSpecification.run();
        VerificationHistorySpecification.run();
        VerificationIntegrationSpecification.run();
        CustomerDocumentSpecification.run();
        DocumentTypeCatalogSpecification.run();
        DocumentRequirementPolicySpecification.run();
        DocumentVersionHistorySpecification.run();
        DocumentValiditySpecification.run();
        DocumentCompletenessSpecification.run();
        DocumentVerificationSpecification.run();
        DocumentHistorySpecification.run();
        DocumentIntegrationSpecification.run();
        CustomerRecordVersionSpecification.run();
        CustomerRegistrationSpecification.run();
        CustomerUpdatePolicySpecification.run();
        CriticalFieldChangeSpecification.run();
        CustomerUpdateVerificationSpecification.run();
        CustomerChangeHistorySpecification.run();
        CustomerChangeEventDraftSpecification.run();
        CustomerRecordIntegrationSpecification.run();
        BranchRecordVersionSpecification.run();
        BranchRegistrationSpecification.run();
        BranchUpdatePolicySpecification.run();
        BranchTypeContactUpdateSpecification.run();
        BranchChangeHistorySpecification.run();
        BranchChangeEventDraftSpecification.run();
        BranchRecordIntegrationSpecification.run();
        CustomerLifecycleIntegrationSpecification.run();
        BranchLifecycleIntegrationSpecification.run();
        RelationshipLifecycleIntegrationSpecification.run();
        CrossAggregateLifecycleSpecification.run();
        CrossAggregateLifecycleIntegrationSpecification.run();
        TemporalPolicySpecification.run();
        RelationshipTemporalPolicySpecification.run();
        DocumentVerificationTemporalSpecification.run();
        TemporalIntegrationSpecification.run();
        LogicalAccessPolicySpecification.run();
        CustomerBranchAccessPolicySpecification.run();
        SensitiveAccessPolicySpecification.run();
        AuditRequirementsPolicySpecification.run();
        AccessAuditIntegrationSpecification.run();
        DomainInvariantsReviewSpecification.run();
        FinalDomainIntegrationSpecification.run();
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }

    private static void reject(String expected, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | NullPointerException ex) {
            check(expected.equals(ex.getMessage()), "expected " + expected);
            return;
        }
        throw new AssertionError("Expected rejection: " + expected);
    }
}
