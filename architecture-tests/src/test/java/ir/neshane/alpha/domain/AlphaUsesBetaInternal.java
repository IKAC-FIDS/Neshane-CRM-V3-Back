package ir.neshane.alpha.domain;

import ir.neshane.beta.internal.BetaInternalType;

public final class AlphaUsesBetaInternal {

    private final BetaInternalType internalType;

    public AlphaUsesBetaInternal(BetaInternalType internalType) {
        this.internalType = internalType;
    }

    public String execute() {
        return internalType.value();
    }
}
