package ir.neshane.contract.routing;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;

/** JDK-only, laboratory wire profile. Not JWT, production PKI, or published contract. */
final class LabRoutingTrust {
    static final String DEPLOYMENT = "rref-laboratory";
    static final DeploymentProfile PROFILE = DeploymentProfile.DEDICATED_MANAGED;
    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
            .followRedirects(HttpClient.Redirect.NEVER).proxy(new ProxySelector() {
                @Override public List<Proxy> select(URI uri) { return List.of(Proxy.NO_PROXY); }
                @Override public void connectFailed(URI uri, SocketAddress address, IOException failure) { }
            }).build();
    final String role;
    final String run;
    private final PrivateKey privateKey;
    private final Map<String, PublicKey> publicKeys = new HashMap<>();
    private final Set<String> commands = new HashSet<>();
    private final Map<String, Long> sequences = new HashMap<>();
    private final Map<String, String> digests = new HashMap<>();
    private final Map<String, Long> generations = new HashMap<>();
    private final Map<String, String> cells = new HashMap<>();
    volatile long now = 100;

    LabRoutingTrust(Path directory, String role, String run) throws Exception {
        this.role = role; this.run = run;
        var factory = KeyFactory.getInstance("Ed25519");
        privateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(directory.resolve(role + ".key"))));
        for (String name : List.of("authority", "router", "cell-a", "cell-b", "client-a", "client-b", "controller", "rogue")) {
            publicKeys.put(name, factory.generatePublic(new X509EncodedKeySpec(Files.readAllBytes(directory.resolve(name + ".pub")))));
        }
    }

    static Map<String, String> fields(String... values) {
        require(values.length % 2 == 0, "FIELDS");
        var result = new TreeMap<String, String>();
        for (int i = 0; i < values.length; i += 2) { require(result.put(values[i], values[i+1]) == null, "DUPLICATE_FIELD"); }
        return result;
    }

    static byte[] encode(Map<String, String> fields) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(0x52524631); out.writeInt(fields.size());
            for (var entry : new TreeMap<>(fields).entrySet()) {
                for (String value : List.of(entry.getKey(), entry.getValue())) {
                    byte[] text = value.getBytes(StandardCharsets.UTF_8);
                    require(text.length <= 262144, "FIELD_SIZE"); out.writeInt(text.length); out.write(text);
                }
            }
        }
        require(bytes.size() <= 524288, "MESSAGE_SIZE"); return bytes.toByteArray();
    }

    static Map<String, String> decode(byte[] bytes) throws IOException {
        require(bytes.length <= 524288, "MESSAGE_SIZE");
        var map = new TreeMap<String, String>();
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            require(in.readInt() == 0x52524631, "WIRE_VERSION"); int count = in.readInt();
            require(count >= 0 && count <= 64, "FIELD_COUNT"); String previous = "";
            for (int i = 0; i < count; i++) {
                String[] pair = new String[2];
                for (int j = 0; j < 2; j++) {
                    int size = in.readInt(); require(size >= 0 && size <= 262144, "FIELD_SIZE");
                    byte[] value = in.readNBytes(size); require(value.length == size, "TRUNCATED_FIELD");
                    pair[j] = new String(value, StandardCharsets.UTF_8);
                    require(java.util.Arrays.equals(value, pair[j].getBytes(StandardCharsets.UTF_8)), "INVALID_UTF8");
                }
                require(pair[0].matches("[a-z][a-z0-9-]*") && pair[0].compareTo(previous) > 0, "FIELD_ORDER_OR_DUPLICATE");
                previous = pair[0]; map.put(pair[0], pair[1]);
            }
            require(in.available() == 0, "TRAILING_BYTES");
        } catch (java.io.EOFException truncated) { throw new Denied("WIRE_TRUNCATED"); }
        return map;
    }

    byte[] sign(Map<String, String> input) throws Exception {
        var map = new TreeMap<>(input); map.put("run", run); map.put("signer", role);
        require(!map.containsKey("signature"), "SIGNATURE_INPUT");
        var signer = Signature.getInstance("Ed25519"); signer.initSign(privateKey); signer.update(encode(map));
        map.put("signature", Base64.getEncoder().encodeToString(signer.sign())); return encode(map);
    }

    Map<String, String> verify(byte[] packet, String expected) throws Exception {
        var map = decode(packet); String value = map.remove("signature");
        require(run.equals(map.get("run")) && expected.equals(map.get("signer")), "SIGNER_OR_RUN");
        require(value != null, "MISSING_SIGNATURE");
        var verifier = Signature.getInstance("Ed25519"); verifier.initVerify(publicKeys.get(expected)); verifier.update(encode(map));
        try { require(verifier.verify(Base64.getDecoder().decode(value)), "SIGNATURE_INVALID"); }
        catch (java.security.SignatureException | IllegalArgumentException invalid) { throw new Denied("SIGNATURE_ENCODING"); }
        return map;
    }

    static void schema(Map<String, String> map, String... keys) {
        var expected = new HashSet<>(List.of(keys)); expected.add("run"); expected.add("signer");
        require(map.keySet().equals(expected), "SCHEMA_FIELDS");
    }

    synchronized Map<String, String> control(byte[] packet) throws Exception {
        var map = verify(packet, "controller");
        require("control".equals(map.get("type")) && map.containsKey("nonce"), "CONTROL_TYPE");
        require(commands.add(map.get("nonce")), "CONTROL_REPLAY"); return map;
    }

    static byte[] post(String uri, byte[] body) throws Exception {
        return post(uri, body, 3);
    }
    static byte[] post(String uri, byte[] body, int seconds) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(seconds))
                .header("Content-Type", "application/octet-stream").POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
        require(response.statusCode() == 200, "TRANSPORT_" + response.statusCode()); return response.body();
    }

    Map<String, String> rpc(String uri, Map<String, String> message, String peer) throws Exception {
        return verify(post(uri, sign(message)), peer);
    }

    Map<String, String> placement(String authority, String tenant) throws Exception {
        String challenge = UUID.randomUUID().toString();
        var result = rpc(authority + "/placement", fields("type", "query", "tenant", tenant, "nonce", challenge), "authority");
        if ("UNAVAILABLE".equals(result.get("code"))) { throw new IOException("AUTHORITY_UNAVAILABLE"); }
        if ("DENIED".equals(result.get("code"))) { throw new Denied("AUTHORITY_" + result.get("reason")); }
        schema(result, "type", "tenant", "cell", "generation", "sequence", "snapshot", "not-before", "expires", "issuer", "deployment", "profile", "scope", "cell-state", "available", "nonce");
        require("placement".equals(result.get("type")) && tenant.equals(result.get("tenant")) && challenge.equals(result.get("nonce")), "PLACEMENT_BINDING");
        require("placement-authority".equals(result.get("issuer")) && DEPLOYMENT.equals(result.get("deployment"))
                && PROFILE.name().equals(result.get("profile")) && "tenant-routing".equals(result.get("scope")), "PLACEMENT_AUTHORITY");
        require(now >= number(result, "not-before") && now < number(result, "expires"), "PLACEMENT_EXPIRED");
        long sequence = number(result, "sequence");
        var semantic = new TreeMap<>(result); semantic.remove("nonce");
        String digest = hash(encode(semantic));
        synchronized (this) {
            long old = sequences.getOrDefault(tenant, 0L);
            require(sequence >= old, "PLACEMENT_ROLLBACK");
            require(sequence != old || digest.equals(digests.get(tenant)), "PLACEMENT_CONFLICT");
            long generation = number(result, "generation"), oldGeneration = generations.getOrDefault(tenant, 0L);
            require(generation >= oldGeneration, "GENERATION_ROLLBACK");
            require(generation != oldGeneration || result.get("cell").equals(cells.get(tenant)), "GENERATION_CELL_CONFLICT");
            sequences.put(tenant, sequence); digests.put(tenant, digest);
            generations.put(tenant, generation); cells.put(tenant, result.get("cell"));
        }
        return result;
    }

    Map<String, String> identity(Map<String, String> request) throws Exception {
        String client = request.get("signer"); require(Set.of("client-a", "client-b").contains(client), "CLIENT_UNKNOWN");
        var token = verify(unpack(request.get("identity")), "authority");
        schema(token, "type", "subject", "holder", "tenant", "membership", "not-before", "expires", "issuer", "audience");
        require("identity".equals(token.get("type")) && "lab-identity".equals(token.get("issuer")) && "router".equals(token.get("audience")), "IDENTITY_AUTHORITY");
        require(client.equals(token.get("holder")) && client.equals(token.get("subject")), "IDENTITY_HOLDER");
        require(now >= number(token, "not-before") && now < number(token, "expires") && "ACTIVE".equals(token.get("membership")), "IDENTITY_NOT_CURRENT");
        require(token.get("tenant").equals(request.get("tenant")), "TENANT_BINDING");
        require(request.get("object").equals(request.get("tenant") + ":record") && "write".equals(request.get("action")), "OBJECT_OR_ACTION_DENIED");
        require(request.get("id").matches("[A-Za-z0-9-]{1,80}") && request.get("payload").length() <= 128, "REQUEST_SHAPE");
        return token;
    }

    static Map<String, String> clientRequest(LabRoutingTrust trust, byte[] body) throws Exception {
        String role = decode(body).get("signer");
        require(Set.of("client-a", "client-b").contains(role), "CLIENT_UNKNOWN");
        var request = trust.verify(body, role);
        schema(request, "type", "id", "tenant", "object", "action", "payload", "identity", "requested-cell", "destination", "header-tenant");
        require("operation".equals(request.get("type")), "REQUEST_TYPE");
        trust.identity(request); return request;
    }

    static TenantRoutingPolicy.Decision resolve(Map<String, String> request, Map<String, String> p, long now) throws Exception {
        TenantId tenant = new TenantId(request.get("tenant")); CellId cell = new CellId(p.get("cell"));
        long generation = number(p, "generation"), sequence = number(p, "sequence");
        var placementAuthority = authority(AuthorityScope.PLACEMENT);
        var records = List.of(new SourceRecordReference("TENANT_PLACEMENT", tenant.value(), generation, generation, "lab", placementAuthority),
                new SourceRecordReference("CELL_REGISTRY", cell.value(), 1, null, "lab", authority(AuthorityScope.CELL_REGISTRY)));
        var artifact = new SignedSnapshotArtifact(UUID.fromString(p.get("snapshot")), sequence, PROFILE, DEPLOYMENT, "tenant-routing",
                "rref-binary-v1", "lab-authority", authority(AuthorityScope.SNAPSHOT_ISSUER), Instant.ofEpochSecond(1),
                Instant.ofEpochSecond(number(p,"not-before")), Instant.ofEpochSecond(number(p,"expires")), hash(encode(p)), records,
                "rref-binary-v1", "Ed25519-laboratory", "authority", "already-verified-wire-signature");
        // Input has already passed real wire signature, signer authority, strict schema and freshness checks.
        SnapshotTrustVerifier verifier = a -> new SnapshotTrustVerifier.Result(a.snapshotId(), a.semanticDigest(),
                java.util.EnumSet.allOf(SnapshotTrustVerifier.Check.class), "rref-verified-binary-adapter", null);
        var activation = new SnapshotActivationPolicy(verifier, Clock.fixed(Instant.ofEpochSecond(now), ZoneOffset.UTC))
                .activate(artifact, authority(AuthorityScope.SNAPSHOT_ACTIVATION));
        var state = new TenantRoutingPolicy.RoutingState(TenantRoutingPolicy.TenantRegistryStatus.ACTIVE,
                TenantRoutingPolicy.TenantLifecycleState.ACTIVE,
                new TenantRoutingPolicy.Placement(tenant, cell, generation, PROFILE, DEPLOYMENT, placementAuthority), generation, activation,
                new CellRegistryPolicy.CellRecord(cell, DEPLOYMENT, PROFILE, 1, CellRegistryPolicy.LifecycleState.valueOf(p.get("cell-state")), authority(AuthorityScope.CELL_REGISTRY)),
                new CellAvailabilityPolicy.AvailabilityRecord(cell, DEPLOYMENT, PROFILE, 1,
                        CellAvailabilityPolicy.AvailabilityState.valueOf(p.get("available")), Instant.ofEpochSecond(1),
                        Instant.ofEpochSecond(number(p,"expires")), authority(AuthorityScope.CELL_AVAILABILITY)));
        CellId requested = request.get("requested-cell").isEmpty() ? null : new CellId(request.get("requested-cell"));
        return new TenantRoutingPolicy().resolve(new TenantRoutingPolicy.ValidatedTenantContext(tenant, PROFILE, DEPLOYMENT, request.get("signer")), state, requested, Instant.ofEpochSecond(now));
    }

    static AuthorityProvenance authority(AuthorityScope scope) {
        AuthorityKind kind = switch (scope) {
            case PLACEMENT -> AuthorityKind.GLOBAL_MANAGED_PLACEMENT_AUTHORITY;
            case CELL_REGISTRY -> AuthorityKind.GLOBAL_MANAGED_CELL_REGISTRY_AUTHORITY;
            case CELL_AVAILABILITY -> AuthorityKind.DEDICATED_MANAGED_OPERATIONS_AUTHORITY;
            case SNAPSHOT_ISSUER -> AuthorityKind.GLOBAL_MANAGED_ROUTING_SNAPSHOT_ISSUER;
            case SNAPSHOT_ACTIVATION -> AuthorityKind.DEDICATED_MANAGED_ROUTING_ACTIVATION_AUTHORITY;
            default -> throw new IllegalArgumentException("Unsupported laboratory authority scope");
        };
        return new AuthorityProvenance(PROFILE, DEPLOYMENT, scope, kind, "lab-" + scope, 1);
    }

    @FunctionalInterface interface Handler { Map<String, String> run(String path, byte[] body) throws Exception; }
    static void serve(LabRoutingTrust trust, Handler handler) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 32);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/", exchange -> {
            try {
                require("POST".equals(exchange.getRequestMethod()) && exchange.getRequestURI().getRawQuery() == null, "HTTP_SHAPE");
                for (String key : List.of("X-Tenant", "X-Cell", "X-Subject", "X-Generation", "X-Destination")) {
                    require(exchange.getRequestHeaders().get(key) == null, "FORGED_HEADER");
                }
                byte[] body = exchange.getRequestBody().readNBytes(524289);
                var result = handler.run(exchange.getRequestURI().getPath(), body);
                if (result.size() == 1 && result.containsKey("redirect-to")) {
                    exchange.getResponseHeaders().set("Location", result.get("redirect-to"));
                    exchange.sendResponseHeaders(307, -1); exchange.close(); return;
                }
                reply(exchange, result.size() == 1 && result.containsKey("raw-packet")
                        ? unpack(result.get("raw-packet")) : trust.sign(result));
            } catch (Exception failure) {
                try { reply(exchange, trust.sign(fields("code", failure instanceof Denied || failure instanceof RoutingContractException ? "DENIED" : "UNAVAILABLE", "reason", safe(failure)))); }
                catch (Exception ignored) { exchange.close(); }
            }
        });
        server.start(); System.out.println("READY " + trust.role + " " + trust.run);
    }
    private static void reply(HttpExchange exchange, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Cache-Control", "no-store"); exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }
    static String safe(Exception failure) { return failure instanceof Denied ? failure.getMessage() : failure.getClass().getSimpleName(); }
    static long number(Map<String, String> map, String key) { return Long.parseLong(map.get(key)); }
    static String pack(byte[] bytes) { return Base64.getEncoder().encodeToString(bytes); }
    static byte[] unpack(String value) { return Base64.getDecoder().decode(value); }
    static String hash(byte[] bytes) throws Exception { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)); }
    static void require(boolean condition, String reason) { if (!condition) { throw new Denied(reason); } }
    static final class Denied extends RuntimeException {
        private static final long serialVersionUID = 1L;
        Denied(String reason) { super(reason); }
    }
    private LabRoutingTrust() { throw new AssertionError(); }
}
