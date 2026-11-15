package ir.neshane.contract.routing;

import static ir.neshane.contract.routing.LabRoutingTrust.*;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Synthetic authority in a separate process. No real IAM or production delegation. */
final class LabRoutingAuthority {
    private final LabRoutingTrust trust;
    private final Map<String, Map<String, String>> placements = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile String fault = "";
    private volatile String injected = "";

    private LabRoutingAuthority(LabRoutingTrust trust) { this.trust = trust; }
    public static void main(String[] args) throws Exception {
        var instance = new LabRoutingAuthority(new LabRoutingTrust(Path.of(args[0]), "authority", args[1]));
        serve(instance.trust, instance::handle);
    }

    private Map<String, String> handle(String path, byte[] body) throws Exception {
        if (path.equals("/control")) {
            var c = trust.control(body); String command = c.get("command");
            switch (command) {
                case "health" -> { return fields("code", "OK", "provider", java.security.Signature.getInstance("Ed25519").getProvider().getName()); }
                case "fault" -> { fault = c.get("value"); injected = c.getOrDefault("packet", ""); }
                case "node-control", "inject" -> {
                    String cell = c.get("cell"); require(java.util.Set.of("cell-a", "cell-b").contains(cell), "CONTROL_TARGET");
                    // Explicit authenticated test-controller channel only, never a client forwarding API.
                    byte[] response = post("http://" + cell + ":8080/" + (command.equals("inject") ? "operate" : "control"), unpack(c.get("packet")));
                    return fields("code", "OK", "packet", pack(response));
                }
                case "place" -> {
                    var p = fields("type", "placement", "tenant", c.get("tenant"), "cell", c.get("cell"),
                            "generation", c.get("generation"), "sequence", c.get("sequence"), "snapshot", c.getOrDefault("snapshot", UUID.randomUUID().toString()),
                            "not-before", c.getOrDefault("not-before", "1"), "expires", c.getOrDefault("expires", "1000"),
                            "issuer", c.getOrDefault("issuer", "placement-authority"), "deployment", c.getOrDefault("deployment", DEPLOYMENT),
                            "profile", c.getOrDefault("profile", PROFILE.name()), "scope", "tenant-routing",
                            "cell-state", c.getOrDefault("cell-state", "ACTIVE"), "available", c.getOrDefault("available", "AVAILABLE"));
                    placements.put(c.get("tenant"), p);
                }
                case "mint" -> {
                    var identity = fields("type", "identity", "subject", c.getOrDefault("subject", c.get("holder")),
                            "holder", c.get("holder"), "tenant", c.get("tenant"), "membership", c.getOrDefault("membership", "ACTIVE"),
                            "not-before", c.getOrDefault("not-before", "1"), "expires", c.getOrDefault("expires", "1000"),
                            "issuer", c.getOrDefault("issuer", "lab-identity"), "audience", c.getOrDefault("audience", "router"));
                    return fields("code", "OK", "token", pack(trust.sign(identity)));
                }
                default -> throw new Denied("CONTROL_COMMAND");
            }
            return fields("code", "OK");
        }
        require(path.equals("/placement"), "UNKNOWN_PATH");
        String signer = decode(body).get("signer");
        require(java.util.Set.of("router", "cell-a", "cell-b").contains(signer), "QUERY_CALLER");
        var query = trust.verify(body, signer); schema(query, "type", "tenant", "nonce");
        require("query".equals(query.get("type")), "QUERY_TYPE");
        if (fault.equals("unavailable")) { throw new java.io.IOException("LAB_AUTHORITY_UNAVAILABLE"); }
        if (fault.equals("inject")) { return fields("raw-packet", injected); }
        var p = placements.get(query.get("tenant")); require(p != null, "PLACEMENT_MISSING");
        var result = new TreeMap<>(p); result.put("nonce", query.get("nonce")); return result;
    }
}
