package ir.neshane.reference.application.port.out;

public interface ReferencePeerPort {
    PeerResult echo(String message);

    record PeerResult(String responder, String message, String correlationId, String traceId) {}
}
