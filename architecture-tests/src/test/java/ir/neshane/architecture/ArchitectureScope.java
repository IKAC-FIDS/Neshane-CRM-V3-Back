package ir.neshane.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import java.util.Collection;

final class ArchitectureScope {

    private ArchitectureScope() {}

    static void requirePackages(JavaClasses classes, Collection<String> requiredPackages) {
        if (classes.isEmpty()) {
            throw new IllegalStateException("protected architecture scope is empty");
        }
        for (String requiredPackage : requiredPackages) {
            boolean present = classes.stream()
                    .anyMatch(javaClass -> javaClass.getPackageName().startsWith(requiredPackage));
            if (!present) {
                throw new IllegalStateException("protected package is missing: " + requiredPackage);
            }
        }
    }
}
