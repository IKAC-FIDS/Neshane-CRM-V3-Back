package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** System-maintained synthetic configuration; no tenant bypass or trusted registry. */
public final class ArchiveDependencyCatalog {
    public enum CustomerKind { PERSON, COMPANY }
    public enum Status { ACTIVE, INACTIVE }
    public record Definition(String dependencyId, String code, LocalizedText title,
            LocalizedText description, String ownerService, Set<CustomerKind> applicableCustomerTypes,
            Status status, int displayOrder) {
        public Definition {
            IdentityIdentifier.required(dependencyId, "DEPENDENCY_ID_REQUIRED");
            IdentityIdentifier.required(code, "DEPENDENCY_CODE_REQUIRED");
            code = code.strip().toUpperCase(Locale.ROOT);
            if (!code.matches("[A-Z0-9_]{3,64}")) throw new IllegalArgumentException("DEPENDENCY_CODE_INVALID");
            Objects.requireNonNull(title, "DEPENDENCY_TITLE_REQUIRED");
            if (title.translations().isEmpty()) throw new IllegalArgumentException("DEPENDENCY_TITLE_REQUIRED");
            Objects.requireNonNull(description, "DEPENDENCY_DESCRIPTION_REQUIRED");
            IdentityIdentifier.required(ownerService, "DEPENDENCY_OWNER_REQUIRED");
            applicableCustomerTypes = Set.copyOf(applicableCustomerTypes);
            if (applicableCustomerTypes.isEmpty()) throw new IllegalArgumentException("DEPENDENCY_SCOPE_REQUIRED");
            Objects.requireNonNull(status, "DEPENDENCY_STATUS_REQUIRED");
        }
    }

    private final Map<String, Definition> definitions;
    private ArchiveDependencyCatalog(Map<String, Definition> definitions) {
        this.definitions = Map.copyOf(definitions);
    }
    public static ArchiveDependencyCatalog initial() {
        Map<String, Definition> defs = new HashMap<>();
        add(defs, "OPEN_OPPORTUNITIES", "فرصت‌های فروش باز", "Open opportunities", "Sales", 0);
        add(defs, "ACTIVE_CONTRACTS", "قراردادهای فعال", "Active contracts", "Contract", 1);
        add(defs, "OPEN_ORDERS", "سفارش‌های باز", "Open orders", "Order", 2);
        add(defs, "OPEN_TICKETS", "تیکت‌های باز", "Open tickets", "Support", 3);
        add(defs, "ACTIVE_PROJECTS", "پروژه‌های فعال", "Active projects", "Project", 4);
        add(defs, "ONGOING_TENDERS", "مناقصات جاری", "Ongoing tenders", "Tender", 5);
        add(defs, "DELIVERY_COMMITMENTS", "تعهدات تحویل", "Delivery commitments", "Delivery", 6);
        add(defs, "OPEN_ACTIVITIES", "فعالیت‌های باز", "Open activities", "Activity", 7);
        add(defs, "ACTIVE_BRANCHES", "شعب فعال", "Active branches", "Customer", 8);
        add(defs, "ACTIVE_RELATIONSHIPS", "روابط فعال", "Active relationships", "Customer", 9);
        return new ArchiveDependencyCatalog(defs);
    }
    private static void add(Map<String, Definition> defs, String code, String fa, String en, String owner, int order) {
        Set<CustomerKind> scope = code.equals("ACTIVE_BRANCHES") ? Set.of(CustomerKind.COMPANY)
            : Set.of(CustomerKind.PERSON, CustomerKind.COMPANY);
        defs.put(code, new Definition(code, code, new LocalizedText(Map.of("fa", fa, "en", en)),
            new LocalizedText(Map.of()), owner, scope, Status.ACTIVE, order));
    }
    public Map<String, Definition> definitions() { return definitions; }
    public ArchiveDependencyCatalog addSystemCandidate(Definition definition) {
        Objects.requireNonNull(definition, "DEPENDENCY_REQUIRED");
        if (definitions.containsKey(definition.code()) || definitions.values().stream()
                .anyMatch(existing -> existing.dependencyId().equals(definition.dependencyId())))
            throw new IllegalArgumentException("DEPENDENCY_ID_OR_CODE_DUPLICATE");
        Map<String, Definition> next = new HashMap<>(definitions);
        next.put(definition.code(), definition);
        return new ArchiveDependencyCatalog(next);
    }
}
