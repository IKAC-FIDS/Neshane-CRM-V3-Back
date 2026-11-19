package ir.neshane.customer.domain;

import ir.neshane.customer.domain.identity.IdentifierTypeDefinition;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;
import ir.neshane.customer.domain.identity.IdentityInfo;

import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.identity.IdentityIdentifier;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.IdentityConflictDetection.Verification.*;
import static ir.neshane.customer.domain.IdentityConflictDetection.Outcome.*;

final class IdentityConflictSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("a");
        var owner = new IdentityOwner(tenant, new CustomerId("a"), IdentityOwner.Kind.PERSON);
        var other = new IdentityOwner(tenant, new CustomerId("b"), IdentityOwner.Kind.PERSON);
        var foreign = new IdentityOwner(new TenantId("b"), other.customerId(), IdentityOwner.Kind.PERSON);
        var type = new IdentifierTypeDefinition("passport", "PASSPORT", new LocalizedText(Map.of("en", "Passport")),
            Set.of(IdentityOwner.Kind.PERSON), Set.of(), Optional.empty(), true, Optional.empty(), IdentifierTypeDefinition.Status.ACTIVE);
        var catalog = IdentifierTypeCatalog.empty().withSystemCandidate(type);
        var day = LocalDate.of(2030, 1, 1);
        var first = identifier(owner, "IR", "issuer-a", Optional.empty(), IdentityIdentifier.Status.ACTIVE);
        var second = identifier(other, "IR", "issuer-a", Optional.of(day.plusDays(1)), IdentityIdentifier.Status.ACTIVE);
        var a = evidence(first, "passport:IR:issuer-a", UNVERIFIED);
        var b = evidence(second, "passport:IR:issuer-a", VERIFIED_VALID);
        expect(compare(a,b,catalog,day).outcome() == DEFINITE_CONFLICT);
        expect(compare(a,b,catalog,day).signal().orElseThrow() == DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER);
        expect(compare(a,evidence(second,"passport:IR:issuer-a",UNVERIFIED),catalog,day).outcome() == POTENTIAL_DUPLICATE);
        expect(compare(a,evidence(second,"passport:IR:issuer-a",UNVERIFIED),catalog,day).signal().orElseThrow() == DuplicateDetection.Signal.UNVERIFIED_IDENTIFIER);
        expect(compare(a,b,catalog,day.plusDays(1)).outcome() == BLOCKED);
        expect(compare(a,b,catalog,day.plusDays(2)).outcome() == POTENTIAL_DUPLICATE);
        expect(compare(a,evidence(second,"passport:IR:issuer-b",VERIFIED_VALID),catalog,day).outcome() == NO_MATCH);
        var differentValue = new IdentityConflictDetection.Evidence(second, b.namespace(), Optional.of("different"), VERIFIED_VALID);
        expect(compare(a,differentValue,catalog,day).outcome() == NO_MATCH);
        expect(compare(a,new IdentityConflictDetection.Evidence(second, Optional.empty(), b.comparisonValue(), VERIFIED_VALID),catalog,day).outcome() == BLOCKED);
        expect(compare(a,new IdentityConflictDetection.Evidence(second, b.namespace(), Optional.empty(), VERIFIED_VALID),catalog,day).outcome() == BLOCKED);
        expect(compare(a,evidence(second,"passport:IR:issuer-a",UNKNOWN),catalog,day).outcome() == BLOCKED);
        expect(compare(a,b,IdentifierTypeCatalog.empty(),day).code().equals("IDENTIFIER_TYPE_UNKNOWN"));
        expect(compare(a,b,catalog.withTenantHidden(tenant,"passport",true),day).outcome() == DEFINITE_CONFLICT);
        var inactive = identifier(other,"IR","issuer-a",Optional.empty(),IdentityIdentifier.Status.INACTIVE);
        expect(compare(a,evidence(inactive,"passport:IR:issuer-a",VERIFIED_VALID),catalog,day).outcome() == POTENTIAL_DUPLICATE);
        var foreignId = identifier(foreign,"IR","issuer-a",Optional.empty(),IdentityIdentifier.Status.ACTIVE);
        expect(compare(a,evidence(foreignId,"passport:IR:issuer-a",VERIFIED_VALID),catalog,day).code().equals("IDENTITY_CONFLICT_TENANT_MISMATCH"));
        expect(compare(a,a,catalog,day).code().equals("IDENTITY_CONFLICT_SELF_COMPARISON"));
        expect(IdentityConflictDetection.compare(new IdentityInfo(owner,List.of(first)),a,IdentityInfo.empty(other),b,catalog,day).code().equals("IDENTITY_CONFLICT_EVIDENCE_MISMATCH"));
        var companyOwner = new IdentityOwner(tenant,new CustomerId("company"),IdentityOwner.Kind.COMPANY);
        var companyId = identifier(companyOwner,"IR","issuer-a",Optional.empty(),IdentityIdentifier.Status.ACTIVE);
        expect(compare(a,evidence(companyId,"passport:IR:issuer-a",VERIFIED_VALID),catalog,day).code().equals("IDENTITY_CONFLICT_TYPE_NOT_APPLICABLE"));
        expect(!b.toString().contains("private-value"));
        expect(!compare(a,b,catalog,day).toString().contains("private-value"));
        expect(compare(a,differentValue,catalog,day).signal().isEmpty());
        System.out.println("PASS: " + checks + " isolated identity conflict assertions");
    }
    private static IdentityIdentifier identifier(IdentityOwner owner,String country,String issuer,Optional<LocalDate> expiry,IdentityIdentifier.Status status) {
        return new IdentityIdentifier("id",owner,"passport","private-value",Optional.of(country),Optional.of(issuer),Optional.empty(),expiry,status);
    }
    private static IdentityConflictDetection.Evidence evidence(IdentityIdentifier id,String namespace,IdentityConflictDetection.Verification verification) {
        return new IdentityConflictDetection.Evidence(id,Optional.of(namespace),Optional.of(id.value()),verification);
    }
    private static IdentityConflictDetection.Assessment compare(IdentityConflictDetection.Evidence a,IdentityConflictDetection.Evidence b,IdentifierTypeCatalog catalog,LocalDate day) {
        return IdentityConflictDetection.compare(new IdentityInfo(a.identifier().owner(),List.of(a.identifier())),a,
            new IdentityInfo(b.identifier().owner(),List.of(b.identifier())),b,catalog,day);
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Identity conflict assertion " + (checks + 1));
        checks++;
    }
}
