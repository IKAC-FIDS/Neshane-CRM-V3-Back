package ir.neshane.contract.routing;

import java.util.List;

final class RoutingContractException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    RoutingContractException(String message) {
        super(message);
    }

    RoutingContractException(String message, List<String> violations) {
        super(message + System.lineSeparator() + String.join(System.lineSeparator(), violations));
    }
}
