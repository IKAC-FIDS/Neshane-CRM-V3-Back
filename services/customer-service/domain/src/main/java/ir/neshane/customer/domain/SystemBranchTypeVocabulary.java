package ir.neshane.customer.domain;

import java.util.Map;
import java.util.Optional;

public enum SystemBranchTypeVocabulary {
    HEAD_OFFICE("دفتر مرکزی", "Head Office"),
    BRANCH("شعبه", "Branch"),
    REGIONAL_OFFICE("دفتر منطقه‌ای", "Regional Office"),
    SALES_OFFICE("دفتر فروش", "Sales Office"),
    SUPPORT_CENTER("مرکز پشتیبانی", "Support Center"),
    WAREHOUSE("انبار", "Warehouse"),
    FACTORY("کارخانه", "Factory"),
    REPRESENTATIVE_OFFICE("دفتر نمایندگی", "Representative Office"),
    OTHER("سایر", "Other");

    private final BranchTypeDefinition definition;
    SystemBranchTypeVocabulary(String fa, String en) {
        definition = new BranchTypeDefinition(name(), Optional.empty(), name(), new LocalizedText(Map.of("fa", fa, "en", en)),
            new LocalizedText(Map.of()), BranchTypeDefinition.Status.ACTIVE, ordinal());
    }
    public BranchTypeDefinition definition() { return definition; }
}
