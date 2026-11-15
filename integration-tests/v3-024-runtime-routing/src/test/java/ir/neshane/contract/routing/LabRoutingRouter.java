package ir.neshane.contract.routing;

import static ir.neshane.contract.routing.LabRoutingTrust.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Fail-closed router with no automatic retry, redirect, proxy or alternate Cell path. */
final class LabRoutingRouter {
    private final LabRoutingTrust trust;
    private final String authority;
    private final Map<String, String> destinations = Map.of("cell-a", "http://cell-a:8080", "cell-b", "http://cell-b:8080");
    private final Path journal = Path.of("/tmp/forward.journal");
    private volatile String holding = "";
    private volatile String held = "";
    private volatile boolean wrongPeer;
    private volatile CompletableFuture<Void> release = CompletableFuture.completedFuture(null);

    private LabRoutingRouter(LabRoutingTrust trust, String authority) { this.trust = trust; this.authority = authority; }
    public static void main(String[] args) throws Exception {
        var instance = new LabRoutingRouter(new LabRoutingTrust(Path.of(args[0]), "router", args[1]), args[2]);
        Files.writeString(instance.journal, "", StandardOpenOption.CREATE_NEW);
        serve(instance.trust, instance::handle);
    }

    private Map<String, String> handle(String path, byte[] body) throws Exception {
        if (path.equals("/control")) {
            var c = trust.control(body);
            switch (c.get("command")) {
                case "health" -> { return fields("code", "OK", "provider", java.security.Signature.getInstance("Ed25519").getProvider().getName()); }
                case "stats" -> { synchronized (this) { return fields("code", "OK", "journal", Files.readString(journal), "held", held); } }
                case "clock" -> trust.now = number(c,"value");
                case "hold" -> { holding = c.get("id"); held = ""; release = new CompletableFuture<>(); }
                case "release" -> { release.complete(null); holding = ""; }
                case "wrong-peer" -> wrongPeer = Boolean.parseBoolean(c.get("value"));
                case "destination" -> require(destinations.get(c.get("cell")).equals(c.get("uri")), "DESTINATION_ALLOWLIST");
                default -> throw new Denied("CONTROL_COMMAND");
            }
            return fields("code", "OK");
        }
        require(path.equals("/operate"), "UNKNOWN_PATH");
        var request = clientRequest(trust, body);
        require(request.get("destination").isEmpty(), "UNTRUSTED_DESTINATION");
        require(request.get("header-tenant").isEmpty() || request.get("header-tenant").equals(request.get("tenant")), "CONFLICTING_TENANT_HEADER");
        var p = trust.placement(authority, request.get("tenant"));
        var decision = resolve(request,p,trust.now);
        require(decision.outcome() == TenantRoutingPolicy.Outcome.ROUTE_ALLOWED, decision.outcome().name());
        String cell = decision.targetCellId().value();
        if (holding.equals(request.get("id"))) { held = holding; release.get(15, TimeUnit.SECONDS); }
        String uri = destinations.get(wrongPeer ? (cell.equals("cell-a") ? "cell-b" : "cell-a") : cell);
        require(uri != null, "DESTINATION_ALLOWLIST");
        String nonce = UUID.randomUUID().toString();
        var challenge = trust.rpc(uri + "/challenge", fields("type", "challenge", "nonce", nonce), cell);
        schema(challenge,"code","nonce","cell","boot");
        require("OK".equals(challenge.get("code")) && nonce.equals(challenge.get("nonce")) && cell.equals(challenge.get("cell")), "DESTINATION_IDENTITY");
        byte[] envelope = trust.sign(fields("type", "forward", "request", pack(body), "tenant", request.get("tenant"), "cell", cell,
                "generation", p.get("generation"), "sequence", p.get("sequence"), "snapshot", p.get("snapshot"), "boot", challenge.get("boot")));
        synchronized (this) {
            Files.writeString(journal, request.get("id") + "|" + request.get("tenant") + "|" + cell + "|" + p.get("generation") + "\n", StandardOpenOption.APPEND);
        }
        var result = trust.verify(post(uri + "/operate", envelope, 12), cell);
        if (!"OK".equals(result.get("code"))) { return fields("code", result.get("code"), "reason", "DESTINATION_" + result.get("reason")); }
        return fields("code", "OK", "cell", cell, "id", request.get("id"));
    }
}
