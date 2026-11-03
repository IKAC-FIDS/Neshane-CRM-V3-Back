package ir.neshane.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;

final class ArchitecturePolicy {

    private static final String NESHANE_PREFIX = "ir.neshane.";
    private static final Set<String> NON_SERVICE_ROOTS = Set.of("architecture", "contract", "platform");

    private ArchitecturePolicy() {}

    static ArchRule domainIsolation() {
        return noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..application..",
                        "..adapter..",
                        "..configuration..",
                        "..infrastructure..",
                        "org.springframework..",
                        "org.hibernate..",
                        "jakarta.persistence..")
                .allowEmptyShould(true)
                .as("Domain must remain independent from application, adapters, infrastructure and frameworks");
    }

    static ArchRule applicationDependencyDirection() {
        return noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..adapter..",
                        "..configuration..",
                        "..infrastructure..",
                        "org.springframework..",
                        "org.hibernate..",
                        "jakarta.persistence..")
                .allowEmptyShould(true)
                .as("Application may depend on Domain ports but not adapters, configuration or frameworks");
    }

    static ArchRule serviceAndPlatformIsolation() {
        return classes()
                .that()
                .resideInAPackage("ir.neshane..")
                .should(new ArchCondition<>("not cross service implementation or platform ownership boundaries") {
                    @Override
                    public void check(JavaClass origin, ConditionEvents events) {
                        for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                            String originRoot = neshaneRoot(origin.getPackageName());
                            String targetRoot = neshaneRoot(dependency.getTargetClass().getPackageName());
                            if (originRoot == null || targetRoot == null || originRoot.equals(targetRoot)) {
                                continue;
                            }

                            boolean crossService = isService(originRoot) && isService(targetRoot);
                            boolean platformToService = originRoot.equals("platform") && isService(targetRoot);
                            if (crossService || platformToService) {
                                events.add(SimpleConditionEvent.violated(
                                        dependency,
                                        "%s depends on prohibited %s".formatted(origin.getName(), dependency)));
                            }
                        }
                    }
                })
                .allowEmptyShould(false)
                .as("Services communicate through approved contracts; platform libraries never import services");
    }

    private static String neshaneRoot(String packageName) {
        if (!packageName.startsWith(NESHANE_PREFIX)) {
            return null;
        }
        String remainder = packageName.substring(NESHANE_PREFIX.length());
        int separator = remainder.indexOf('.');
        return separator < 0 ? remainder : remainder.substring(0, separator);
    }

    private static boolean isService(String root) {
        return !NON_SERVICE_ROOTS.contains(root);
    }
}
