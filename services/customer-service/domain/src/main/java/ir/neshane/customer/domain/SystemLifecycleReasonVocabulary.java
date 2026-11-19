package ir.neshane.customer.domain;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.CustomerLifecycle.Transition.*;
import static ir.neshane.customer.domain.LifecycleReasonDefinition.Scope.*;

public enum SystemLifecycleReasonVocabulary {
    CUSTOMER_REQUEST("درخواست مشتری", "Customer request", false, BOTH, DEACTIVATE, ARCHIVE),
    COOPERATION_ENDED("پایان همکاری", "Cooperation ended", false, BOTH, DEACTIVATE, ARCHIVE),
    BUSINESS_CLOSED("توقف فعالیت", "Business closed", false, CUSTOMER, DEACTIVATE, ARCHIVE),
    TEMPORARY_SUSPENSION("توقف موقت همکاری", "Temporary suspension", false, CUSTOMER, DEACTIVATE),
    REGISTRATION_ERROR("ثبت اشتباه", "Registration error", false, CUSTOMER, ARCHIVE),
    COOPERATION_RESUMED("شروع مجدد همکاری", "Cooperation resumed", false, BOTH, REACTIVATE),
    ARCHIVE_CORRECTION("اصلاح بایگانی", "Archive correction", false, CUSTOMER, RESTORE),
    OTHER("سایر موارد", "Other", true, BOTH, DEACTIVATE, REACTIVATE, ARCHIVE, RESTORE),
    BRANCH_CLOSED("تعطیلی شعبه", "Branch closed", false, BRANCH, DEACTIVATE),
    BRANCH_RELOCATION("جابه‌جایی شعبه", "Branch relocation", false, BRANCH, DEACTIVATE),
    BRANCH_REOPENED("بازگشایی شعبه", "Branch reopened", false, BRANCH, REACTIVATE);

    private final LifecycleReasonDefinition definition;

    SystemLifecycleReasonVocabulary(String fa, String en, boolean comment, LifecycleReasonDefinition.Scope scope,
                                    CustomerLifecycle.Transition... transitions) {
        definition = new LifecycleReasonDefinition(name(), Optional.empty(), name(),
            new LocalizedText(Map.of("fa", fa, "en", en)), new LocalizedText(Map.of()),
            Set.of(transitions), comment, LifecycleReasonDefinition.Status.ACTIVE, ordinal(), scope);
    }

    public LifecycleReasonDefinition definition() { return definition; }
}
