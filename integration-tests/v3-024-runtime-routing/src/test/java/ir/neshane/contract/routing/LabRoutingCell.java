package ir.neshane.contract.routing;

import static ir.neshane.contract.routing.LabRoutingTrust.*;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Independent destination; private journal/replay/fence linearization, in-process only. */
final class LabRoutingCell {
    private static final class Slot {
        DestinationFencingPolicy policy;
        long generation;
        long sequence;
        long revision;
        boolean enabled;
        UUID snapshot;
    }
    private final LabRoutingTrust trust;
    private final String authority;
    private final String boot = UUID.randomUUID().toString();
    private final Map<String, Slot> slots = new HashMap<>();
    private final Map<String, String> replay = new HashMap<>();
    private final Path journal = Path.of("/tmp/effects.journal");
    private final AtomicInteger received = new AtomicInteger();
    private volatile String holding = "";
    private volatile String preparedId = "";
    private volatile CompletableFuture<Void> release = CompletableFuture.completedFuture(null);
    private volatile boolean dropResponse;
    private volatile boolean redirect;

    private LabRoutingCell(LabRoutingTrust trust, String authority) { this.trust = trust; this.authority = authority; }
    public static void main(String[] args) throws Exception {
        var instance = new LabRoutingCell(new LabRoutingTrust(Path.of(args[0]), args[2], args[1]), args[3]);
        Files.writeString(instance.journal, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        serve(instance.trust, instance::handle);
    }

    private Map<String, String> handle(String path, byte[] body) throws Exception {
        if (path.equals("/control")) {
            var c = trust.control(body);
            switch (c.get("command")) {
                case "health" -> { return fields("code", "OK", "boot", boot, "provider", java.security.Signature.getInstance("Ed25519").getProvider().getName()); }
                case "stats" -> {
                    synchronized (this) { return fields("code", "OK", "journal", Files.readString(journal), "received", "" + received.get(), "prepared", preparedId, "boot", boot); }
                }
                case "clock" -> trust.now = number(c, "value");
                case "hold" -> { holding = c.get("id"); preparedId = ""; release = new CompletableFuture<>(); }
                case "release" -> { release.complete(null); holding = ""; }
                case "drop" -> dropResponse = Boolean.parseBoolean(c.get("value"));
                case "redirect" -> redirect = Boolean.parseBoolean(c.get("value"));
                case "connectivity" -> {
                    boolean reachable;
                    try { post(c.get("uri") + "/control", unpack(c.get("packet")), 1); reachable = true; }
                    catch (java.io.IOException | Denied failure) { reachable = false; }
                    return fields("code", "OK", "reachable", "" + reachable);
                }
                case "activate" -> {
                    var p = trust.placement(authority, c.get("tenant"));
                    require(p.get("cell").equals(trust.role), "ACTIVATION_CELL");
                    synchronized (this) {
                        Slot s = slots.computeIfAbsent(c.get("tenant"), ignored -> new Slot());
                        long generation = number(p,"generation"), sequence = number(p,"sequence");
                        require(generation >= s.generation && sequence >= s.sequence, "ACTIVATION_ROLLBACK");
                        require(generation > s.generation || sequence > s.sequence || s.snapshot == null || s.snapshot.toString().equals(p.get("snapshot")), "ACTIVATION_CONFLICT");
                        s.generation = generation; s.sequence = sequence; s.snapshot = UUID.fromString(p.get("snapshot"));
                        s.policy = new DestinationFencingPolicy(new DestinationFencingPolicy.FenceState(new TenantId(c.get("tenant")), new CellId(trust.role), generation, ++s.revision, s.snapshot, sequence));
                        s.enabled = true;
                    }
                }
                case "fence" -> {
                    synchronized (this) {
                        Slot s = slots.get(c.get("tenant")); require(s != null, "FENCE_MISSING");
                        s.policy.advanceFence(new CellId(c.get("cell")), number(c,"generation"));
                        s.generation = number(c,"generation"); s.revision++; s.enabled = false;
                    }
                }
                default -> throw new Denied("CONTROL_COMMAND");
            }
            return fields("code", "OK");
        }
        if (path.equals("/challenge")) {
            var q = trust.verify(body, "router"); schema(q,"type","nonce"); require("challenge".equals(q.get("type")),"CHALLENGE_TYPE");
            if (redirect) { return fields("redirect-to", "http://cell-b:8080/operate"); }
            return fields("code", "OK", "nonce", q.get("nonce"), "cell", trust.role, "boot", boot);
        }
        require(path.equals("/operate"), "UNKNOWN_PATH"); received.incrementAndGet();
        var envelope = trust.verify(body, "router");
        schema(envelope,"type","request","tenant","cell","generation","sequence","snapshot","boot");
        require("forward".equals(envelope.get("type")) && trust.role.equals(envelope.get("cell")) && boot.equals(envelope.get("boot")), "DESTINATION_BINDING");
        var request = clientRequest(trust, unpack(envelope.get("request")));
        require(request.get("tenant").equals(envelope.get("tenant")), "ENVELOPE_TENANT");
        var current = trust.placement(authority, request.get("tenant"));
        require(current.get("cell").equals(trust.role) && current.get("generation").equals(envelope.get("generation"))
                && current.get("sequence").equals(envelope.get("sequence")) && current.get("snapshot").equals(envelope.get("snapshot")), "DESTINATION_STALE");
        var decision = resolve(request,current,trust.now); require(decision.outcome() == TenantRoutingPolicy.Outcome.ROUTE_ALLOWED, "DOMAIN_ROUTE_DENIED");
        require(request.get("destination").isEmpty() && (request.get("header-tenant").isEmpty() || request.get("header-tenant").equals(request.get("tenant"))), "DESTINATION_CONTEXT");
        Slot slot; long revision; DestinationFencingPolicy.PreparedMutation prepared;
        String key = request.get("tenant") + ":" + request.get("id");
        String meaning = hash(encode(request));
        synchronized (this) {
            slot = slots.get(request.get("tenant")); require(slot != null && slot.enabled, "DESTINATION_NOT_ACTIVE");
            require(!replay.containsKey(key), replay.getOrDefault(key, "").equals(meaning) ? "REPLAY" : "OPERATION_ID_CONFLICT");
            revision = slot.revision;
            prepared = slot.policy.prepare(new DestinationFencingPolicy.FencingEvidence(UUID.randomUUID(), new TenantId(request.get("tenant")), new CellId(trust.role),
                    number(envelope,"generation"), UUID.fromString(envelope.get("snapshot")), number(envelope,"sequence"), authority(AuthorityScope.PLACEMENT)),
                    new DestinationFencingPolicy.DomainAuthorizationEvidence(new TenantId(request.get("tenant")), request.get("action"), "lab-cell-domain", 1),
                    new TenantId(request.get("tenant")), new CellId(trust.role));
        }
        if (holding.equals(request.get("id"))) { preparedId = holding; release.get(15, TimeUnit.SECONDS); }
        synchronized (this) {
            require(slot.enabled && slot.revision == revision, "FENCE_ADVANCED");
            require(!replay.containsKey(key), "REPLAY");
            slot.policy.commit(prepared, () -> {
                try {
                    String line = request.get("id") + "|" + request.get("tenant") + "|" + trust.role + "|" + envelope.get("generation") + "|" + meaning + "\n";
                    try (var channel = FileChannel.open(journal, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                        ByteBuffer bytes = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8)); while (bytes.hasRemaining()) { channel.write(bytes); } channel.force(true);
                    }
                    replay.put(key, meaning);
                } catch (Exception failure) { throw new IllegalStateException("JOURNAL_FAILURE", failure); }
            });
        }
        if (dropResponse) { throw new java.io.IOException("LAB_LOST_REPLY_AFTER_EFFECT"); }
        return fields("code", "OK", "cell", trust.role, "id", request.get("id"));
    }
}
