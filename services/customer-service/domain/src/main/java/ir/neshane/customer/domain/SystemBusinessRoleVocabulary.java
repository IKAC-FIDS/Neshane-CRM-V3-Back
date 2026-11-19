package ir.neshane.customer.domain;


import ir.neshane.customer.domain.shared.localization.LocalizedText;

import java.util.Map;
import java.util.Optional;
import static ir.neshane.customer.domain.BusinessRoleDefinition.Scope.*;

/** Initial product vocabulary; candidate identifiers are not a persistence contract. */
public enum SystemBusinessRoleVocabulary {
    CEO("مدیرعامل", "CEO", COMPANY),
    BOARD_MEMBER("عضو هیئت‌مدیره", "Board Member", COMPANY),
    BRANCH_MANAGER("مدیر شعبه", "Branch Manager", BRANCH),
    GENERAL_MANAGER("مدیر / مسئول واحد", "General Manager", BOTH),
    PRIMARY_CONTACT("رابط اصلی", "Primary Contact", BOTH),
    SALES_CONTACT("رابط امور فروش", "Sales Contact", BOTH),
    TECHNICAL_CONTACT("رابط فنی", "Technical Contact", BOTH),
    SUPPORT_CONTACT("رابط پشتیبانی", "Support Contact", BOTH),
    PROCUREMENT_CONTACT("مسئول خرید و تدارکات", "Procurement Contact", BOTH),
    CONTRACT_CONTACT("مسئول امور قراردادها", "Contract Contact", BOTH),
    TENDER_CONTACT("مسئول امور مناقصات", "Tender Contact", BOTH),
    LEGAL_CONTACT("رابط حقوقی", "Legal Contact", BOTH);

    private final BusinessRoleDefinition definition;
    SystemBusinessRoleVocabulary(String fa, String en, BusinessRoleDefinition.Scope scope) {
        definition = new BusinessRoleDefinition(name(), Optional.empty(), name(),
            new LocalizedText(Map.of("fa", fa, "en", en)), new LocalizedText(Map.of()), scope,
            BusinessRoleDefinition.Status.ACTIVE);
    }
    public BusinessRoleDefinition definition() { return definition; }
}
