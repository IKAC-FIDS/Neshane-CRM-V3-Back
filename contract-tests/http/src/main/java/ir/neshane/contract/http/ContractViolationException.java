package ir.neshane.contract.http;

import java.util.List;

public final class ContractViolationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ContractViolationException(String message) {
        super(message);
    }

    public ContractViolationException(String heading, List<String> violations) {
        super(heading + System.lineSeparator() + String.join(System.lineSeparator(), violations));
    }
}
