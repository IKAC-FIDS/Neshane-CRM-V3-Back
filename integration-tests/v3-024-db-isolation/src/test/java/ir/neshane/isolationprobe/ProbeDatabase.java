package ir.neshane.isolationprobe;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/** Real PostgreSQL probe only. No production identity or application integration. */
final class ProbeDatabase {
    static final String DATABASE = "v3024_probe_a";
    static final String A = "11111111-1111-1111-1111-111111111111";
    static final String B = "22222222-2222-2222-2222-222222222222";
    static final String LOGIN_A = "v3024_tenant_a_login";
    static final String LOGIN_B = "v3024_tenant_b_login";
    static final String RUNTIME = "v3024_runtime_login";
    static final String CELL_A_RUNTIME = "v3024_db12_runtime_a";
    static final String CELL_B_RUNTIME = "v3024_db12_runtime_b";
    static final String CELL_A_MIGRATION = "v3024_db12_migration_a";
    static final String CELL_B_MIGRATION = "v3024_db12_migration_b";
    static final long FIXTURE_ID = -302400001L;
    private final Map<String, String> credentials = new LinkedHashMap<>();
    String testId = "preflight";
    int repetition;

    ProbeDatabase() throws Exception {
        try (var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            for (String line; (line = input.readLine()) != null;) {
                int separator = line.indexOf(':');
                if (separator < 1) {
                    throw new IllegalArgumentException("Invalid credential input framing");
                }
                credentials.put(line.substring(0, separator), new String(
                        Base64.getDecoder().decode(line.substring(separator + 1)), StandardCharsets.UTF_8));
            }
        }
        for (String name : List.of("postgres", LOGIN_A, LOGIN_B, RUNTIME)) {
            if (!credentials.containsKey(name)) {
                throw new IllegalArgumentException("Required test credential missing: " + name);
            }
        }
        Class.forName("org.postgresql.Driver");
    }

    Connection connect(String user) throws SQLException {
        return connect(user, DATABASE);
    }

    Connection connect(String user, String database) throws SQLException {
        return authenticate(user, credentials.get(user), database);
    }

    Connection authenticate(String user, String password, String database) throws SQLException {
        return authenticateHost(user, password, database, "127.0.0.1");
    }

    Connection connectHost(String user, String database, String host) throws SQLException {
        return authenticateHost(user, Objects.requireNonNull(credentials.get(user), "Test credential missing"), database, host);
    }

    Connection authenticateHost(String user, String password, String database, String host) throws SQLException {
        if (!List.of(DATABASE, "v3024_probe_b", "v3024_control_a").contains(database)) {
            throw new IllegalArgumentException("Database outside probe allowlist");
        }
        if (!List.of("127.0.0.1", "::1").contains(host)) {
            throw new IllegalArgumentException("Only actual TCP loopback authentication probes are allowed");
        }
        var properties = new Properties();
        properties.setProperty("user", user);
        // Null is intentional only for the missing-password authentication negative.
        // No password provider, environment or alternate-principal fallback is used.
        if (password != null) {
            properties.setProperty("password", password);
        }
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "10");
        properties.setProperty("ApplicationName", "v3024-formal-isolation");
        try {
            Connection connection = DriverManager.getConnection(
                    "jdbc:postgresql://" + (host.equals("::1") ? "[::1]" : host) + ":5432/" + database, properties);
            emit(Map.of("type", "connection", "id", testId, "repeat", repetition,
                    "principal", user, "database", database, "host", host, "sqlstate", "00000"));
            return connection;
        } catch (SQLException exception) {
            emit(Map.of("type", "connection", "id", testId, "repeat", repetition,
                    "principal", user, "database", database, "host", host, "sqlstate", state(exception)));
            throw exception;
        }
    }

    HikariDataSource pool(String user) {
        return pool(user, DATABASE);
    }

    HikariDataSource pool(String user, String targetDatabase) {
        if (!List.of(DATABASE, "v3024_probe_b", "v3024_control_a").contains(targetDatabase)) {
            throw new IllegalArgumentException("Pool database outside probe allowlist");
        }
        var configuration = new HikariConfig();
        configuration.setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/" + targetDatabase);
        configuration.setUsername(user);
        configuration.setPassword(credentials.get(user));
        configuration.setMaximumPoolSize(1);
        configuration.setMinimumIdle(1);
        configuration.setConnectionTimeout(5000);
        configuration.setValidationTimeout(2000);
        configuration.setPoolName("v3024-formal-" + user);
        configuration.addDataSourceProperty("connectTimeout", "5");
        configuration.addDataSourceProperty("socketTimeout", "10");
        configuration.addDataSourceProperty("ApplicationName", "v3024-formal-isolation");
        return new HikariDataSource(configuration);
    }

    /** Disposable service-owned authorization authority, not the legacy Tenant mapping. */
    MembershipFixture membershipFixture() throws Exception {
        return new MembershipFixture();
    }

    final class MembershipFixture implements AutoCloseable {
        final String schema = "v3024_db13_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        private final boolean[] created = new boolean[2];
        private final String[] databases = { DATABASE, "v3024_probe_b" };

        MembershipFixture() throws Exception {
            this(false);
        }

        MembershipFixture(boolean failAfterFirstCell) throws Exception {
            try {
                setup(0);
                if (failAfterFirstCell) { throw new IllegalStateException("DB13_SETUP_FAULT"); }
                setup(1);
            } catch (Exception | Error failure) {
                try { close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
                throw failure;
            }
        }

        String database(int cell) { return databases[cell]; }
        String cell(int cell) { return cell == 0 ? "cell-a" : "cell-b"; }
        String owner(int cell) { return schema + (cell == 0 ? "_oa" : "_ob"); }
        String guard(int cell) { return schema + (cell == 0 ? "_ga" : "_gb"); }

        private void setup(int cell) throws Exception {
            String owner = owner(cell);
            String guard = guard(cell);
            String callers = cell == 0 ? LOGIN_A + "," + LOGIN_B + "," + RUNTIME + "," + CELL_A_RUNTIME : CELL_B_RUNTIME;
            try (Connection c = connect("postgres", database(cell))) {
                equal(scalar(c, "SELECT count(*) FROM pg_namespace WHERE nspname=?", schema), "0", "Unique DB13 schema absent");
                equal(scalar(c, "SELECT count(*) FROM pg_roles WHERE rolname IN (?,?)", owner, guard), "0", "Unique DB13 roles absent");
                c.setAutoCommit(false);
                try {
                    for (String role : List.of(owner, guard)) {
                        update(c, "CREATE ROLE " + role + " NOLOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION");
                    }
                    update(c, "CREATE SCHEMA " + schema + " AUTHORIZATION " + owner);
                    update(c, "SET LOCAL ROLE " + owner);
                    update(c, "CREATE TABLE " + schema + ".source(id boolean PRIMARY KEY CHECK(id), available boolean NOT NULL, head bigint NOT NULL, applied bigint NOT NULL, conflict boolean NOT NULL, clock bigint NOT NULL, cell text NOT NULL, placement bigint NOT NULL)");
                    update(c, "INSERT INTO " + schema + ".source VALUES(true,true,1,1,false,100,?,1)", cell(cell));
                    update(c, "CREATE TABLE " + schema + ".fence(id boolean PRIMARY KEY CHECK(id),tick bigint NOT NULL)");
                    update(c, "INSERT INTO " + schema + ".fence VALUES(true,1)");
                    update(c, "CREATE TABLE " + schema + ".watermark(id boolean PRIMARY KEY CHECK(id),head bigint NOT NULL)");
                    update(c, "INSERT INTO " + schema + ".watermark VALUES(true,1)");
                    update(c, "CREATE TABLE " + schema + ".members(principal name PRIMARY KEY, tenant uuid NOT NULL, status text NOT NULL, epoch bigint NOT NULL, valid_until bigint NOT NULL, user_enabled boolean NOT NULL, tenant_enabled boolean NOT NULL, policy_enabled boolean NOT NULL, operation text)");
                    if (cell == 0) {
                        for (String principal : List.of(LOGIN_A, LOGIN_B, CELL_A_RUNTIME)) {
                            update(c, "INSERT INTO " + schema + ".members VALUES(?,?::uuid,'ACTIVE',1,200,true,true,true,NULL)", principal, principal.equals(LOGIN_B) ? B : A);
                        }
                    } else {
                        update(c, "INSERT INTO " + schema + ".members VALUES(?,?::uuid,'ACTIVE',1,200,true,true,true,NULL)", CELL_B_RUNTIME, B);
                    }
                    update(c, "CREATE TABLE " + schema + ".membership_heads(principal name PRIMARY KEY,epoch bigint NOT NULL)");
                    update(c, "INSERT INTO " + schema + ".membership_heads SELECT principal,epoch FROM " + schema + ".members");
                    update(c, "CREATE TABLE " + schema + ".leases(pid integer NOT NULL, xid bigint NOT NULL, principal name NOT NULL, tenant uuid NOT NULL, epoch bigint NOT NULL, PRIMARY KEY(pid,xid))");
                    update(c, "CREATE TABLE " + schema + ".records(id integer PRIMARY KEY, tenant uuid NOT NULL, payload text NOT NULL)");
                    update(c, "INSERT INTO " + schema + ".records VALUES(1,?::uuid,'a'),(2,?::uuid,'b')", A, B);
                    update(c, "CREATE TABLE " + schema + ".operations(id text PRIMARY KEY, phase text NOT NULL, targets text NOT NULL)");
                    update(c, "RESET ROLE");
                    update(c, "GRANT USAGE ON SCHEMA " + schema + " TO " + guard + "," + callers);
                    // Locking SELECT requires UPDATE: limited to separate fixture fences, never legacy mapping.
                    update(c, "GRANT SELECT ON " + schema + ".source," + schema + ".members TO " + guard);
                    update(c, "GRANT SELECT ON " + schema + ".watermark," + schema + ".membership_heads TO " + guard);
                    update(c, "GRANT SELECT,UPDATE(tick) ON " + schema + ".fence TO " + guard);
                    update(c, "GRANT SELECT,INSERT,DELETE ON " + schema + ".leases TO " + guard);
                    update(c, "GRANT SELECT,INSERT,UPDATE,DELETE ON " + schema + ".records TO " + callers);
                    String evidence = """
                            CREATE FUNCTION %1$s.evidence() RETURNS text LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $body$
                            DECLARE s %1$s.source%%ROWTYPE; m %1$s.members%%ROWTYPE; token bigint;
                            BEGIN
                              IF current_setting('transaction_isolation') <> 'read committed' THEN
                                RAISE EXCEPTION USING ERRCODE='0A000',MESSAGE='AUTHZ_ISOLATION_UNSUPPORTED'; END IF;
                              BEGIN
                                SELECT tick INTO token FROM %1$s.fence WHERE id FOR SHARE;
                                IF NOT FOUND THEN RAISE EXCEPTION USING ERRCODE='55000',MESSAGE='AUTHZ_FRESHNESS_UNAVAILABLE'; END IF;
                                SELECT * INTO s FROM %1$s.source WHERE id;
                              EXCEPTION WHEN insufficient_privilege OR undefined_table THEN
                                RAISE EXCEPTION USING ERRCODE='55000',MESSAGE='AUTHZ_FRESHNESS_UNAVAILABLE';
                              END;
                              IF NOT FOUND OR NOT s.available OR s.head<>s.applied OR s.conflict THEN
                                RAISE EXCEPTION USING ERRCODE='55000',MESSAGE='AUTHZ_FRESHNESS_UNAVAILABLE'; END IF;
                              IF NOT EXISTS (SELECT 1 FROM %1$s.watermark WHERE id) OR s.head < (SELECT head FROM %1$s.watermark WHERE id) THEN
                                RAISE EXCEPTION USING ERRCODE='55000',MESSAGE='AUTHZ_FRESHNESS_UNAVAILABLE'; END IF;
                              SELECT * INTO m FROM %1$s.members WHERE principal=SESSION_USER;
                              IF NOT FOUND OR m.status<>'ACTIVE' OR NOT m.user_enabled OR NOT m.tenant_enabled OR NOT m.policy_enabled OR s.clock>=m.valid_until THEN
                                RAISE EXCEPTION USING ERRCODE='42501',MESSAGE='AUTHZ_DENIED'; END IF;
                              IF NOT EXISTS (SELECT 1 FROM %1$s.membership_heads WHERE principal=SESSION_USER) OR m.epoch < (SELECT epoch FROM %1$s.membership_heads WHERE principal=SESSION_USER) THEN
                                RAISE EXCEPTION USING ERRCODE='55000',MESSAGE='AUTHZ_FRESHNESS_UNAVAILABLE'; END IF;
                              RETURN m.epoch::text||'|'||s.applied::text||'|'||m.tenant::text||'|'||s.cell||'|'||s.placement::text;
                            END $body$
                            """.formatted(schema);
                    update(c, evidence);
                    update(c, """
                            CREATE FUNCTION %1$s.admit(me bigint,se bigint,t uuid,c text,p bigint) RETURNS boolean LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $body$
                            DECLARE e text;
                            BEGIN
                              e := %1$s.evidence();
                              IF e IS DISTINCT FROM me::text||'|'||se::text||'|'||t::text||'|'||c||'|'||p::text THEN
                                RAISE EXCEPTION USING ERRCODE='42501',MESSAGE='AUTHZ_CONTEXT_INVALID'; END IF;
                              DELETE FROM %1$s.leases WHERE pid=pg_backend_pid();
                              INSERT INTO %1$s.leases VALUES(pg_backend_pid(),txid_current(),SESSION_USER,t,me);
                              RETURN true;
                            END $body$
                            """.formatted(schema));
                    update(c, """
                            CREATE FUNCTION %1$s.allowed(t uuid) RETURNS boolean LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $body$
                            DECLARE own uuid;
                            BEGIN
                              SELECT tenant INTO own FROM %1$s.leases WHERE pid=pg_backend_pid() AND xid=txid_current() AND principal=SESSION_USER;
                              IF NOT FOUND THEN RAISE EXCEPTION USING ERRCODE='42501',MESSAGE='AUTHZ_CONTEXT_INVALID'; END IF;
                              RETURN own=t;
                            END $body$
                            """.formatted(schema));
                    for (String signature : List.of("evidence()", "admit(bigint,bigint,uuid,text,bigint)", "allowed(uuid)")) {
                        update(c, "ALTER FUNCTION " + schema + "." + signature + " OWNER TO " + guard);
                        update(c, "REVOKE ALL ON FUNCTION " + schema + "." + signature + " FROM PUBLIC");
                        update(c, "GRANT EXECUTE ON FUNCTION " + schema + "." + signature + " TO " + callers);
                    }
                    update(c, "ALTER TABLE " + schema + ".records ENABLE ROW LEVEL SECURITY");
                    update(c, "ALTER TABLE " + schema + ".records FORCE ROW LEVEL SECURITY");
                    update(c, "CREATE POLICY db13_scope ON " + schema + ".records USING (" + schema + ".allowed(tenant)) WITH CHECK (" + schema + ".allowed(tenant))");
                    c.commit();
                    created[cell] = true;
                    emit(Map.of("type", "db13-fixture", "id", "DB13", "repeat", repetition, "schema", schema, "cell", cell(cell), "stage", "CREATED", "owner", owner, "guard", guard));
                } catch (Exception | Error failure) {
                    c.rollback();
                    throw failure;
                }
            }
        }

        void mutate(int cell, String sql, Object... args) throws Exception {
            require(sql.startsWith("UPDATE " + schema + ".") || sql.startsWith("INSERT INTO " + schema + "."), "Fixture mutation boundary");
            try (Connection c = connect("postgres", database(cell))) {
                c.setAutoCommit(false);
                try {
                    update(c, "SET LOCAL ROLE " + owner(cell));
                    equal(update(c, "UPDATE " + schema + ".fence SET tick=tick+1 WHERE id"), 1, "Authority takes incompatible local fence");
                    require(update(c, sql, args) > 0, "Authority mutation changed expected fixture rows");
                    update(c, "UPDATE " + schema + ".watermark SET head=greatest(head,(SELECT head FROM " + schema + ".source WHERE id))");
                    update(c, "UPDATE " + schema + ".membership_heads h SET epoch=greatest(h.epoch,m.epoch) FROM " + schema + ".members m WHERE h.principal=m.principal");
                    c.commit();
                } catch (Exception | Error failure) { c.rollback(); throw failure; }
            }
        }

        String fingerprint(int cell) throws Exception {
            try (Connection c = connect("postgres", database(cell))) {
                return scalar(c, "SELECT jsonb_agg(to_jsonb(r) ORDER BY id)::text FROM " + schema + ".records r");
            }
        }

        void revoke(int cell, String principal, String operation) throws Exception {
            mutate(cell, "UPDATE " + schema + ".members SET status='REVOKED',epoch=epoch+1,operation=? WHERE principal=?", operation, principal);
            emit(Map.of("type", "revocation", "id", "DB13", "repeat", repetition, "phase", "LOCALLY_EFFECTIVE", "operation", operation, "cell", cell(cell)));
        }

        void request(String operation) throws Exception {
            mutate(0, "INSERT INTO " + schema + ".operations VALUES(?,'REQUESTED','cell-a,cell-b')", operation);
            emit(Map.of("type", "revocation", "id", "DB13", "repeat", repetition, "phase", "REQUESTED", "operation", operation));
        }

        boolean complete(String operation) throws Exception {
            // Harness coordinates separate local authority reads, not cross-service SQL or distributed commit.
            for (int cell = 0; cell < 2; cell++) {
                try (Connection c = connect("postgres", database(cell))) {
                    if (!scalar(c, "SELECT count(*) FROM " + schema + ".members WHERE principal=? AND status='REVOKED' AND operation=?",
                            cell == 0 ? LOGIN_B : CELL_B_RUNTIME, operation).equals("1")) {
                        emit(Map.of("type", "revocation", "id", "DB13", "repeat", repetition, "phase", "PENDING_NOT_GLOBAL", "operation", operation));
                        return false;
                    }
                }
            }
            mutate(0, "UPDATE " + schema + ".operations SET phase='GLOBAL_COMPLETE' WHERE id=?", operation);
            emit(Map.of("type", "revocation", "id", "DB13", "repeat", repetition, "phase", "GLOBAL_COMPLETE", "operation", operation));
            return true;
        }

        @Override
        public void close() throws SQLException {
            SQLException failure = null;
            for (int cell = 1; cell >= 0; cell--) {
                if (!created[cell]) { continue; }
                try (Connection c = connect("postgres", database(cell))) {
                    update(c, "DROP SCHEMA " + schema + " CASCADE");
                    update(c, "DROP ROLE " + guard(cell));
                    update(c, "DROP ROLE " + owner(cell));
                    equal(scalar(c, "SELECT count(*) FROM pg_namespace WHERE nspname=?", schema), "0", "Owned schema removed");
                    equal(scalar(c, "SELECT count(*) FROM pg_roles WHERE rolname IN (?,?)", owner(cell), guard(cell)), "0", "Owned roles removed");
                    created[cell] = false;
                    emit(Map.of("type", "db13-fixture", "id", "DB13", "repeat", repetition, "schema", schema, "cell", cell(cell), "stage", "REMOVED"));
                } catch (SQLException cleanup) {
                    if (failure == null) { failure = cleanup; } else { failure.addSuppressed(cleanup); }
                }
            }
            if (failure != null) { throw failure; }
        }
    }

    String scalar(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql, parameters);
                ResultSet result = statement.executeQuery()) {
            require(result.next(), "Expected one SQL result row");
            String value = result.getString(1);
            require(!result.next(), "Expected exactly one SQL result row");
            sql(sql, "00000", 1);
            return value;
        } catch (SQLException exception) {
            sql(sql, state(exception), 0);
            throw exception;
        }
    }

    int update(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql, parameters)) {
            int rows = statement.executeUpdate();
            sql(sql, "00000", rows);
            return rows;
        } catch (SQLException exception) {
            sql(sql, state(exception), 0);
            throw exception;
        }
    }

    List<Map<String, String>> inventory(Connection connection, String sql) throws SQLException {
        var rows = new ArrayList<Map<String, String>>();
        try (PreparedStatement statement = prepare(connection, sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                var row = new LinkedHashMap<String, String>();
                for (int i = 1; i <= result.getMetaData().getColumnCount(); i++) {
                    row.put(result.getMetaData().getColumnLabel(i), result.getString(i));
                }
                rows.add(row);
            }
        }
        emit(Map.of("type", "inventory", "id", testId, "repeat", repetition, "sql", sql, "rows", rows));
        return rows;
    }

    void denied(Connection connection, String sql, String expectedState, Object... parameters) throws SQLException {
        Savepoint savepoint = connection.getAutoCommit() ? null : connection.setSavepoint();
        SQLException rejection = null;
        boolean accepted;
        try (PreparedStatement statement = prepare(connection, sql, parameters)) {
            statement.execute();
            accepted = true;
            sql(sql, "00000", statement.getUpdateCount());
        } catch (SQLException exception) {
            accepted = false;
            rejection = exception;
            sql(sql, state(exception), 0);
        } finally {
            if (savepoint != null) {
                connection.rollback(savepoint);
                connection.releaseSavepoint(savepoint);
            }
        }
        require(!accepted, "Unauthorized SQL was accepted: " + sql);
        equal(state(Objects.requireNonNull(rejection)), expectedState, "Expected PostgreSQL denial SQLSTATE");
    }

    String dataFingerprint() throws Exception {
        try (Connection administrator = connect("postgres")) {
            String records = scalar(administrator, "SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY id)::text,'[]') "
                    + "FROM isolation.tenant_records t");
            String principals = scalar(administrator,
                    "SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY principal_name)::text,'[]') "
                            + "FROM isolation.tenant_principals t");
            return hex(MessageDigest.getInstance("SHA-256").digest(
                    (records + "\n" + principals).getBytes(StandardCharsets.UTF_8)));
        }
    }

    void secretSql(Connection connection, String role, String password) throws SQLException {
        // Only the task-owned role is allowed; password never enters logs, argv or source.
        require(List.of("v3024_formal_revocation", CELL_A_RUNTIME, CELL_B_RUNTIME,
                CELL_A_MIGRATION, CELL_B_MIGRATION).contains(role), "Task-owned login role allowlist");
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT format('CREATE ROLE %I LOGIN NOINHERIT NOSUPERUSER NOBYPASSRLS "
                        + "NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L', ?, ?)")) {
            statement.setString(1, role);
            statement.setString(2, password);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next(), "Role setup SQL missing");
                try (var creation = connection.createStatement()) {
                    creation.execute(result.getString(1));
                }
            }
        }
        sql("CREATE ROLE [task-owned] ... PASSWORD [REDACTED]", "00000", 0);
    }

    String credential(String user) {
        require(List.of(CELL_A_RUNTIME, CELL_B_RUNTIME, CELL_A_MIGRATION, CELL_B_MIGRATION).contains(user),
                "DB12 credential access allowlist");
        return Objects.requireNonNull(credentials.get(user), "DB12 credential unavailable");
    }

    String cellFingerprint() throws Exception {
        var values = new ArrayList<String>();
        for (String database : List.of(DATABASE, "v3024_probe_b")) {
            try (Connection administrator = connect("postgres", database)) {
                String table = scalar(administrator, "SELECT to_regclass('cell_probe.cell_records')");
                values.add(database + ":" + (table == null ? "ABSENT" : scalar(administrator,
                        "SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY record_key)::text,'[]') FROM cell_probe.cell_records t")));
            }
        }
        return hex(MessageDigest.getInstance("SHA-256").digest(String.join("\n", values).getBytes(StandardCharsets.UTF_8)));
    }

    private PreparedStatement prepare(Connection connection, String sql, Object... parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
        return statement;
    }

    private void sql(String sql, String state, int rows) {
        emit(Map.of("type", "sql", "id", testId, "repeat", repetition, "statement", sql,
                "sqlstate", state, "affectedRows", rows));
    }

    static String state(SQLException exception) {
        return exception.getSQLState() == null ? "NO_SQLSTATE" : exception.getSQLState();
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void equal(Object actual, Object expected, String message) {
        require(Objects.equals(actual, expected), message + "; expected=" + expected + "; actual=" + actual);
    }

    static String hex(byte[] bytes) {
        return java.util.HexFormat.of().formatHex(bytes);
    }

    static synchronized void emit(Map<String, ?> value) {
        System.out.println(json(value));
    }

    static String json(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream().map(e -> json(e.getKey().toString()) + ":" + json(e.getValue()))
                    .collect(java.util.stream.Collectors.joining(",", "{", "}"));
        }
        if (value instanceof Iterable<?> items) {
            var encoded = new ArrayList<String>();
            items.forEach(item -> encoded.add(json(item)));
            return String.join(",", encoded).transform(s -> "[" + s + "]");
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        var out = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 32) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.append('"').toString();
    }
}
