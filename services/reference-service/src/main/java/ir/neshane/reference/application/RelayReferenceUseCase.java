package ir.neshane.reference.application;

import ir.neshane.reference.application.port.out.ReferencePeerPort;
public final class RelayReferenceUseCase {
    private final ReferencePeerPort peerPort;

    public RelayReferenceUseCase(ReferencePeerPort peerPort) {
        this.peerPort = peerPort;
    }

    public ReferencePeerPort.PeerResult relay(String message) {
        return peerPort.echo(message);
    }
}
