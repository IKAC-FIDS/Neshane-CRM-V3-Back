package ir.neshane.customer.domain.duplicate;

import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;

import ir.neshane.customer.domain.contact.ContactPoint;

import ir.neshane.customer.domain.contact.ContactValue;

import java.text.Normalizer;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Conservative exact normalized hints, never identity proof or deliverability checks. */
public final class BasicSimilarityDetection {
    private BasicSimilarityDetection() {}
    public static DuplicateDetection.Comparison compare(Customer candidate, DuplicateDetection.Request request, Customer existing) {
        Objects.requireNonNull(existing);
        if (!DuplicateDetection.Request.of(candidate, request.operation(), request.requestId()).equals(request))
            throw new IllegalArgumentException("DUPLICATE_REQUEST_SCOPE_MISMATCH");
        if (!candidate.tenantId().equals(existing.tenantId()))
            throw new IllegalArgumentException("DUPLICATE_EVIDENCE_SCOPE_MISMATCH");
        var signals = EnumSet.noneOf(DuplicateDetection.Signal.class);
        if (namesMatch(candidate, existing)) signals.add(DuplicateDetection.Signal.NAME);
        if (intersects(phones(candidate), phones(existing))) signals.add(DuplicateDetection.Signal.PHONE);
        if (intersects(emails(candidate), emails(existing))) signals.add(DuplicateDetection.Signal.EMAIL);
        return new DuplicateDetection.Comparison(request, existing.tenantId(), existing.id(),
            existing.identityInfo().owner().kind(), signals);
    }
    private static boolean namesMatch(Customer a, Customer b) {
        if (a instanceof Person x && b instanceof Person y)
            return equalName(x.givenName(), y.givenName()) && equalName(x.familyName(), y.familyName());
        if (a instanceof Company x && b instanceof Company y)
            return equalName(x.companyName(), y.companyName());
        return false;
    }
    private static boolean equalName(String a, String b) {
        String x = normalize(a), y = normalize(b);
        return !x.isEmpty() && x.equals(y);
    }
    private static String normalize(String text) {
        return digits(Normalizer.normalize(text, Normalizer.Form.NFC))
            .replace('ي','ی').replace('ك','ک')
            .replaceAll("[\\p{Z}\\s\\u200C]+", " ").strip().toLowerCase(Locale.ROOT);
    }
    private static String digits(String text) {
        var result = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (cp >= 0x660 && cp <= 0x669) result.append((char) ('0' + cp - 0x660));
            else if (cp >= 0x6f0 && cp <= 0x6f9) result.append((char) ('0' + cp - 0x6f0));
            else result.appendCodePoint(cp);
        });
        return result.toString();
    }
    private static Set<String> phones(Customer customer) {
        var values = new java.util.HashSet<String>();
        for (var point : customer.contactInfo().points()) {
            if (point.status() == ContactPoint.Status.ACTIVE && point.value() instanceof ContactValue.Phone phone) {
                String key = digits(phone.number()).replaceAll("[\\p{Z}\\s()\\-]", "");
                // Keep country prefix and leading zero; no inferred international equivalence.
                if (key.matches("\\+?[0-9]+")) values.add(key);
            }
        }
        return values;
    }
    private static Set<String> emails(Customer customer) {
        var values = new java.util.HashSet<String>();
        for (var point : customer.contactInfo().points()) {
            if (point.status() == ContactPoint.Status.ACTIVE && point.value() instanceof ContactValue.Email email) {
                String key = email.address().strip();
                int at = key.indexOf('@');
                if (at > 0 && at == key.lastIndexOf('@') && at < key.length() - 1 && !key.matches(".*[\\p{Z}\\s].*"))
                    values.add(key.substring(0, at) + "@" + key.substring(at + 1).toLowerCase(Locale.ROOT));
            }
        }
        return values;
    }
    private static boolean intersects(Set<String> a, Set<String> b) {
        return a.stream().anyMatch(b::contains);
    }
}
