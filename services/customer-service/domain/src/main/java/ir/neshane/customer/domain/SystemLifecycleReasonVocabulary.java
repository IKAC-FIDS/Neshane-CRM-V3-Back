package ir.neshane.customer.domain;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.CustomerLifecycle.Transition.*;

public enum SystemLifecycleReasonVocabulary {
    CUSTOMER_REQUEST("درخواست مشتری", "Customer request", false, DEACTIVATE, ARCHIVE),
    COOPERATION_ENDED("پایان همکاری", "Cooperation ended", false, DEACTIVATE, ARCHIVE),
    BUSINESS_CLOSED("توقف فعالیت", "Business closed", false, DEACTIVATE, ARCHIVE),
    TEMPORARY_SUSPENSION("توقف موقت همکاری", "Temporary suspension", false, DEACTIVATE),
    REGISTRATION_ERROR("ثبت اشتباه", "Registration error", false, ARCHIVE),
    COOPERATION_RESUMED("شروع مجدد همکاری", "Cooperation resumed", false, REACTIVATE),
    ARCHIVE_CORRECTION("اصلاح بایگانی", "Archive correction", false, RESTORE),
    OTHER("سایر موارد", "Other", true, DEACTIVATE, REACTIVATE, ARCHIVE, RESTORE);

    private final LifecycleReasonDefinition definition;

    SystemLifecycleReasonVocabulary(String fa, String en, boolean comment, CustomerLifecycle.Transition... transitions) {
        definition = new LifecycleReasonDefinition(name(), Optional.empty(), name(),
            new LocalizedText(Map.of("fa", fa, "en", en)), new LocalizedText(Map.of()),
            Set.of(transitions), comment, LifecycleReasonDefinition.Status.ACTIVE, ordinal());
    }

    public LifecycleReasonDefinition definition() { return definition; }
}
