package ir.neshane.customer.domain.customer.update;

import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;

import java.util.HashSet;

/** Pure difference assessment over explicitly supplied snapshots. */
public record IdentityRevisionDifferences(boolean identifiers, boolean critical, boolean changed) {
    public static IdentityRevisionDifferences assess(Customer original, Customer proposed) {
        boolean identifiers = !new HashSet<>(original.identityInfo().identifiers()).equals(new HashSet<>(proposed.identityInfo().identifiers()));
        boolean names = switch (original) {
            case Person p -> !p.givenName().equals(((Person) proposed).givenName()) || !p.familyName().equals(((Person) proposed).familyName());
            case Company c -> !c.companyName().equals(((Company) proposed).companyName());
        };
        boolean critical = identifiers || names;
        boolean changed = critical || !original.englishName().equals(proposed.englishName()) || !original.contactInfo().equals(proposed.contactInfo());
        return new IdentityRevisionDifferences(identifiers, critical, changed);
    }
}
