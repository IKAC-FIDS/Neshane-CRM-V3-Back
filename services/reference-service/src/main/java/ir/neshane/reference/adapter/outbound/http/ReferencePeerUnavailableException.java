package ir.neshane.reference.adapter.outbound.http;

public final class ReferencePeerUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ReferencePeerUnavailableException() {
        super("Reference peer call failed");
    }
}
