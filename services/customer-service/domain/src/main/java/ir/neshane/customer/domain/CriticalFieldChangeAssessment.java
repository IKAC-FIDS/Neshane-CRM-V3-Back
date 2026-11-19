package ir.neshane.customer.domain;

import ir.neshane.customer.domain.identity.IdentityIdentifier;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Value-free classification of actual changes, independent of identity evidence impact. */
public final class CriticalFieldChangeAssessment {
    public enum Field {
        GIVEN_NAME, FAMILY_NAME, COMPANY_NAME, IDENTIFIER_ADDED, IDENTIFIER_REMOVED,
        IDENTIFIER_TYPE, IDENTIFIER_VALUE, IDENTIFIER_ISSUING_COUNTRY, IDENTIFIER_ISSUER,
        IDENTIFIER_DATES, IDENTIFIER_STATUS, ENGLISH_NAME, CONTACT_INFO, EXCEPTIONAL_CHANGE
    }
    public enum ExceptionScope { ORDINARY, EXCEPTIONAL, UNRESOLVED }
    public enum Outcome { ELIGIBLE_CANDIDATE, UNCHANGED, BLOCKED }
    public record Assessment(Outcome outcome,String code,Set<Field> fields,boolean reasonRequired,boolean auditRequired) {
        public Assessment { fields = Set.copyOf(fields); }
    }
    private CriticalFieldChangeAssessment() {}

    public static Assessment assess(Customer original,Customer proposed,ExceptionScope exceptionScope,String reason) {
        Objects.requireNonNull(original);
        Objects.requireNonNull(proposed);
        Objects.requireNonNull(exceptionScope);
        if (!original.identityInfo().owner().equals(proposed.identityInfo().owner()))
            return new Assessment(Outcome.BLOCKED,"CUSTOMER_FIELD_CHANGE_SCOPE_MISMATCH",Set.of(),false,false);
        var fields = differences(original,proposed);
        if (fields.isEmpty()) return new Assessment(Outcome.UNCHANGED,"CUSTOMER_FIELD_CHANGE_UNCHANGED",fields,false,false);
        boolean critical = fields.stream().anyMatch(CriticalFieldChangeAssessment::critical);
        if (exceptionScope == ExceptionScope.UNRESOLVED)
            return new Assessment(Outcome.BLOCKED,"CUSTOMER_EXCEPTION_SCOPE_UNRESOLVED",fields,critical,false);
        if (exceptionScope == ExceptionScope.EXCEPTIONAL) fields.add(Field.EXCEPTIONAL_CHANGE);
        boolean required = critical || exceptionScope == ExceptionScope.EXCEPTIONAL;
        if (required && (reason == null || reason.codePoints().allMatch(cp -> Character.isWhitespace(cp) || Character.isSpaceChar(cp))))
            return new Assessment(Outcome.BLOCKED,"CUSTOMER_CRITICAL_CHANGE_REASON_REQUIRED",fields,true,false);
        return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"CUSTOMER_FIELD_CHANGE_CANDIDATE",fields,required,true);
    }
    static boolean critical(Field field) {
        return field != Field.ENGLISH_NAME && field != Field.CONTACT_INFO;
    }
    static EnumSet<Field> differences(Customer original,Customer proposed) {
        var fields = EnumSet.noneOf(Field.class);
        switch (original) {
            case Person person -> {
                var next = (Person) proposed;
                if (!person.givenName().equals(next.givenName())) fields.add(Field.GIVEN_NAME);
                if (!person.familyName().equals(next.familyName())) fields.add(Field.FAMILY_NAME);
            }
            case Company company -> {
                if (!company.companyName().equals(((Company) proposed).companyName())) fields.add(Field.COMPANY_NAME);
            }
        }
        if (!original.englishName().equals(proposed.englishName())) fields.add(Field.ENGLISH_NAME);
        if (!original.contactInfo().equals(proposed.contactInfo())) fields.add(Field.CONTACT_INFO);
        var before = new HashMap<String,IdentityIdentifier>();
        var after = new HashMap<String,IdentityIdentifier>();
        original.identityInfo().identifiers().forEach(i -> before.put(i.id(),i));
        proposed.identityInfo().identifiers().forEach(i -> after.put(i.id(),i));
        var ids = new HashSet<>(before.keySet());
        ids.addAll(after.keySet());
        for (var id : ids) {
            var a = before.get(id);
            var b = after.get(id);
            if (a == null) { fields.add(Field.IDENTIFIER_ADDED); continue; }
            if (b == null) { fields.add(Field.IDENTIFIER_REMOVED); continue; }
            if (!a.typeId().equals(b.typeId())) fields.add(Field.IDENTIFIER_TYPE);
            if (!a.value().equals(b.value())) fields.add(Field.IDENTIFIER_VALUE);
            if (!a.issuingCountry().equals(b.issuingCountry())) fields.add(Field.IDENTIFIER_ISSUING_COUNTRY);
            if (!a.issuingAuthority().equals(b.issuingAuthority())) fields.add(Field.IDENTIFIER_ISSUER);
            if (!a.issueDate().equals(b.issueDate()) || !a.expiryDate().equals(b.expiryDate())) fields.add(Field.IDENTIFIER_DATES);
            if (a.status() != b.status()) fields.add(Field.IDENTIFIER_STATUS);
        }
        return fields;
    }
}
