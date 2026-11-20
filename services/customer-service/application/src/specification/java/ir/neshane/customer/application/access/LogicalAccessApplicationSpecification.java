package ir.neshane.customer.application.access;

import ir.neshane.customer.application.access.logical.query.LogicalAccessQuery;
import ir.neshane.customer.application.access.logical.usecase.AssessLogicalAccess;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.access.shared.AccessAssessmentMapper;
import ir.neshane.customer.application.port.in.LogicalAccessUseCase;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class LogicalAccessApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("protected-tenant");

    private static final TenantId FOREIGN =
            new TenantId("protected-foreign");

    private static final LogicalAccessUseCase USE_CASE =
            new AssessLogicalAccess();

    private LogicalAccessApplicationSpecification() {}

    public static void run() {

        checks = 0;

        operationMatrix();
        tenantIsolation();
        actorValidation();
        grantImmutability();
        safeMapping();
        resultInvariants();
        redaction();

        System.out.println(
                "AP-12.7.1: " + checks
                + " logical access application checks PASS");
    }

    private static LogicalAccessPolicy.ActorContext actor(
            TenantId tenant,
            Set<LogicalAccessPolicy.Grant> grants) {

        return new LogicalAccessPolicy.ActorContext(
                "protected-actor",
                tenant,
                grants);
    }

    private static LogicalAccessPolicy.ActorContext actor(
            TenantId tenant,
            LogicalAccessPolicy.Operation operation) {

        return actor(
                tenant,
                Set.of(new LogicalAccessPolicy.Grant(
                        tenant, operation)));
    }

    private static AccessAssessmentResult assess(
            LogicalAccessPolicy.ActorContext actor,
            TenantId tenant,
            LogicalAccessPolicy.Operation operation) {

        return USE_CASE.execute(
                new LogicalAccessQuery(
                        actor,
                        new LogicalAccessPolicy.Request(
                                tenant, operation)));
    }

    private static void operationMatrix() {

        var operations = LogicalAccessPolicy.Operation.values();

        expect(operations.length == 26);

        for (var granted : operations) {

            var actor = actor(TENANT, granted);

            for (var requested : operations) {

                var result = assess(
                        actor, TENANT, requested);

                if (granted == requested) {

                    expectEligible(result);

                } else {

                    expectDenied(
                            result,
                            "ACCESS_OPERATION_NOT_GRANTED");
                }
            }
        }

        var allGrants = Arrays.stream(operations)
                .map(operation ->
                        new LogicalAccessPolicy.Grant(
                                TENANT, operation))
                .collect(Collectors.toUnmodifiableSet());

        var fullyGranted = actor(TENANT, allGrants);

        for (var operation : operations) {

            expectEligible(
                    assess(fullyGranted, TENANT, operation));
        }

        var noGrants = actor(TENANT, Set.of());

        for (var operation : operations) {

            expectDenied(
                    assess(noGrants, TENANT, operation),
                    "ACCESS_OPERATION_NOT_GRANTED");
        }
    }

    private static void tenantIsolation() {

        for (var operation : LogicalAccessPolicy.Operation.values()) {

            var localActor = actor(TENANT, operation);

            expectDenied(
                    assess(localActor, FOREIGN, operation),
                    "ACCESS_TENANT_MISMATCH");

            var foreignActor = actor(FOREIGN, operation);

            expectDenied(
                    assess(foreignActor, TENANT, operation),
                    "ACCESS_TENANT_MISMATCH");

            var foreignGrant = actor(
                    TENANT,
                    Set.of(new LogicalAccessPolicy.Grant(
                            FOREIGN, operation)));

            expectDenied(
                    assess(foreignGrant, TENANT, operation),
                    "ACCESS_OPERATION_NOT_GRANTED");

            expectDenied(
                    assess(foreignGrant, FOREIGN, operation),
                    "ACCESS_TENANT_MISMATCH");

            var mixedGrants = actor(
                    TENANT,
                    Set.of(
                            new LogicalAccessPolicy.Grant(
                                    TENANT, operation),
                            new LogicalAccessPolicy.Grant(
                                    FOREIGN, operation)));

            expectEligible(
                    assess(mixedGrants, TENANT, operation));

            expectDenied(
                    assess(mixedGrants, FOREIGN, operation),
                    "ACCESS_TENANT_MISMATCH");
        }
    }

    private static void actorValidation() {

        for (var operation : LogicalAccessPolicy.Operation.values()) {

            expectDenied(
                    assess(null, TENANT, operation),
                    "ACCESS_ACTOR_CONTEXT_REQUIRED");
        }

        expectNull(() -> USE_CASE.execute(null));

        expectNull(() -> new LogicalAccessQuery(null, null));

        expectNull(() -> new LogicalAccessPolicy.Request(
                null,
                LogicalAccessPolicy.Operation.CUSTOMER_READ));

        expectNull(() -> new LogicalAccessPolicy.Request(
                TENANT, null));
    }

    private static void grantImmutability() {

        var read = LogicalAccessPolicy.Operation.CUSTOMER_READ;
        var update = LogicalAccessPolicy.Operation.CUSTOMER_UPDATE;

        var mutableGrants =
                new HashSet<LogicalAccessPolicy.Grant>();

        mutableGrants.add(
                new LogicalAccessPolicy.Grant(TENANT, read));

        var actor = actor(TENANT, mutableGrants);

        mutableGrants.clear();

        mutableGrants.add(
                new LogicalAccessPolicy.Grant(TENANT, update));

        expect(actor.grants().size() == 1);

        expectEligible(
                assess(actor, TENANT, read));

        expectDenied(
                assess(actor, TENANT, update),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectImmutable(
                () -> actor.grants().clear());

        expect(actor.grants().contains(
                new LogicalAccessPolicy.Grant(TENANT, read)));

        var query = new LogicalAccessQuery(
                actor,
                new LogicalAccessPolicy.Request(
                        TENANT, read));

        var first = USE_CASE.execute(query);
        var second = USE_CASE.execute(query);

        expect(first.equals(second));
    }

    private static void safeMapping() {

        expectEligible(
                AccessAssessmentMapper.map(
                        new LogicalAccessPolicy.Assessment(
                                LogicalAccessPolicy.Outcome.ELIGIBLE_CANDIDATE,
                                "ACCESS_OPERATION_ELIGIBLE_CANDIDATE")));

        expectDenied(
                AccessAssessmentMapper.map(
                        new LogicalAccessPolicy.Assessment(
                                LogicalAccessPolicy.Outcome.DENIED,
                                "ACCESS_TENANT_MISMATCH")),
                "ACCESS_TENANT_MISMATCH");

        expectDenied(
                AccessAssessmentMapper.map(
                        new LogicalAccessPolicy.Assessment(
                                LogicalAccessPolicy.Outcome.DENIED,
                                "protected-sensitive-value")),
                "ACCESS_ASSESSMENT_DENIED");

        expectDenied(
                AccessAssessmentMapper.map(
                        new LogicalAccessPolicy.Assessment(
                                LogicalAccessPolicy.Outcome.DENIED,
                                null)),
                "ACCESS_ASSESSMENT_DENIED");

        expectDenied(
                AccessAssessmentMapper.map(
                        new LogicalAccessPolicy.Assessment(
                                LogicalAccessPolicy.Outcome.ELIGIBLE_CANDIDATE,
                                "UNKNOWN_CANDIDATE")),
                "ACCESS_ASSESSMENT_DENIED");

        expectDenied(
                AccessAssessmentMapper.map(
                        new LogicalAccessPolicy.Assessment(
                                LogicalAccessPolicy.Outcome.DENIED,
                                "ACCESS_OPERATION_ELIGIBLE_CANDIDATE")),
                "ACCESS_ASSESSMENT_DENIED");

        expectDenied(
                AccessAssessmentMapper.map(
                        new LogicalAccessPolicy.Assessment(
                                null,
                                "ACCESS_OPERATION_ELIGIBLE_CANDIDATE")),
                "ACCESS_ASSESSMENT_DENIED");

        expectNull(() -> AccessAssessmentMapper.map(null));
    }

    private static void resultInvariants() {

        expectIllegal(() -> new AccessAssessmentResult(
                AccessAssessmentResult.Status.ELIGIBLE_CANDIDATE,
                "ACCESS_OPERATION_NOT_GRANTED"));

        expectIllegal(() -> new AccessAssessmentResult(
                AccessAssessmentResult.Status.DENIED,
                "ACCESS_OPERATION_ELIGIBLE_CANDIDATE"));

        expectIllegal(() -> new AccessAssessmentResult(
                AccessAssessmentResult.Status.DENIED,
                ""));

        expectIllegal(() -> new AccessAssessmentResult(
                AccessAssessmentResult.Status.DENIED,
                "   "));

        expectNull(() -> new AccessAssessmentResult(
                null,
                "ACCESS_ASSESSMENT_DENIED"));

        expectNull(() -> new AccessAssessmentResult(
                AccessAssessmentResult.Status.DENIED,
                null));
    }

    private static void redaction() {

        var operation =
                LogicalAccessPolicy.Operation.IDENTITY_SENSITIVE_READ;

        var actor = actor(TENANT, operation);

        var query = new LogicalAccessQuery(
                actor,
                new LogicalAccessPolicy.Request(
                        TENANT, operation));

        var eligible = USE_CASE.execute(query);

        var denied = assess(actor, FOREIGN, operation);

        expect(query.toString().equals(
                "LogicalAccessQuery[redacted]"));

        expect(actor.toString().equals(
                "LogicalAccessPolicy.ActorContext[redacted]"));

        expect(eligible.toString().equals(
                "AccessAssessmentResult[redacted]"));

        expect(denied.toString().equals(
                "AccessAssessmentResult[redacted]"));

        for (var value : new Object[] {
                actor, query, eligible, denied }) {

            expect(!value.toString().contains("protected-actor"));
            expect(!value.toString().contains("protected-tenant"));
            expect(!value.toString().contains("protected-foreign"));
            expect(!value.toString().contains("IDENTITY_SENSITIVE_READ"));
        }
    }

    private static void expectEligible(
            AccessAssessmentResult result) {

        expect(result.status()
                == AccessAssessmentResult.Status.ELIGIBLE_CANDIDATE);

        expect(result.code().equals(
                "ACCESS_OPERATION_ELIGIBLE_CANDIDATE"));
    }

    private static void expectDenied(
            AccessAssessmentResult result,
            String code) {

        expect(result.status()
                == AccessAssessmentResult.Status.DENIED);

        expect(result.code().equals(code));
    }

    private static void expectNull(Runnable action) {

        try {
            action.run();
        } catch (NullPointerException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected null rejection");
    }

    private static void expectIllegal(Runnable action) {

        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected invariant rejection");
    }

    private static void expectImmutable(Runnable action) {

        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected immutable collection");
    }

    private static void expect(boolean value) {

        if (!value) {

            throw new AssertionError(
                    "AP-12.7.1 assertion " + (checks + 1));
        }

        checks++;
    }
}
