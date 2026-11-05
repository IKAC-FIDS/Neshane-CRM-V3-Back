package ir.neshane.alpha.application;

import ir.neshane.contract.customer.CustomerContract;

public final class AllowedContractUse {

    public String customerId(CustomerContract contract) {
        return contract.customerId();
    }
}
