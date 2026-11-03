package ir.neshane.contract.event;

import java.util.List;

public final class EventContractException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EventContractException(String message) {
        super(message);
    }

    public EventContractException(String heading, List<String> violations) {
        super(heading + System.lineSeparator() + String.join(System.lineSeparator(), violations));
    }
}
