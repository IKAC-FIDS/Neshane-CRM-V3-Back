package ir.neshane.isolationprobe;

import static ir.neshane.isolationprobe.ProbeDatabase.*;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Standalone formal DB01-DB14 runner. FAIL and BLOCKED are never converted to PASS. */
public final class PostgresTenantIsolationTest {
    @FunctionalInterface
    private interface Check {
        void run() throws Exception;
    }

    private static final class Blocked extends Exception {
        private static final long serialVersionUID = 1L;

        Blocked(String message) {
            super(message);
        }
    }

    private final ProbeDatabase db;
    private final TenantUnitOfWork transactions;
    private final Map<String, List<String>> outcomes = new LinkedHashMap<>();
    private final Map<String, String> descriptions = new LinkedHashMap<>();

    private PostgresTenantIsolationTest(ProbeDatabase database) {
        db = database;
        transactions = new TenantUnitOfWork(db);
    }

    public static void main(String[] arguments) throws Exception {
        var suite = new PostgresTenantIsolationTest(new ProbeDatabase());
        if (arguments.length == 1 && arguments[0].equals("--authentication-only")) {
            suite.authentication();
            return;
        }
        if (arguments.length == 1 && arguments[0].equals("--setup-db12")) {
            suite.setupDb12();
            return;
        }
        if (arguments.length == 1 && List.of("--db12-only", "--db09-only", "--db07-only", "--db01-only", "--db13-only").contains(arguments[0])) {
            String id = arguments[0].equals("--db09-only") ? "DB09" : arguments[0].equals("--db07-only") ? "DB07"
                    : arguments[0].equals("--db01-only") ? "DB01" : arguments[0].equals("--db13-only") ? "DB13" : "DB12";
            for (int repeat = 1; repeat <= 2; repeat++) {
                suite.db.repetition = repeat;
                if (id.equals("DB09")) { suite.db09(); } else if (id.equals("DB07")) { suite.db07(); }
                else if (id.equals("DB01")) { suite.db01(); } else if (id.equals("DB13")) { suite.db13(); } else { suite.db12(); }
            }
            List<String> outcomes = suite.outcomes.get(id);
            String status = outcomes.contains("FAIL") ? "FAIL" : outcomes.contains("BLOCKED") ? "BLOCKED" : "PASS";
            emit(Map.of("type", "scenario", "id", id, "description", suite.descriptions.get(id),
                    "status", status, "repetitions", 2, "subcaseExecutions", outcomes.size()));
            emit(Map.of("type", "summary", "scenarios", 1, "pass", status.equals("PASS") ? 1 : 0,
                    "fail", status.equals("FAIL") ? 1 : 0, "blocked", status.equals("BLOCKED") ? 1 : 0,
                    "acceptance", "Final Runtime Tenant Isolation Acceptance: PENDING", "boundary", id + " test-only secured contract"));
            System.exit(status.equals("PASS") ? 0 : status.equals("FAIL") ? 1 : 2);
        }
        require(arguments.length == 0, "Unknown runner arguments");
        suite.execute();
    }

    private void authentication() throws Exception {
        String initial = db.dataFingerprint();
        for (int repeat = 1; repeat <= 2; repeat++) {
            db.repetition = repeat;
            scenario("AUTH01", "Correct passwords and actual IPv4/IPv6 TCP identities");
            check("Valid principal credentials", "Four supplied disposable LOGIN credentials; both TCP loopbacks",
                    "Each correct password connects as the intended SESSION_USER", () -> {
                        for (String host : List.of("127.0.0.1", "::1")) {
                            for (String user : List.of(LOGIN_A, LOGIN_B, RUNTIME, "postgres")) {
                                try (Connection c = db.connectHost(user, DATABASE, host)) {
                                    equal(db.scalar(c, "SELECT session_user"), user, "Intended authenticated identity");
                                    equal(db.scalar(c, "SELECT host(inet_server_addr())"), host, "Actual TCP, not Unix socket");
                                    equal(db.scalar(c, "SELECT 1"), "1", "Healthy TCP positive control");
                                }
                            }
                        }
                    });
            scenario("AUTH02", "Incorrect password rejection");
            check("Bad passwords denied on both TCP loopbacks", "Correct A login succeeds immediately before each negative",
                    "Incorrect password rejected with server SQLSTATE 28P01", () -> {
                        for (String host : List.of("127.0.0.1", "::1")) {
                            tcpPositive(host);
                            tcpDenied(LOGIN_A, "intentionally-invalid-v3024-auth-password", DATABASE, host, "28P01");
                        }
                    });
            scenario("AUTH03", "Missing password rejection");
            check("No password supplied", "Correct A login positive; password property omitted, no client fallback",
                    "SCRAM challenge rejected by JDBC with 08004; no authenticated session", () -> {
                        for (String host : List.of("127.0.0.1", "::1")) {
                            tcpPositive(host);
                            tcpDenied(LOGIN_A, null, DATABASE, host, "08004");
                        }
                    });
            scenario("AUTH04", "Unknown login rejection");
            check("Unknown principal cannot authenticate", "Known valid A credential positive; unknown role confirmed absent",
                    "Unknown role rejected with 28P01 on both loopbacks", () -> {
                        try (Connection admin = db.connect("postgres")) {
                            equal(db.scalar(admin, "SELECT count(*) FROM pg_roles WHERE rolname='v3024_formal_unknown_login'"), "0", "Unknown-role precondition");
                        }
                        for (String host : List.of("127.0.0.1", "::1")) {
                            tcpPositive(host);
                            tcpDenied("v3024_formal_unknown_login", "intentionally-invalid-v3024-auth-password", DATABASE, host, "28P01");
                        }
                    });
            scenario("AUTH05", "NOLOGIN despite correct SCRAM credential");
            check("Task-owned role positive then NOLOGIN negative", "Only fresh disposable role; known password; role/grant removed in finally",
                    "Correct password succeeds before NOLOGIN and fails with 28000 afterwards on IPv4/IPv6", () -> {
                        String role = "v3024_formal_revocation";
                        byte[] entropy = new byte[32];
                        new SecureRandom().nextBytes(entropy);
                        String password = Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
                        try (Connection admin = db.connect("postgres")) {
                            if (!db.scalar(admin, "SELECT count(*) FROM pg_roles WHERE rolname=?", role).equals("0")) {
                                throw new Blocked("Disposable role already exists; will not reuse or alter it");
                            }
                            boolean created = false;
                            try {
                                db.secretSql(admin, role, password);
                                created = true;
                                db.update(admin, "GRANT CONNECT ON DATABASE v3024_probe_a TO v3024_formal_revocation");
                                for (String host : List.of("127.0.0.1", "::1")) {
                                    try (Connection c = db.authenticateHost(role, password, DATABASE, host)) {
                                        equal(db.scalar(c, "SELECT session_user"), role, "Task-owned role positive before NOLOGIN");
                                    }
                                }
                                db.update(admin, "ALTER ROLE v3024_formal_revocation NOLOGIN");
                                for (String host : List.of("127.0.0.1", "::1")) {
                                    tcpDenied(role, password, DATABASE, host, "28000");
                                }
                            } finally {
                                if (created) {
                                    db.inventory(admin, "SELECT pg_terminate_backend(pid) AS terminated FROM pg_stat_activity WHERE usename='v3024_formal_revocation'");
                                    db.update(admin, "REVOKE CONNECT ON DATABASE v3024_probe_a FROM v3024_formal_revocation");
                                    db.update(admin, "DROP ROLE v3024_formal_revocation");
                                }
                            }
                        }
                    });
            scenario("AUTH06", "Runtime cannot access control database");
            check("Authenticated runtime control DB denial", "Correct runtime credential and positive SELECT 1 on own database",
                    "Control DB connection denied with 42501 on both loopbacks", () -> {
                        for (String host : List.of("127.0.0.1", "::1")) {
                            try (Connection c = db.connectHost(RUNTIME, DATABASE, host)) {
                                equal(db.scalar(c, "SELECT session_user"), RUNTIME, "Runtime positive identity");
                                equal(db.scalar(c, "SELECT 1"), "1", "Runtime positive connection");
                            }
                            // Password remains private; connectHost uses the original stored credential.
                            try (Connection unexpected = db.connectHost(RUNTIME, "v3024_control_a", host)) {
                                throw new AssertionError("Control DB CONNECT accepted; autoCommit=" + unexpected.getAutoCommit());
                            } catch (SQLException expected) {
                                equal(state(expected), "42501", "Authenticated runtime permission denial");
                            }
                        }
                    });
            scenario("AUTH07", "Tenant identity binding after SCRAM authentication");
            check("A/B retain intended SESSION_USER mapping and row visibility", "Separate actual TCP logins, both loopbacks",
                    "Each authenticated Tenant reads own data and zero other-Tenant rows", () -> {
                        for (String host : List.of("127.0.0.1", "::1")) {
                            try (Connection a = db.connectHost(LOGIN_A, DATABASE, host);
                                    Connection b = db.connectHost(LOGIN_B, DATABASE, host)) {
                                visibility(a, LOGIN_A, A);
                                visibility(b, LOGIN_B, B);
                            }
                        }
                    });
            scenario("AUTH08", "HBA ordering and SCRAM verifier enforcement");
            check("No non-SCRAM TCP rules or parser errors", "Administrator uses TCP; inventories contain verifier types only, never hashes",
                    "All host rules SCRAM, no errors, password_encryption SCRAM; existing LOGIN roles compatible", () -> {
                        try (Connection admin = db.connect("postgres")) {
                            equal(db.scalar(admin, "SHOW password_encryption"), "scram-sha-256", "New password encoding");
                            equal(db.scalar(admin, "SELECT count(*) FROM pg_hba_file_rules WHERE error IS NOT NULL"), "0", "HBA syntax valid");
                            equal(db.scalar(admin, "SELECT count(*) FROM pg_hba_file_rules WHERE type LIKE 'host%' AND auth_method<>'scram-sha-256'"), "0", "No TCP trust override");
                            equal(db.scalar(admin, "SELECT count(*) FROM pg_authid WHERE rolcanlogin AND (rolname LIKE 'v3024%' OR rolname='postgres') AND (rolpassword IS NULL OR rolpassword NOT LIKE 'SCRAM-SHA-256$%')"), "0", "Compatible stored verifier types");
                            db.inventory(admin, "SELECT line_number,type,database,user_name,address,netmask,auth_method,error FROM pg_hba_file_rules ORDER BY line_number");
                        }
                    });
        }
        equal(db.dataFingerprint(), initial, "Authentication-only protected data/mapping preservation");
        int passed = 0;
        int failed = 0;
        int blocked = 0;
        for (var entry : outcomes.entrySet()) {
            String status = entry.getValue().contains("FAIL") ? "FAIL"
                    : entry.getValue().contains("BLOCKED") ? "BLOCKED" : "PASS";
            passed += status.equals("PASS") ? 1 : 0;
            failed += status.equals("FAIL") ? 1 : 0;
            blocked += status.equals("BLOCKED") ? 1 : 0;
            emit(Map.of("type", "scenario", "id", entry.getKey(), "description", descriptions.get(entry.getKey()),
                    "status", status, "repetitions", 2, "subcaseExecutions", entry.getValue().size()));
        }
        emit(Map.of("type", "summary", "scenarios", 8, "pass", passed, "fail", failed, "blocked", blocked,
                "acceptance", "Final Runtime Tenant Isolation Acceptance: PENDING", "boundary", "SCRAM authentication-only TCP verification"));
        System.exit(failed > 0 ? 1 : blocked > 0 ? 2 : 0);
    }

    private void tcpPositive(String host) throws SQLException {
        try (Connection c = db.connectHost(LOGIN_A, DATABASE, host)) {
            equal(db.scalar(c, "SELECT session_user"), LOGIN_A, "Matching TCP positive control");
        }
    }

    private void tcpDenied(String user, String password, String database, String host, String expectedState) throws SQLException {
        try (Connection unexpected = db.authenticateHost(user, password, database, host)) {
            throw new AssertionError("Authentication negative unexpectedly accepted; autoCommit=" + unexpected.getAutoCommit());
        } catch (SQLException expected) {
            equal(state(expected), expectedState, "TCP authentication denial");
        }
    }

    private void execute() throws Exception {
        try (Connection administrator = db.connect("postgres")) {
            equal(db.scalar(administrator, "SELECT current_setting('server_version_num')"), "170012",
                    "Existing PostgreSQL version");
            equal(db.scalar(administrator, "SELECT count(*) FROM isolation.tenant_records WHERE id BETWEEN -302400099 AND -302400001"),
                    "0", "Reserved rollback-only fixture IDs must be absent");
            emit(Map.of("type", "runtime", "java", System.getProperty("java.runtime.version"),
                    "jdbc", administrator.getMetaData().getDriverVersion(),
                    "hikari", "7.0.2 (classpath SHA-256 pinned by wrapper)",
                    "postgresql", "17.12 / 170012", "boundary", "REAL POSTGRESQL PROBE ONLY"));
            db.inventory(administrator, "SELECT rolname,rolsuper,rolinherit,rolcreaterole,rolcreatedb,rolcanlogin,rolreplication,rolbypassrls "
                    + "FROM pg_roles WHERE rolname LIKE 'v3024%' OR rolname='postgres' ORDER BY rolname");
            db.inventory(administrator, "SELECT pg_get_userbyid(roleid) AS granted,pg_get_userbyid(member) AS member,"
                    + "admin_option,inherit_option,set_option FROM pg_auth_members WHERE pg_get_userbyid(member) LIKE 'v3024%' ORDER BY 1,2");
            db.inventory(administrator, "SELECT datname,pg_get_userbyid(datdba) AS owner,datacl FROM pg_database WHERE datname LIKE 'v3024%' ORDER BY datname");
            db.inventory(administrator, "SELECT nspname,pg_get_userbyid(nspowner) AS owner,nspacl FROM pg_namespace WHERE nspname IN ('isolation','public') ORDER BY nspname");
            db.inventory(administrator, "SELECT relname,pg_get_userbyid(relowner) AS owner,relrowsecurity,relforcerowsecurity,relacl "
                    + "FROM pg_class WHERE relnamespace='isolation'::regnamespace ORDER BY relname");
            db.inventory(administrator, "SELECT * FROM pg_policies WHERE schemaname='isolation' ORDER BY tablename,policyname");
            db.inventory(administrator, "SELECT pg_get_userbyid(proowner) AS owner,prosecdef,proconfig,proacl,pg_get_functiondef(oid) "
                    + "FROM pg_proc WHERE oid='isolation.current_tenant_id()'::regprocedure");
            db.inventory(administrator, "SELECT pg_get_userbyid(defaclrole) AS creator,defaclnamespace::regnamespace,defaclobjtype,defaclacl FROM pg_default_acl ORDER BY 1,2,3");
            db.inventory(administrator, "SELECT conname,pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid IN "
                    + "('isolation.tenant_records'::regclass,'isolation.tenant_principals'::regclass) ORDER BY conname");
            db.inventory(administrator, "SELECT line_number,type,database,user_name,address,auth_method,error FROM pg_hba_file_rules ORDER BY line_number");
        }
        String initial = db.dataFingerprint();
        for (int repeat = 1; repeat <= 2; repeat++) {
            db.repetition = repeat;
            db01(); db02(); db03(); db04(); db05(); db06(); db07();
            db08(); db09(); db10(); db11(); db12(); db13(); db14();
        }
        equal(db.dataFingerprint(), initial, "Final independently observed data and principal-map preservation");
        emit(Map.of("type", "preservation", "initialDataSha256", initial, "finalDataSha256", db.dataFingerprint(),
                "existingRecordsAndMappingsUnchanged", true));
        int passed = 0;
        int failed = 0;
        int blocked = 0;
        for (var entry : outcomes.entrySet()) {
            String status = entry.getValue().contains("FAIL") ? "FAIL"
                    : entry.getValue().contains("BLOCKED") ? "BLOCKED" : "PASS";
            passed += status.equals("PASS") ? 1 : 0;
            failed += status.equals("FAIL") ? 1 : 0;
            blocked += status.equals("BLOCKED") ? 1 : 0;
            emit(Map.of("type", "scenario", "id", entry.getKey(), "description", descriptions.get(entry.getKey()),
                    "status", status, "repetitions", 2, "subcaseExecutions", entry.getValue().size()));
        }
        emit(Map.of("type", "summary", "scenarios", 14, "pass", passed, "fail", failed, "blocked", blocked,
                "acceptance", "Final Runtime Tenant Isolation Acceptance: PENDING",
                "boundary", "Separate-login PostgreSQL probe; not shared-credential Backend binding evidence"));
        System.exit(failed > 0 ? 1 : blocked > 0 ? 2 : 0);
    }

    private void scenario(String id, String description) {
        db.testId = id;
        descriptions.put(id, description);
        outcomes.computeIfAbsent(id, ignored -> new ArrayList<>());
    }

    private void check(String name, String precondition, String expected, Check action) {
        String status = "PASS";
        String actual = "Assertions satisfied";
        String sqlstate = "00000";
        String before = null;
        String cellsBefore = null;
        try {
            before = db.dataFingerprint();
            if (db.testId.equals("DB12")) {
                cellsBefore = db.cellFingerprint();
            }
            action.run();
        } catch (Blocked exception) {
            status = "BLOCKED";
            actual = exception.getMessage();
            sqlstate = "NOT_EXECUTED";
        } catch (SQLException exception) {
            status = "FAIL";
            actual = "Unexpected PostgreSQL/JDBC exception; message intentionally withheld";
            sqlstate = state(exception);
        } catch (AssertionError exception) {
            status = "FAIL";
            actual = exception.getMessage();
        } catch (Exception exception) {
            status = "FAIL";
            actual = exception.getClass().getSimpleName();
        }
        try {
            require(before != null, "Independent pre-test data snapshot unavailable");
            equal(db.dataFingerprint(), before, "Independent protected-data fingerprint changed");
            if (db.testId.equals("DB12")) {
                equal(db.cellFingerprint(), cellsBefore, "Independent Cell A/B fixture fingerprint changed");
            }
        } catch (Exception | AssertionError preservationFailure) {
            status = "FAIL";
            actual += "; independent data preservation FAILED";
        }
        outcomes.get(db.testId).add(status);
        emit(Map.of("type", "case", "id", db.testId, "repeat", db.repetition, "description", name,
                "preconditions", precondition, "expected", expected, "actual", actual, "status", status,
                "sqlstate", sqlstate));
    }

    private void visibility(Connection connection, String login, String tenant) throws SQLException {
        equal(db.scalar(connection, "SELECT session_user"), login, "Authenticated pool/connection principal");
        equal(db.scalar(connection, "SELECT isolation.current_tenant_id()::text"), tenant, "Trusted SESSION_USER mapping");
        require(Long.parseLong(db.scalar(connection, "SELECT count(*) FROM isolation.tenant_records WHERE tenant_id=?::uuid", tenant)) > 0,
                "Positive Tenant fixture must exist and be readable");
        equal(db.scalar(connection, "SELECT count(*) FROM isolation.tenant_records WHERE tenant_id<>?::uuid", tenant),
                "0", "No other-Tenant rows visible");
    }

    private void fixture(Connection connection, String tenant) throws SQLException {
        equal(db.update(connection, "INSERT INTO isolation.tenant_records(id,tenant_id,payload) OVERRIDING SYSTEM VALUE VALUES (?,?::uuid,?)",
                FIXTURE_ID, tenant, "v3024-formal-rollback-only"), 1, "Positive fixture INSERT");
    }

    private void db01() {
        scenario("DB01", "Runtime versus migration role privileges");
        check("Runtime CRUD positive and DDL/migration denials", "Actual Tenant A TCP credentials; existing grants",
                "CRUD succeeds; DDL, TRUNCATE and mapping writes receive 42501", () -> transactions.rollback(LOGIN_A, c -> {
                    visibility(c, LOGIN_A, A);
                    fixture(c, A);
                    equal(db.update(c, "UPDATE isolation.tenant_records SET payload=? WHERE id=?", "changed", FIXTURE_ID), 1, "Positive UPDATE");
                    equal(db.update(c, "DELETE FROM isolation.tenant_records WHERE id=?", FIXTURE_ID), 1, "Positive DELETE");
                    db.denied(c, "CREATE TABLE isolation.v3024_formal_forbidden(id integer)", "42501");
                    db.denied(c, "TRUNCATE isolation.tenant_records", "42501");
                    db.denied(c, "ALTER TABLE isolation.tenant_records DISABLE ROW LEVEL SECURITY", "42501");
                    db.denied(c, "UPDATE isolation.tenant_principals SET tenant_id=?::uuid WHERE principal_name=session_user", "42501", B);
                }));
        check("Migration effective identity positive", "Administrator impersonates migration identity transaction-locally; not migration-password authentication",
                "Migration identity can SET ROLE owning role and create only rollback-only fixture DDL", () -> transactions.rollback("postgres", c -> {
                    db.update(c, "SET LOCAL SESSION AUTHORIZATION v3024_migration_login");
                    equal(db.scalar(c, "SELECT session_user"), "v3024_migration_login", "Effective migration session identity");
                    db.update(c, "SET LOCAL ROLE v3024_owner");
                    db.update(c, "CREATE TABLE isolation.v3024_formal_migration(id integer)");
                    equal(db.scalar(c, "SELECT current_user"), "v3024_owner", "Explicit owner operation");
                }));
        check("Guard/owner separation", "Existing SECURITY DEFINER function and role inventory",
                "Guard function is owned by restricted v3024_guard, separately from table owner", () -> {
                    try (Connection c = db.connect("postgres")) {
                        equal(db.scalar(c, "SELECT pg_get_userbyid(proowner) FROM pg_proc WHERE oid='isolation.current_tenant_id()'::regprocedure"),
                                "v3024_guard", "Required guard/owner separation");
                    }
                });
        check("Guard effective least privilege and nonprivileged denials", "Administrator sets SESSION AUTHORIZATION to existing NOLOGIN Guard; not Guard-password authentication",
                "Only mapping SELECT and schema USAGE; no data, mapping mutation, RLS administration, CREATE, role escalation or grant option", () -> transactions.rollback("postgres", c -> {
                    equal(db.scalar(c, "SELECT count(*) FROM pg_class WHERE oid IN ('isolation.tenant_records'::regclass,'isolation.tenant_principals'::regclass) AND relowner='v3024_owner'::regrole"), "2", "Data and mapping ownership preserved");
                    equal(db.scalar(c, "SELECT (NOT rolsuper AND NOT rolcanlogin AND NOT rolbypassrls AND NOT rolcreaterole AND NOT rolcreatedb AND NOT rolreplication)::text FROM pg_roles WHERE rolname='v3024_guard'"), "true", "Restricted nonlogin Guard attributes");
                    equal(db.scalar(c, "SELECT count(*) FROM pg_database WHERE datdba='v3024_guard'::regrole"), "0", "Guard owns no database");
                    equal(db.scalar(c, "SELECT count(*) FROM pg_class WHERE relowner='v3024_guard'::regrole"), "0", "Guard owns no relations");
                    equal(db.scalar(c, "SELECT count(*) FROM pg_auth_members WHERE roleid='v3024_guard'::regrole OR member='v3024_guard'::regrole"), "0", "No incoming/outgoing Guard membership, hence no inherited or SET ROLE paths");
                    db.update(c, "SET LOCAL SESSION AUTHORIZATION v3024_guard");
                    equal(db.scalar(c, "SELECT SESSION_USER"), "v3024_guard", "Nonprivileged session principal");
                    equal(db.scalar(c, "SELECT CURRENT_USER"), "v3024_guard", "Not superuser SET ROLE with privileged session identity");
                    equal(db.scalar(c, "SELECT has_schema_privilege(session_user,'isolation','USAGE')::text"), "true", "Only required schema usage");
                    equal(db.scalar(c, "SELECT has_schema_privilege(session_user,'isolation','CREATE')::text"), "false", "No schema CREATE");
                    equal(db.scalar(c, "SELECT tenant_id::text FROM isolation.tenant_principals WHERE principal_name=?", LOGIN_A), A, "Real required mapping SELECT works");
                    equal(db.scalar(c, "SELECT tenant_id::text FROM isolation.tenant_principals WHERE principal_name=?", LOGIN_B), B, "Both mapping rows readable");
                    for (String privilege : List.of("INSERT", "UPDATE", "DELETE", "TRUNCATE", "REFERENCES", "TRIGGER", "MAINTAIN", "SELECT WITH GRANT OPTION")) {
                        equal(db.scalar(c, "SELECT has_table_privilege(session_user,'isolation.tenant_principals',?)::text", privilege), "false", "No extra mapping privilege: " + privilege);
                    }
                    for (String privilege : List.of("SELECT", "INSERT", "UPDATE", "DELETE", "TRUNCATE", "REFERENCES", "TRIGGER", "MAINTAIN")) {
                        equal(db.scalar(c, "SELECT has_table_privilege(session_user,'isolation.tenant_records',?)::text", privilege), "false", "No application data privilege: " + privilege);
                    }
                    for (String sql : List.of(
                            "INSERT INTO isolation.tenant_principals(principal_name,tenant_id) VALUES ('v3024_db01_forbidden','" + A + "'::uuid)",
                            "UPDATE isolation.tenant_principals SET tenant_id=tenant_id WHERE principal_name='" + LOGIN_A + "'",
                            "DELETE FROM isolation.tenant_principals WHERE principal_name='" + LOGIN_A + "'",
                            "TRUNCATE isolation.tenant_principals", "SELECT * FROM isolation.tenant_records",
                            "UPDATE isolation.tenant_records SET payload=payload", "DELETE FROM isolation.tenant_records",
                            "INSERT INTO isolation.tenant_records(id,tenant_id,payload) OVERRIDING SYSTEM VALUE VALUES (" + FIXTURE_ID + ",'" + A + "'::uuid,'guard-forbidden')",
                            "ALTER TABLE isolation.tenant_records DISABLE ROW LEVEL SECURITY",
                            "ALTER TABLE isolation.tenant_records NO FORCE ROW LEVEL SECURITY",
                            "ALTER TABLE isolation.tenant_records ADD COLUMN v3024_db01_forbidden integer",
                            "ALTER POLICY tenant_isolation ON isolation.tenant_records USING (true)",
                            "CREATE TABLE isolation.v3024_db01_forbidden(id integer)",
                            "CREATE FUNCTION isolation.v3024_db01_forbidden() RETURNS integer LANGUAGE sql AS 'SELECT 1'",
                            "GRANT v3024_owner TO v3024_guard", "ALTER ROLE v3024_guard BYPASSRLS")) {
                        db.denied(c, sql, "42501");
                    }
                    for (String role : List.of("v3024_owner", "v3024_migration", "v3024_runtime", "postgres", CELL_A_RUNTIME, CELL_B_RUNTIME)) {
                        db.denied(c, "SET ROLE " + role, "42501");
                    }
                    db.update(c, "SET LOCAL row_security=off");
                    db.denied(c, "SELECT * FROM isolation.tenant_records", "42501");
                    db.update(c, "SET LOCAL row_security=on");
                    // PostgreSQL may report an ineffective GRANT as warning 01007 rather than error.
                    try (var grant = c.prepareStatement("GRANT UPDATE ON isolation.tenant_principals TO v3024_guard")) {
                        grant.execute();
                        require(grant.getWarnings() != null, "Ineffective self-grant must report a warning");
                        equal(grant.getWarnings().getSQLState(), "01007", "Privilege not granted warning, not hidden successful escalation");
                        emit(Map.of("type", "classification", "id", "DB01", "repeat", db.repetition, "sqlstate", "01007", "actual", "SELF_GRANT_INEFFECTIVE"));
                    }
                    equal(db.scalar(c, "SELECT has_table_privilege(session_user,'isolation.tenant_principals','UPDATE')::text"), "false", "Self-grant did not escalate");
                }));
        check("Runtime and migration cannot assume Guard or replace function", "Nonprivileged SESSION AUTHORIZATION runtime/migration identities; actual Tenant TCP checks in adjacent cases",
                "Function ALTER/REPLACE and Guard SET ROLE denied 42501, including migration's explicit Data Owner role", () -> {
                    for (String login : List.of(LOGIN_A, LOGIN_B, RUNTIME, "v3024_migration_login")) {
                        transactions.rollback("postgres", c -> {
                            db.update(c, "SET LOCAL SESSION AUTHORIZATION " + login);
                            db.denied(c, "SET ROLE v3024_guard", "42501");
                            if (login.equals("v3024_migration_login")) { db.update(c, "SET LOCAL ROLE v3024_owner"); }
                            db.denied(c, "ALTER FUNCTION isolation.current_tenant_id() IMMUTABLE", "42501");
                            db.denied(c, "CREATE OR REPLACE FUNCTION isolation.current_tenant_id() RETURNS uuid LANGUAGE sql AS 'SELECT NULL::uuid'", "42501");
                            db.denied(c, "ALTER FUNCTION isolation.current_tenant_id() OWNER TO v3024_owner", "42501");
                        });
                    }
                });
        check("Trusted SESSION_USER binding and qualified definer lookup", "Actual A/B/unmapped TCP identities; adversarial search_path and Tenant metadata",
                "A/B resolve themselves, unmapped runtime NULL; no attacker schema CREATE; restricted definer path and no PUBLIC execution", () -> {
                    transactions.rollback("postgres", c -> {
                        equal(db.scalar(c, "SELECT prosecdef::text FROM pg_proc WHERE oid='isolation.current_tenant_id()'::regprocedure"), "true", "Definer retained");
                        equal(db.scalar(c, "SELECT array_to_string(proconfig,',') FROM pg_proc WHERE oid='isolation.current_tenant_id()'::regprocedure"), "search_path=pg_catalog, pg_temp", "Restricted function search_path");
                        equal(db.scalar(c, "SELECT position('isolation.tenant_principals' in prosrc)>0 AND position('SESSION_USER' in prosrc)>0 FROM pg_proc WHERE oid='isolation.current_tenant_id()'::regprocedure"), "t", "Qualified mapping and authenticated identity source retained");
                        assertDb07NoPublic(c, "isolation.current_tenant_id()");
                        // Admin supplies a real readable shadow object; runtime cannot CREATE TEMP itself.
                        db.update(c, "CREATE TEMP TABLE tenant_principals(principal_name name,tenant_id uuid)");
                        db.update(c, "INSERT INTO pg_temp.tenant_principals VALUES (?,?::uuid)", LOGIN_A, B);
                        db.update(c, "GRANT SELECT ON pg_temp.tenant_principals TO v3024_runtime");
                        db.update(c, "SET LOCAL SESSION AUTHORIZATION " + LOGIN_A);
                        db.scalar(c, "SELECT set_config('search_path','pg_temp, public, isolation',true)");
                        equal(db.scalar(c, "SELECT tenant_id::text FROM pg_temp.tenant_principals WHERE principal_name=SESSION_USER"), B, "Readable adversarial shadow actually exists");
                        equal(db.scalar(c, "SELECT isolation.current_tenant_id()::text"), A, "Qualified definer lookup ignores real shadow mapping");
                    });
                    for (String login : List.of(LOGIN_A, LOGIN_B, RUNTIME)) {
                        transactions.rollback(login, c -> {
                            db.scalar(c, "SELECT set_config('search_path','pg_temp, public, isolation',true)");
                            db.scalar(c, "SELECT set_config('app.tenant_id',?,true)", login.equals(LOGIN_A) ? B : A);
                            equal(db.scalar(c, "SELECT coalesce(isolation.current_tenant_id()::text,'NONE')"), login.equals(LOGIN_A) ? A : login.equals(LOGIN_B) ? B : "NONE", "Authenticated identity unaffected by attacker session settings");
                            db.denied(c, "CREATE TABLE public.v3024_db01_shadow(id integer)", "42501");
                            if (!login.equals(RUNTIME)) {
                                String own = login.equals(LOGIN_A) ? A : B;
                                visibility(c, login, own);
                                fixture(c, own);
                                equal(db.update(c, "UPDATE isolation.tenant_records SET payload=? WHERE id=?", "db01-positive", FIXTURE_ID), 1, "Own UPDATE under Guard-owned resolver");
                                equal(db.scalar(c, "SELECT count(*) FROM isolation.tenant_records WHERE tenant_id<>?::uuid", own), "0", "No other Tenant SELECT");
                                db.denied(c, "UPDATE isolation.tenant_records SET tenant_id=?::uuid WHERE id=?", "42501", own.equals(A) ? B : A, FIXTURE_ID);
                            }
                        });
                    }
                });
    }

    private void db02() {
        scenario("DB02", "Cross-Tenant SELECT denial");
        check("Bidirectional SELECT isolation", "Both Tenant fixtures exist; independently authenticated A/B logins",
                "Each login reads its own nonempty fixture and zero rows of the other Tenant", () -> {
                    for (String login : List.of(LOGIN_A, LOGIN_B)) {
                        try (Connection c = db.connect(login)) {
                            visibility(c, login, login.equals(LOGIN_A) ? A : B);
                        }
                    }
                });
    }

    private void db03() {
        scenario("DB03", "Cross-Tenant INSERT/UPDATE/DELETE denial");
        check("CRUD and Tenant ownership reassignment", "Both ordinary Tenant credentials; explicit IDs never consume sequences",
                "Own CRUD succeeds; cross INSERT/reassignment 42501; cross UPDATE/DELETE affect zero", () -> {
                    for (String login : List.of(LOGIN_A, LOGIN_B)) {
                        String own = login.equals(LOGIN_A) ? A : B;
                        String other = login.equals(LOGIN_A) ? B : A;
                        transactions.rollback(login, c -> {
                            visibility(c, login, own);
                            fixture(c, own);
                            db.denied(c, "INSERT INTO isolation.tenant_records(id,tenant_id,payload) OVERRIDING SYSTEM VALUE VALUES (?,?::uuid,?)",
                                    "42501", FIXTURE_ID - 1, other, "forbidden");
                            db.denied(c, "UPDATE isolation.tenant_records SET tenant_id=?::uuid WHERE id=?", "42501", other, FIXTURE_ID);
                            equal(db.update(c, "UPDATE isolation.tenant_records SET payload=? WHERE tenant_id=?::uuid", "forbidden", other), 0, "Cross UPDATE zero effects");
                            equal(db.update(c, "DELETE FROM isolation.tenant_records WHERE tenant_id=?::uuid", other), 0, "Cross DELETE zero effects");
                            equal(db.update(c, "UPDATE isolation.tenant_records SET payload=? WHERE id=?", "positive", FIXTURE_ID), 1, "Own UPDATE");
                            equal(db.update(c, "DELETE FROM isolation.tenant_records WHERE id=?", FIXTURE_ID), 1, "Own DELETE");
                        });
                    }
                });
    }

    private void db04() {
        scenario("DB04", "Effective RLS using real non-owner runtime roles");
        check("RLS positive/negative and ordinary-role attributes", "Actual runtime credentials, not SET ROLE from administrator",
                "Unfiltered SQL returns only own rows; role is non-owner/non-superuser/non-BYPASSRLS; ENABLE and FORCE true", () -> {
                    try (Connection c = db.connect(LOGIN_A)) {
                        visibility(c, LOGIN_A, A);
                        equal(db.scalar(c, "SELECT (NOT rolsuper AND NOT rolbypassrls AND NOT rolcreaterole AND NOT rolcreatedb)::text FROM pg_roles WHERE rolname=session_user"), "true", "Ordinary runtime attributes");
                        equal(db.scalar(c, "SELECT (relrowsecurity AND relforcerowsecurity AND relowner<>(SELECT oid FROM pg_roles WHERE rolname=session_user))::text FROM pg_class WHERE oid='isolation.tenant_records'::regclass"), "true", "RLS ENABLE/FORCE and non-ownership");
                    }
                });
        check("Owner subject to FORCE RLS", "Effective owner identity, no mapping and no superuser current_user",
                "Owner can query but receives zero Tenant rows", () -> transactions.rollback("postgres", c -> {
                    db.update(c, "SET LOCAL SESSION AUTHORIZATION v3024_migration_login");
                    db.update(c, "SET LOCAL ROLE v3024_owner");
                    equal(db.scalar(c, "SELECT count(*) FROM isolation.tenant_records"), "0", "FORCE RLS applies to owner");
                }));
    }

    private void db05() {
        scenario("DB05", "Owner, SUPERUSER and BYPASSRLS escalation prevention");
        check("No privileged-role/RLS bypass", "Authorized A read before adversarial operations",
                "Escalation, ALTER policy and row_security=off data access rejected with 42501", () -> transactions.rollback(LOGIN_A, c -> {
                    visibility(c, LOGIN_A, A);
                    for (String sql : List.of("ALTER ROLE v3024_tenant_a_login SUPERUSER",
                            "ALTER ROLE v3024_tenant_a_login BYPASSRLS", "GRANT v3024_owner TO v3024_tenant_a_login",
                            "ALTER TABLE isolation.tenant_records OWNER TO v3024_tenant_a_login",
                            "ALTER POLICY tenant_isolation ON isolation.tenant_records USING (true)")) {
                        db.denied(c, sql, "42501");
                    }
                    db.update(c, "SET LOCAL row_security=off");
                    db.denied(c, "SELECT * FROM isolation.tenant_records", "42501");
                }));
    }

    private void db06() {
        scenario("DB06", "Role membership and unauthorized SET ROLE denial");
        check("Membership does not grant owner/migration/admin or other Tenant", "Both actual Tenant logins have only approved CRUD inheritance",
                "All unauthorized role/session authorization paths rejected; trusted identity unchanged", () -> {
                    for (String login : List.of(LOGIN_A, LOGIN_B)) {
                        transactions.rollback(login, c -> {
                            visibility(c, login, login.equals(LOGIN_A) ? A : B);
                            for (String role : List.of("v3024_owner", "v3024_migration", "v3024_guard", "postgres",
                                    login.equals(LOGIN_A) ? LOGIN_B : LOGIN_A, "v3024_runtime")) {
                                db.denied(c, "SET ROLE " + role, "42501");
                            }
                            db.denied(c, "SET SESSION AUTHORIZATION postgres", "42501");
                            equal(db.scalar(c, "SELECT session_user"), login, "No impersonation");
                        });
                    }
                });
    }

    private void db07() {
        scenario("DB07", "PUBLIC and default-grant restrictions");
        check("Existing PUBLIC/schema restrictions", "Ordinary A positive visibility; existing catalog ACLs",
                "PUBLIC has no CONNECT/CREATE; runtime has no TEMP, schema CREATE or protected mapping read", () -> transactions.rollback(LOGIN_A, c -> {
                    visibility(c, LOGIN_A, A);
                    equal(db.scalar(c, "SELECT count(*) FROM pg_database d, LATERAL aclexplode(coalesce(d.datacl,acldefault('d',d.datdba))) a "
                            + "WHERE d.datname LIKE 'v3024%' AND a.grantee=0"), "0", "No PUBLIC database grants");
                    equal(db.scalar(c, "SELECT has_schema_privilege(session_user,'isolation','CREATE')::text"), "false", "No schema CREATE");
                    db.denied(c, "CREATE TEMP TABLE v3024_formal_temp(id integer)", "42501");
                    db.denied(c, "SELECT * FROM isolation.tenant_principals", "42501");
                }));
        check("Fresh object default privileges", "Rollback-only table/function created by existing owner; owner positive call succeeds",
                "New objects do not give runtime implicit SELECT or EXECUTE through PUBLIC", () -> transactions.rollback("postgres", c -> {
                    db.update(c, "SET LOCAL ROLE v3024_owner");
                    db.update(c, "CREATE TABLE isolation.v3024_formal_default_table(id integer)");
                    db.update(c, "CREATE FUNCTION isolation.v3024_formal_default_function() RETURNS integer LANGUAGE sql AS 'SELECT 1'");
                    equal(db.scalar(c, "SELECT isolation.v3024_formal_default_function()"), "1", "Owner positive function control");
                    db.update(c, "RESET ROLE");
                    db.update(c, "SET LOCAL SESSION AUTHORIZATION v3024_runtime_login");
                    db.denied(c, "SELECT * FROM isolation.v3024_formal_default_table", "42501");
                    db.denied(c, "SELECT isolation.v3024_formal_default_function()", "42501");
                }));
        check("Protected function ACL, identity positives and actual unauthorized login", "Existing SECURITY DEFINER function and real TCP principals",
                "No effective PUBLIC EXECUTE; fixed owner/search_path; both Tenants work, Cell login denied 42501", () -> {
                    transactions.rollback("postgres", c -> {
                        equal(db.scalar(c, "SELECT count(*) FROM pg_proc p, LATERAL aclexplode(coalesce(p.proacl,acldefault('f',p.proowner))) a "
                                + "WHERE p.pronamespace='isolation'::regnamespace AND a.grantee=0 AND a.privilege_type='EXECUTE'"), "0", "No implicit or explicit PUBLIC function execution");
                        equal(db.scalar(c, "SELECT pg_get_userbyid(proowner) FROM pg_proc WHERE oid='isolation.current_tenant_id()'::regprocedure"), "v3024_guard", "Dedicated Guard ownership required by DB01; DB07 EXECUTE guarantees unchanged");
                        equal(db.scalar(c, "SELECT prosecdef::text FROM pg_proc WHERE oid='isolation.current_tenant_id()'::regprocedure"), "true", "Protected definer behavior preserved");
                        equal(db.scalar(c, "SELECT array_to_string(proconfig,',') FROM pg_proc WHERE oid='isolation.current_tenant_id()'::regprocedure"), "search_path=pg_catalog, pg_temp", "Controlled definer search_path");
                        equal(db.scalar(c, "SELECT has_function_privilege(?, 'isolation.current_tenant_id()', 'EXECUTE')::text", CELL_A_RUNTIME), "false", "No EXECUTE via PUBLIC or role inheritance");
                        equal(db.scalar(c, "SELECT count(*) FROM pg_namespace n, LATERAL aclexplode(coalesce(nspacl,acldefault('n',nspowner))) a WHERE nspname='isolation' AND a.grantee=0 AND a.privilege_type='USAGE'"), "0", "PUBLIC schema USAGE restricted");
                    });
                    for (String login : List.of(LOGIN_A, LOGIN_B)) {
                        transactions.rollback(login, c -> visibility(c, login, login.equals(LOGIN_A) ? A : B));
                    }
                    transactions.rollback(CELL_A_RUNTIME, c -> {
                        equal(db.scalar(c, "SELECT SESSION_USER"), CELL_A_RUNTIME, "Actual unauthorized TCP identity");
                        equal(db.scalar(c, "SELECT 1"), "1", "Unauthorized connection healthy");
                        db.denied(c, "SELECT isolation.current_tenant_id()", "42501");
                    });
                });
        check("Exact creator global defaults and explicit grant/revoke", "Migration must SET ROLE owner; isolated transaction-only function",
                "Global owner function defaults exclude PUBLIC; schema-local grants absent; unauthorized runtime denied, grant works then revoke denies", () -> transactions.rollback("postgres", c -> {
                    equal(db.scalar(c, "SELECT count(*) FROM pg_default_acl d WHERE defaclrole='v3024_owner'::regrole AND defaclnamespace=0 AND defaclobjtype='f' "
                            + "AND NOT EXISTS (SELECT 1 FROM aclexplode(d.defaclacl) a WHERE a.grantee=0 AND a.privilege_type='EXECUTE')"), "1", "Global default PUBLIC EXECUTE revoked for actual creator");
                    db.update(c, "SET LOCAL ROLE v3024_migration");
                    db.denied(c, "CREATE FUNCTION isolation.v3024_db07_wrong_creator() RETURNS integer LANGUAGE sql AS 'SELECT 7'", "42501");
                    db.update(c, "SET LOCAL ROLE v3024_owner");
                    equal(db.scalar(c, "SELECT CURRENT_USER"), "v3024_owner", "Creation identity, not login identity, controls defaults");
                    db.update(c, "CREATE FUNCTION isolation.v3024_db07_explicit() RETURNS integer LANGUAGE sql AS 'SELECT 7'");
                    equal(db.scalar(c, "SELECT isolation.v3024_db07_explicit()"), "7", "Owner positive");
                    assertDb07NoPublic(c, "isolation.v3024_db07_explicit()");
                    db.update(c, "RESET ROLE");
                    db.update(c, "SET LOCAL SESSION AUTHORIZATION v3024_runtime_login");
                    equal(db.scalar(c, "SELECT has_schema_privilege(session_user,'isolation','USAGE')::text"), "true", "Negative is EXECUTE denial, not schema denial");
                    equal(db.scalar(c, "SELECT has_function_privilege(session_user,'isolation.v3024_db07_explicit()','EXECUTE')::text"), "false", "Runtime inheritance supplies no unintended execution");
                    db.denied(c, "SELECT isolation.v3024_db07_explicit()", "42501");
                    db.update(c, "RESET SESSION AUTHORIZATION");
                    db.update(c, "SET LOCAL ROLE v3024_owner");
                    db.update(c, "GRANT EXECUTE ON FUNCTION isolation.v3024_db07_explicit() TO v3024_runtime");
                    db.update(c, "RESET ROLE");
                    db.update(c, "SET LOCAL SESSION AUTHORIZATION v3024_runtime_login");
                    equal(db.scalar(c, "SELECT isolation.v3024_db07_explicit()"), "7", "Explicit approved role grant inherited");
                    db.update(c, "RESET SESSION AUTHORIZATION");
                    db.update(c, "SET LOCAL ROLE v3024_owner");
                    db.update(c, "REVOKE EXECUTE ON FUNCTION isolation.v3024_db07_explicit() FROM v3024_runtime");
                    db.update(c, "RESET ROLE");
                    db.update(c, "SET LOCAL SESSION AUTHORIZATION v3024_runtime_login");
                    db.denied(c, "SELECT isolation.v3024_db07_explicit()", "42501");
                }));
        check("Ownership transfer does not apply new owner defaults", "Harmless invoker-only rollback fixture created as postgres, then transferred",
                "Unconfigured creator PUBLIC default survives transfer; explicit REVOKE required and effective; safe owner-created transfer stays restricted", () -> transactions.rollback("postgres", c -> {
                    db.update(c, "CREATE FUNCTION isolation.v3024_db07_transfer() RETURNS integer LANGUAGE sql AS 'SELECT 7'");
                    db.update(c, "ALTER FUNCTION isolation.v3024_db07_transfer() OWNER TO v3024_owner");
                    equal(db.scalar(c, "SELECT count(*) FROM pg_proc p, LATERAL aclexplode(coalesce(proacl,acldefault('f',proowner))) a WHERE p.oid='isolation.v3024_db07_transfer()'::regprocedure AND a.grantee=0 AND a.privilege_type='EXECUTE'"), "1", "Transfer alone cannot apply owner defaults retroactively");
                    db.update(c, "SET LOCAL SESSION AUTHORIZATION v3024_runtime_login");
                    equal(db.scalar(c, "SELECT isolation.v3024_db07_transfer()"), "7", "Actual unsafe transfer control, harmless non-definer function");
                    db.update(c, "RESET SESSION AUTHORIZATION");
                    db.update(c, "REVOKE EXECUTE ON FUNCTION isolation.v3024_db07_transfer() FROM PUBLIC");
                    assertDb07NoPublic(c, "isolation.v3024_db07_transfer()");
                    db.update(c, "SET LOCAL SESSION AUTHORIZATION v3024_runtime_login");
                    db.denied(c, "SELECT isolation.v3024_db07_transfer()", "42501");
                    db.update(c, "RESET SESSION AUTHORIZATION");
                    db.update(c, "SET LOCAL ROLE v3024_owner");
                    db.update(c, "CREATE FUNCTION isolation.v3024_db07_safe_transfer() RETURNS integer LANGUAGE sql AS 'SELECT 7'");
                    db.update(c, "RESET ROLE");
                    db.update(c, "ALTER FUNCTION isolation.v3024_db07_safe_transfer() OWNER TO v3024_migration");
                    assertDb07NoPublic(c, "isolation.v3024_db07_safe_transfer()");
                    db.update(c, "ALTER FUNCTION isolation.v3024_db07_safe_transfer() OWNER TO v3024_owner");
                    assertDb07NoPublic(c, "isolation.v3024_db07_safe_transfer()");
                }));
    }

    private void assertDb07NoPublic(Connection c, String signature) throws SQLException {
        equal(db.scalar(c, "SELECT count(*) FROM pg_proc p, LATERAL aclexplode(coalesce(proacl,acldefault('f',proowner))) a "
                + "WHERE p.oid=?::regprocedure AND a.grantee=0 AND a.privilege_type='EXECUTE'", signature), "0", "No effective PUBLIC EXECUTE for fixture");
    }

    private void db08() {
        scenario("DB08", "Missing, malformed and spoofed Tenant Context");
        check("Missing principal mapping denies data", "Unmapped runtime login can authenticate and SELECT 1",
                "Trusted Tenant is NULL, SELECT zero, INSERT rejected 42501", () -> transactions.rollback(RUNTIME, c -> {
                    equal(db.scalar(c, "SELECT 1"), "1", "Positive connectivity control");
                    equal(db.scalar(c, "SELECT coalesce(isolation.current_tenant_id()::text,'NONE')"), "NONE", "Missing trusted mapping");
                    equal(db.scalar(c, "SELECT count(*) FROM isolation.tenant_records"), "0", "Default deny");
                    db.denied(c, "INSERT INTO isolation.tenant_records(id,tenant_id,payload) OVERRIDING SYSTEM VALUE VALUES (?,?::uuid,?)",
                            "42501", FIXTURE_ID, A, "forbidden-unmapped");
                }));
        check("Custom GUC never supplies Tenant authority", "Actual A identity; valid B, malformed and empty untrusted custom settings",
                "A remains A; B remains invisible; B INSERT rejected; malformed SQL UUID rejected", () -> transactions.rollback(LOGIN_A, c -> {
                    visibility(c, LOGIN_A, A);
                    for (String value : List.of(B, "malformed-not-a-uuid", "")) {
                        db.scalar(c, "SELECT set_config('app.tenant_id',?,true)", value);
                        visibility(c, LOGIN_A, A);
                        db.denied(c, "INSERT INTO isolation.tenant_records(id,tenant_id,payload) OVERRIDING SYSTEM VALUE VALUES (?,?::uuid,?)",
                                "42501", FIXTURE_ID, B, "forbidden-spoof");
                    }
                    db.denied(c, "SELECT ?::uuid", "22P02", "malformed-not-a-uuid");
                }));
    }

    private void db09() {
        scenario("DB09", "Context clearing after commit and connection reuse");
        check("Transaction-local setting cleared after COMMIT", "One-connection A pool; actual principal and backend PID checked",
                "Same PID reused; transaction-local setting cleared and no other-Tenant visibility", () -> {
                    try (var pool = db.pool(LOGIN_A)) {
                        String pid;
                        try (Connection c = pool.getConnection()) {
                            visibility(c, LOGIN_A, A);
                            pid = db.scalar(c, "SELECT pg_backend_pid()");
                            c.setAutoCommit(false);
                            db.scalar(c, "SELECT set_config('app.tenant_id',?,true)", B);
                            c.commit();
                            c.setAutoCommit(true);
                        }
                        try (Connection c = pool.getConnection()) {
                            equal(db.scalar(c, "SELECT pg_backend_pid()"), pid, "Physical connection reuse after COMMIT");
                            equal(db.scalar(c, "SELECT coalesce(current_setting('app.tenant_id',true),'')"), "", "Transaction-local Context clearing");
                            visibility(c, LOGIN_A, A);
                        }
                    }
                });
        check("Original uncontrolled leakage remains a separately reported FAIL", "Unmodified raw Hikari pool, explicitly outside private secured Unit of Work",
                "Original empty-next-borrower assertion must still fail and be recorded; raw path is NOT remediated", () -> {
                    try (var pool = db.pool(LOGIN_A)) {
                        String pid;
                        boolean originalAssertionFailed = false;
                        try {
                            try (Connection c = pool.getConnection()) {
                                visibility(c, LOGIN_A, A);
                                pid = db.scalar(c, "SELECT pg_backend_pid()");
                                db.scalar(c, "SELECT set_config('app.tenant_id',?,false)", B);
                            }
                            try (Connection c = pool.getConnection()) {
                                equal(db.scalar(c, "SELECT pg_backend_pid()"), pid, "Physical reuse for leakage test");
                                String leaked = db.scalar(c, "SELECT coalesce(current_setting('app.tenant_id',true),'')");
                                visibility(c, LOGIN_A, A);
                                try {
                                    // The original safe-state expectation is unchanged, not inverted to call a leak safe.
                                    equal(leaked, "", "Hikari arbitrary SESSION GUC reset (known hazard)");
                                } catch (AssertionError originalFailure) {
                                    originalAssertionFailed = true;
                                    emit(Map.of("type", "finding", "id", "DB09", "repeat", db.repetition,
                                            "classification", "CONFIRMED UNCONTROLLED SESSION-STATE LEAKAGE", "observedSetting", leaked,
                                            "trustedTenantStill", A, "backendPid", pid, "status", "FAIL",
                                            "acceptanceScope", "RAW PATH OUTSIDE SECURED CONTRACT; NOT FIXED"));
                                    emit(Map.of("type", "raw-hazard", "id", "DB09", "repeat", db.repetition,
                                            "status", "FAIL", "expected", "empty next-borrower SESSION GUC", "actual", leaked,
                                            "reason", originalFailure.getMessage(), "securedPathAcceptance", false));
                                }
                                require(originalAssertionFailed && leaked.equals(B), "Known raw leak must remain faithfully reproducible");
                            }
                        } finally {
                            try (Connection c = pool.getConnection()) {
                                db.update(c, "RESET app.tenant_id");
                            }
                        }
                    }
                });
        check("Private secured commit path removes adversarial SESSION contamination", "Pool size one; fixed authenticated A identity; explicit completion",
                "Same PID reused, local request metadata cleared, SESSION tenant/search_path scrubbed, no B access", () -> {
                    try (var safe = transactions.secured(LOGIN_A)) {
                        var pid = new String[1];
                        safe.execute("db09-commit-a", s -> {
                            secureVisibility(s, LOGIN_A, A);
                            require(!s.autoCommit(), "Explicit request transaction, not auto-commit");
                            pid[0] = s.scalar("SELECT pg_catalog.pg_backend_pid()");
                            equal(s.scalar("SELECT pg_catalog.current_setting('app.request_context',true)"), "db09-commit-a", "Own local request context");
                            s.untrustedSetting("app.tenant_id", B, false);
                            s.untrustedSetting("search_path", "pg_temp, public", false);
                            equal(s.scalar("SELECT pg_catalog.current_setting('app.tenant_id',true)"), B, "Actual SESSION poison present inside leased connection");
                            secureVisibility(s, LOGIN_A, A);
                            s.rejectCrossTenantInsert(B);
                            for (String role : List.of("postgres", "v3024_owner", "v3024_migration", "v3024_runtime", CELL_B_RUNTIME)) {
                                s.rejectRole(role);
                            }
                            s.complete();
                        });
                        equal(safe.inspectIdle().pid(), pid[0], "Verified clean before reuse, not a replacement connection");
                        safe.execute("db09-next-a", s -> {
                            equal(s.scalar("SELECT pg_catalog.pg_backend_pid()"), pid[0], "Same physical connection next request");
                            equal(s.scalar("SELECT pg_catalog.current_setting('app.request_context',true)"), "db09-next-a", "No prior request metadata");
                            equal(s.scalar("SELECT pg_catalog.current_setting('app.tenant_id',true)"), "", "No previous SESSION Tenant setting");
                            equal(s.scalar("SELECT pg_catalog.current_setting('search_path')"), "pg_catalog", "No prior search_path");
                            secureVisibility(s, LOGIN_A, A);
                            s.complete();
                        });
                        equal(safe.inspectIdle().pid(), pid[0], "Clean auto-commit idle connection after multiple transactions");
                    }
                });
        check("SQL exception rolls back data and state", "Private one-connection A pool; real PostgreSQL 22012, not a simulated driver error",
                "SQLSTATE 22012 surfaced; fixture rollback and same-PID clean recovery", () -> {
                    try (var safe = transactions.secured(LOGIN_A)) {
                        var pid = new String[1];
                        try {
                            safe.execute("db09-sql-error", s -> {
                                pid[0] = s.scalar("SELECT pg_catalog.pg_backend_pid()");
                                secureFixture(s, A);
                                s.untrustedSetting("app.tenant_id", B, false);
                                s.scalar("SELECT 1/0");
                                s.complete();
                            });
                            throw new AssertionError("Expected real SQL failure");
                        } catch (SQLException expected) {
                            equal(state(expected), "22012", "Execution failure surfaced, not hidden by cleanup");
                        }
                        equal(safe.inspectIdle().pid(), pid[0], "Same connection clean after SQL failure");
                        assertSecuredRecovery(safe, A, "db09-after-sql-error");
                    }
                });
        check("Application exception and incomplete completion roll back", "No raw JDBC transaction control exposed to callback",
                "Application error/omitted completion surfaced; uncommitted fixtures removed; safe reuse", () -> {
                    try (var safe = transactions.secured(LOGIN_A)) {
                        try {
                            safe.execute("db09-app-error", s -> {
                                secureFixture(s, A);
                                s.untrustedSetting("app.tenant_id", B, false);
                                throw new IllegalStateException("db09-intentional-application-error");
                            });
                            throw new AssertionError("Application failure was swallowed");
                        } catch (IllegalStateException expected) {
                            equal(expected.getMessage(), "db09-intentional-application-error", "Original application exception surfaced");
                        }
                        String pid = safe.inspectIdle().pid();
                        assertSecuredRecovery(safe, A, "db09-after-app-error");
                        try {
                            safe.execute("db09-incomplete", s -> {
                                secureFixture(s, A);
                                s.untrustedSetting("search_path", "public", false);
                                // Intentionally omit logical complete(); caller cannot accidentally commit it.
                            });
                            throw new AssertionError("Incomplete request was committed");
                        } catch (TenantUnitOfWork.IncompleteRequest expected) {
                            emit(Map.of("type", "classification", "id", "DB09", "repeat", db.repetition, "actual", "INCOMPLETE_REQUEST_ROLLED_BACK"));
                        }
                        equal(safe.inspectIdle().pid(), pid, "Same safe backend after incomplete request");
                        assertSecuredRecovery(safe, A, "db09-after-incomplete");
                    }
                });
        check("Lease lifetime, context integrity and transaction-boundary guards", "No Connection/pool getter; request view closes on return and is thread-confined",
                "Tampered request metadata and caller COMMIT rejected; escaped/cross-thread views cannot execute SQL", () -> {
                    try (var safe = transactions.secured(LOGIN_A)) {
                        try {
                            safe.execute("db09-context-integrity", s -> {
                                s.untrustedSetting("app.request_context", "forged-other-request", false);
                                s.complete();
                            });
                            throw new AssertionError("Request Context tampering not surfaced");
                        } catch (TenantUnitOfWork.ContextViolation expected) {
                            safe.inspectIdle();
                        }
                        try {
                            safe.execute("db09-boundary", s -> s.scalar("COMMIT"));
                            throw new AssertionError("Caller transaction boundary accepted");
                        } catch (IllegalArgumentException expected) {
                            require(expected.getMessage().contains("transaction control"), "Precise boundary rejection");
                        }
                        var escaped = new java.util.concurrent.atomic.AtomicReference<TenantUnitOfWork.RequestSession>();
                        safe.execute("db09-lease", s -> {
                            escaped.set(s);
                            try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
                                try {
                                    worker.submit(() -> s.scalar("SELECT 1")).get(10, java.util.concurrent.TimeUnit.SECONDS);
                                    throw new AssertionError("Cross-thread lease used");
                                } catch (java.util.concurrent.ExecutionException expected) {
                                    require(expected.getCause() instanceof IllegalStateException, "Lease thread guard failure category");
                                }
                            }
                            secureVisibility(s, LOGIN_A, A);
                            s.complete();
                        });
                        try {
                            escaped.get().scalar("SELECT 1");
                            throw new AssertionError("Escaped lease usable after pool return");
                        } catch (IllegalStateException expected) {
                            require(expected.getMessage().contains("lease"), "Closed lease failure category");
                        }
                        for (var method : TenantUnitOfWork.RequestSession.class.getDeclaredMethods()) {
                            require(!Connection.class.isAssignableFrom(method.getReturnType())
                                    && !com.zaxxer.hikari.HikariDataSource.class.isAssignableFrom(method.getReturnType()),
                                    "Request API must not expose raw JDBC or pool handles");
                        }
                        safe.inspectIdle();
                    }
                });
        check("Real cleanup failure evicts and surfaces", "Terminate only the currently leased task-owned pool backend after its commit",
                "Cleanup failure is visible, old PID never reused, replacement authenticates and recovers safely", () -> {
                    try (var safe = transactions.secured(LOGIN_A)) {
                        safe.execute("db09-before-cleanup-fault", s -> { secureVisibility(s, LOGIN_A, A); s.complete(); });
                        String oldPid = safe.inspectIdle().pid();
                        safe.terminateOwnBackendAtNextCleanup();
                        try {
                            safe.execute("db09-cleanup-fault", s -> { secureVisibility(s, LOGIN_A, A); s.complete(); });
                            throw new AssertionError("Cleanup failure silently accepted");
                        } catch (TenantUnitOfWork.CleanupFailure expected) {
                            require(List.of("57P01", "08006", "08003").contains(state(expected)), "Real disconnected-session cleanup classification");
                            require(safe.evictions() >= 1, "Explicit eviction occurred");
                        }
                        assertSecuredRecovery(safe, A, "db09-after-cleanup-fault");
                        require(!safe.inspectIdle().pid().equals(oldPid), "Evicted physical connection cannot be reused");
                    }
                });
        check("Sequential interleaved Tenant requests", "Two private authenticated one-connection pools, never shared credentials",
                "Multiple transactions reuse own PID; A/B contexts and data never cross logical requests", () -> {
                    try (var a = transactions.secured(LOGIN_A); var b = transactions.secured(LOGIN_B)) {
                        String pidA = null;
                        String pidB = null;
                        for (int i = 0; i < 5; i++) {
                            assertSecuredRecovery(a, A, "db09-interleave-a-" + i);
                            assertSecuredRecovery(b, B, "db09-interleave-b-" + i);
                            String currentA = a.inspectIdle().pid();
                            String currentB = b.inspectIdle().pid();
                            require(!currentA.equals(currentB), "Independently authenticated pools");
                            if (pidA != null) { equal(currentA, pidA, "A physical reuse"); equal(currentB, pidB, "B physical reuse"); }
                            pidA = currentA;
                            pidB = currentB;
                        }
                    }
                });
        check("Concurrent logical requests with bounded single-connection pools", "Four caller threads, twelve read-only requests, two private authenticated pools",
                "Pool serialization preserves own PID, Context, principal and data; no context remains on idle return", () -> {
                    try (var workers = java.util.concurrent.Executors.newFixedThreadPool(4);
                            var a = transactions.secured(LOGIN_A); var b = transactions.secured(LOGIN_B)) {
                        var start = new java.util.concurrent.CountDownLatch(1);
                        var pidA = new java.util.concurrent.atomic.AtomicReference<String>();
                        var pidB = new java.util.concurrent.atomic.AtomicReference<String>();
                        var pending = new ArrayList<java.util.concurrent.Future<Void>>();
                        for (int i = 0; i < 12; i++) {
                            boolean first = i % 2 == 0;
                            String request = "db09-concurrent-" + i;
                            pending.add(workers.submit(() -> {
                                require(start.await(10, java.util.concurrent.TimeUnit.SECONDS), "Concurrent start bound");
                                (first ? a : b).execute(request, s -> {
                                    secureVisibility(s, first ? LOGIN_A : LOGIN_B, first ? A : B);
                                    equal(s.scalar("SELECT pg_catalog.current_setting('app.request_context',true)"), request, "Concurrent request owns metadata");
                                    String pid = s.scalar("SELECT pg_catalog.pg_backend_pid()");
                                    var expected = first ? pidA : pidB;
                                    expected.compareAndSet(null, pid);
                                    equal(pid, expected.get(), "Same per-Tenant physical connection reused concurrently");
                                    s.complete();
                                });
                                return null;
                            }));
                        }
                        start.countDown();
                        for (var future : pending) { future.get(30, java.util.concurrent.TimeUnit.SECONDS); }
                        require(!pidA.get().equals(pidB.get()), "No shared authenticated physical connection across Tenants");
                        equal(a.inspectIdle().pid(), pidA.get(), "A safe idle PID after concurrent requests");
                        equal(b.inspectIdle().pid(), pidB.get(), "B safe idle PID after concurrent requests");
                    }
                });
    }

    private void secureVisibility(TenantUnitOfWork.RequestSession s, String login, String tenant) throws SQLException {
        equal(s.scalar("SELECT SESSION_USER"), login, "Secured authenticated principal");
        equal(s.scalar("SELECT CURRENT_USER"), login, "No role escalation");
        equal(s.scalar("SELECT isolation.current_tenant_id()::text"), tenant, "Protected mapping, not request metadata");
        require(Long.parseLong(s.scalar("SELECT count(*) FROM isolation.tenant_records WHERE tenant_id=?::uuid", tenant)) > 0,
                "Secured own-Tenant positive control");
        equal(s.scalar("SELECT count(*) FROM isolation.tenant_records WHERE tenant_id<>?::uuid", tenant), "0", "No cross-Tenant visibility");
    }

    private void secureFixture(TenantUnitOfWork.RequestSession s, String tenant) throws SQLException {
        equal(s.update("INSERT INTO isolation.tenant_records(id,tenant_id,payload) OVERRIDING SYSTEM VALUE VALUES (?,?::uuid,?)",
                FIXTURE_ID, tenant, "db09-rollback-only"), 1, "Real uncommitted fixture");
    }

    private void assertSecuredRecovery(TenantUnitOfWork.Secured unit, String tenant, String context) throws Exception {
        unit.execute(context, s -> {
            secureVisibility(s, tenant.equals(A) ? LOGIN_A : LOGIN_B, tenant);
            equal(s.scalar("SELECT pg_catalog.current_setting('app.request_context',true)"), context, "Only current request metadata");
            equal(s.scalar("SELECT coalesce(pg_catalog.current_setting('app.tenant_id',true),'')"), "", "No stale caller Tenant GUC");
            equal(s.scalar("SELECT count(*) FROM isolation.tenant_records WHERE id=?", FIXTURE_ID), "0", "Failed/incomplete operation did not commit fixture");
            s.complete();
        });
        unit.inspectIdle();
    }

    private void db10() {
        scenario("DB10", "Context clearing after rollback");
        check("Rollback clears local setting and uncommitted writes", "Actual A pool; rollback-only explicit fixture ID",
                "Same PID reused; local setting and row disappear; trusted identity and positive reads remain valid", () -> {
                    try (var pool = db.pool(LOGIN_A)) {
                        var pid = new String[1];
                        transactions.pooledRollback(pool, c -> {
                            visibility(c, LOGIN_A, A);
                            pid[0] = db.scalar(c, "SELECT pg_backend_pid()");
                            fixture(c, A);
                            db.scalar(c, "SELECT set_config('app.tenant_id',?,true)", B);
                        });
                        try (Connection c = pool.getConnection()) {
                            equal(db.scalar(c, "SELECT pg_backend_pid()"), pid[0], "Physical reuse after ROLLBACK");
                            equal(db.scalar(c, "SELECT coalesce(current_setting('app.tenant_id',true),'')"), "", "Local Context removed after rollback");
                            equal(db.scalar(c, "SELECT count(*) FROM isolation.tenant_records WHERE id=?", FIXTURE_ID), "0", "Fixture rolled back");
                            visibility(c, LOGIN_A, A);
                        }
                    }
                });
    }

    private void db11() {
        scenario("DB11", "Error-path cleanup and pool reuse");
        check("Failed SQL aborts transaction, rollback recovers same connection", "Separate A/B pools; explicit Unit of Work, not implicit arbitrary-GUC sanitizer",
                "22012 then 25P02, rollback; same PID reused with no local Context or forbidden data", () -> {
                    try (var poolA = db.pool(LOGIN_A); var poolB = db.pool(LOGIN_B)) {
                        var pid = new String[1];
                        transactions.pooledRollback(poolA, c -> {
                            visibility(c, LOGIN_A, A);
                            pid[0] = db.scalar(c, "SELECT pg_backend_pid()");
                            fixture(c, A);
                            db.scalar(c, "SELECT set_config('app.tenant_id',?,true)", B);
                            try {
                                db.scalar(c, "SELECT 1/0");
                                throw new AssertionError("Expected real division-by-zero SQL error");
                            } catch (SQLException expected) {
                                equal(state(expected), "22012", "Actual SQL error classification");
                            }
                            try {
                                db.scalar(c, "SELECT 1");
                                throw new AssertionError("Aborted transaction unexpectedly usable");
                            } catch (SQLException expected) {
                                equal(state(expected), "25P02", "Actual aborted-transaction state");
                            }
                        });
                        for (int i = 0; i < 3; i++) {
                            try (Connection a = poolA.getConnection(); Connection b = poolB.getConnection()) {
                                equal(db.scalar(a, "SELECT pg_backend_pid()"), pid[0], "Same A backend after SQL error");
                                require(!db.scalar(b, "SELECT pg_backend_pid()").equals(pid[0]), "B must have independent authenticated connection");
                                equal(db.scalar(a, "SELECT coalesce(current_setting('app.tenant_id',true),'')"), "", "Error-path local cleanup");
                                visibility(a, LOGIN_A, A);
                                visibility(b, LOGIN_B, B);
                            }
                        }
                    }
                });
    }

    private void db12() {
        scenario("DB12", "Cross-Cell and cross-service DB credential boundaries");
        check("Cross-service connection rejected", "Admin verifies control database healthy; A login has positive own-data access",
                "Tenant A cannot CONNECT v3024_control_a; 42501", () -> {
                    try (Connection own = db.connect(LOGIN_A); Connection control = db.connect("postgres", "v3024_control_a")) {
                        visibility(own, LOGIN_A, A);
                        equal(db.scalar(control, "SELECT 1"), "1", "Control database healthy");
                    }
                    connectionDenied(LOGIN_A, "v3024_control_a", "42501");
                });
        check("Cross-Cell credential misuse rejected", "Cell B database exists and is healthy; no routing mock",
                "Tenant A credentials must not CONNECT v3024_probe_b", () -> {
                    try (Connection own = db.connect(LOGIN_A); Connection other = db.connect("postgres", "v3024_probe_b")) {
                        visibility(own, LOGIN_A, A);
                        equal(db.scalar(other, "SELECT 1"), "1", "Cell B database healthy");
                    }
                    connectionDenied(LOGIN_A, "v3024_probe_b", "42501");
                });
        check("Independent Cell A/B ordinary-role data controls", "Dedicated passwords, independently owned cell_probe fixtures; no legacy Tenant identity substitution",
                "Own Cell SELECT/CRUD succeeds; owner, schema and RLS controls verified independently", () -> {
                    for (boolean cellA : List.of(true, false)) {
                        String target = cellA ? DATABASE : "v3024_probe_b";
                        String runtime = cellA ? CELL_A_RUNTIME : CELL_B_RUNTIME;
                        try (Connection c = db.connect(runtime, target)) {
                            cellVisibility(c, cellA);
                            c.setAutoCommit(false);
                            try {
                                equal(db.update(c, "INSERT INTO cell_probe.cell_records(record_key,tenant_id,cell_id,payload) VALUES (?,?::uuid,?,?)",
                                        "db12-rollback", cellA ? A : B, cellA ? "cell-a" : "cell-b", "rollback-only"), 1, "Own Cell INSERT");
                                equal(db.update(c, "UPDATE cell_probe.cell_records SET payload=? WHERE record_key=?", "changed", "db12-rollback"), 1, "Own Cell UPDATE");
                                equal(db.update(c, "DELETE FROM cell_probe.cell_records WHERE record_key=?", "db12-rollback"), 1, "Own Cell DELETE");
                                db.denied(c, "CREATE TABLE cell_probe.db12_runtime_forbidden(id integer)", "42501");
                                db.denied(c, "TRUNCATE cell_probe.cell_records", "42501");
                            } finally {
                                c.rollback();
                            }
                        }
                        try (Connection admin = db.connect("postgres", target)) {
                            equal(db.scalar(admin, "SELECT pg_get_userbyid(nspowner) FROM pg_namespace WHERE nspname='cell_probe'"),
                                    cellA ? "v3024_db12_owner_a" : "v3024_db12_owner_b", "Independent schema ownership");
                            equal(db.scalar(admin, "SELECT pg_get_userbyid(relowner) FROM pg_class WHERE oid='cell_probe.cell_records'::regclass"),
                                    cellA ? "v3024_db12_owner_a" : "v3024_db12_owner_b", "Independent table ownership");
                            db.inventory(admin, "SELECT * FROM pg_policies WHERE schemaname='cell_probe' ORDER BY policyname");
                        }
                    }
                });
        check("Bidirectional direct TCP CONNECT enforcement", "Both Cell runtime principals have valid own-database positive controls on IPv4/IPv6",
                "A-to-B and B-to-A denied by PostgreSQL CONNECT, SQLSTATE 42501; wrong password 28P01", () -> {
                    for (String host : List.of("127.0.0.1", "::1")) {
                        for (boolean cellA : List.of(true, false)) {
                            String runtime = cellA ? CELL_A_RUNTIME : CELL_B_RUNTIME;
                            String own = cellA ? DATABASE : "v3024_probe_b";
                            String other = cellA ? "v3024_probe_b" : DATABASE;
                            try (Connection positive = db.connectHost(runtime, own, host)) {
                                cellVisibility(positive, cellA);
                            }
                            try (Connection unexpected = db.connectHost(runtime, other, host)) {
                                throw new AssertionError("Cross-Cell CONNECT accepted; autoCommit=" + unexpected.getAutoCommit());
                            } catch (SQLException expected) {
                                equal(state(expected), "42501", "Correct credentials, unauthorized database");
                            }
                            tcpDenied(runtime, "intentionally-invalid-db12-password", own, host, "28P01");
                            connectionDenied(runtime, "v3024_control_a", "42501");
                        }
                    }
                });
        check("No transitive runtime escape or SET ROLE bypass", "Dedicated runtime logins have no role memberships; legacy runtime group is Cell A only",
                "No inherited CONNECT to other Cell, migration/owner/admin role switching or session impersonation", () -> {
                    for (boolean cellA : List.of(true, false)) {
                        String runtime = cellA ? CELL_A_RUNTIME : CELL_B_RUNTIME;
                        try (Connection c = db.connect(runtime, cellA ? DATABASE : "v3024_probe_b")) {
                            cellVisibility(c, cellA);
                            equal(db.scalar(c, "SELECT (NOT rolsuper AND NOT rolinherit AND NOT rolbypassrls AND NOT rolcreaterole AND NOT rolcreatedb)::text FROM pg_roles WHERE rolname=session_user"), "true", "Ordinary NOINHERIT runtime");
                            equal(db.scalar(c, "SELECT count(*) FROM pg_auth_members WHERE member=(SELECT oid FROM pg_roles WHERE rolname=session_user)"), "0", "No runtime membership paths");
                            equal(db.scalar(c, "SELECT has_database_privilege(session_user,?,'CONNECT')::text", cellA ? "v3024_probe_b" : DATABASE), "false", "Effective cross-Cell CONNECT denied");
                            c.setAutoCommit(false);
                            try {
                                for (String role : List.of("postgres", "v3024_owner", "v3024_migration", "v3024_runtime",
                                        "v3024_db12_owner_a", "v3024_db12_owner_b", CELL_A_MIGRATION, CELL_B_MIGRATION,
                                        cellA ? CELL_B_RUNTIME : CELL_A_RUNTIME)) {
                                    db.denied(c, "SET ROLE " + role, "42501");
                                }
                                db.denied(c, "SET SESSION AUTHORIZATION postgres", "42501");
                            } finally {
                                c.rollback();
                            }
                        }
                    }
                    for (String legacy : List.of(LOGIN_A, LOGIN_B, RUNTIME)) {
                        try (Connection positive = db.connect(legacy)) {
                            equal(db.scalar(positive, "SELECT 1"), "1", "Legacy Cell A connection preserved");
                        }
                        connectionDenied(legacy, "v3024_probe_b", "42501");
                    }
                    try (Connection admin = db.connect("postgres")) {
                        db.inventory(admin, "SELECT r.rolname,d.datname,has_database_privilege(r.oid,d.oid,'CONNECT') AS effective_connect FROM pg_roles r CROSS JOIN pg_database d "
                                + "WHERE r.rolname LIKE 'v3024%' AND r.rolcanlogin AND d.datname LIKE 'v3024%' ORDER BY r.rolname,d.datname");
                        db.inventory(admin, "WITH RECURSIVE paths AS (SELECT member,roleid,inherit_option,set_option,ARRAY[member,roleid] AS path FROM pg_auth_members "
                                + "UNION ALL SELECT p.member,m.roleid,p.inherit_option AND m.inherit_option,p.set_option AND m.set_option,p.path||m.roleid "
                                + "FROM paths p JOIN pg_auth_members m ON m.member=p.roleid WHERE NOT m.roleid=ANY(p.path)) "
                                + "SELECT pg_get_userbyid(member) AS login,pg_get_userbyid(roleid) AS reachable_role,inherit_option,set_option FROM paths "
                                + "WHERE pg_get_userbyid(member) LIKE 'v3024%' ORDER BY 1,2");
                    }
                });
        check("Cell migration identities are independently scoped", "Actual SCRAM migration logins, only same-Cell NOLOGIN owner membership with SET but no INHERIT",
                "Own schema DDL succeeds and rolls back; other database and owner role denied", () -> {
                    for (boolean cellA : List.of(true, false)) {
                        String migration = cellA ? CELL_A_MIGRATION : CELL_B_MIGRATION;
                        try (Connection c = db.connect(migration, cellA ? DATABASE : "v3024_probe_b")) {
                            equal(db.scalar(c, "SELECT session_user"), migration, "Actual authenticated migration identity");
                            c.setAutoCommit(false);
                            try {
                                db.denied(c, "SET ROLE " + (cellA ? "v3024_db12_owner_b" : "v3024_db12_owner_a"), "42501");
                                db.denied(c, "SET ROLE v3024_owner", "42501");
                                db.update(c, "SET LOCAL ROLE " + (cellA ? "v3024_db12_owner_a" : "v3024_db12_owner_b"));
                                db.update(c, "CREATE TABLE cell_probe.db12_migration_rollback(id text PRIMARY KEY)");
                                equal(db.scalar(c, "SELECT current_user"), cellA ? "v3024_db12_owner_a" : "v3024_db12_owner_b", "Scoped owning role operation");
                            } finally {
                                c.rollback();
                            }
                        }
                        connectionDenied(migration, cellA ? "v3024_probe_b" : DATABASE, "42501");
                        connectionDenied(migration, "v3024_control_a", "42501");
                    }
                });
        check("Separate Cell pools cannot reuse another Cell credential or data path", "One physical connection per runtime/database pool; no sanitizer or routing mock",
                "Same own PID reused; distinct Cell PIDs/principals/data; wrong-target pool initialization fails 42501", () -> {
                    try (var a = db.pool(CELL_A_RUNTIME, DATABASE); var b = db.pool(CELL_B_RUNTIME, "v3024_probe_b")) {
                        String pidA = null;
                        String pidB = null;
                        for (int i = 0; i < 3; i++) {
                            try (Connection ca = a.getConnection(); Connection cb = b.getConnection()) {
                                cellVisibility(ca, true);
                                cellVisibility(cb, false);
                                String currentA = db.scalar(ca, "SELECT pg_backend_pid()");
                                String currentB = db.scalar(cb, "SELECT pg_backend_pid()");
                                require(!currentA.equals(currentB), "Cells must not share a physical connection");
                                if (pidA != null) {
                                    equal(currentA, pidA, "Cell A physical pool reuse");
                                    equal(currentB, pidB, "Cell B physical pool reuse");
                                }
                                pidA = currentA;
                                pidB = currentB;
                            }
                        }
                    }
                    poolDenied(CELL_A_RUNTIME, "v3024_probe_b");
                    poolDenied(CELL_B_RUNTIME, DATABASE);
                });
    }

    private void cellVisibility(Connection c, boolean cellA) throws SQLException {
        equal(db.scalar(c, "SELECT session_user"), cellA ? CELL_A_RUNTIME : CELL_B_RUNTIME, "Dedicated Cell runtime principal");
        equal(db.scalar(c, "SELECT current_database()"), cellA ? DATABASE : "v3024_probe_b", "Actual Cell database");
        equal(db.scalar(c, "SELECT count(*) FROM cell_probe.cell_records WHERE record_key='db12-baseline' AND tenant_id=?::uuid AND cell_id=?",
                cellA ? A : B, cellA ? "cell-a" : "cell-b"), "1", "Independent Cell fixture positive control");
        equal(db.scalar(c, "SELECT count(*) FROM cell_probe.cell_records WHERE tenant_id<>?::uuid OR cell_id<>?",
                cellA ? A : B, cellA ? "cell-a" : "cell-b"), "0", "No unauthorized Cell/Tenant data");
        equal(db.scalar(c, "SELECT (relrowsecurity AND relforcerowsecurity AND relowner<>(SELECT oid FROM pg_roles WHERE rolname=session_user))::text FROM pg_class WHERE oid='cell_probe.cell_records'::regclass"),
                "true", "Independent fixture RLS and non-owner runtime");
    }

    private void poolDenied(String user, String database) {
        try (var unexpected = db.pool(user, database)) {
            throw new AssertionError("Cross-Cell pool initialized: " + unexpected.getPoolName());
        } catch (com.zaxxer.hikari.pool.HikariPool.PoolInitializationException expected) {
            Throwable cause = expected;
            while (cause != null && !(cause instanceof SQLException)) {
                cause = cause.getCause();
            }
            require(cause instanceof SQLException, "Pool failure must originate from an actual PostgreSQL denial");
            equal(state((SQLException) cause), "42501", "Wrong-target pool SQLSTATE");
            emit(Map.of("type", "pool-denial", "id", "DB12", "repeat", db.repetition,
                    "principal", user, "database", database, "sqlstate", "42501"));
        }
    }

    private void setupDb12() throws Exception {
        String preserved = db.dataFingerprint();
        boolean rolesCommitted = false;
        boolean fixtureA = false;
        boolean fixtureB = false;
        List<String> roles = List.of("v3024_db12_owner_a", "v3024_db12_owner_b", CELL_A_RUNTIME,
                CELL_B_RUNTIME, CELL_A_MIGRATION, CELL_B_MIGRATION);
        try (Connection admin = db.connect("postgres")) {
            equal(db.scalar(admin, "SHOW password_encryption"), "scram-sha-256", "Setup requires existing SCRAM encoding");
            equal(db.scalar(admin, "SELECT count(*) FROM pg_hba_file_rules WHERE type LIKE 'host%' AND auth_method<>'scram-sha-256'"), "0", "Setup never repairs or changes authentication");
            for (String role : roles) {
                equal(db.scalar(admin, "SELECT count(*) FROM pg_roles WHERE rolname=?", role), "0", "Will not overwrite any existing DB12 role");
            }
            for (String target : List.of(DATABASE, "v3024_probe_b")) {
                try (Connection c = db.connect("postgres", target)) {
                    equal(db.scalar(c, "SELECT to_regnamespace('cell_probe')"), null, "Will not overwrite an existing schema");
                }
            }
            equal(db.scalar(admin, "SELECT has_database_privilege('v3024_runtime','v3024_probe_b','CONNECT')::text"), "true", "Expected legacy shared CONNECT finding before remediation");
            equal(db.scalar(admin, "SELECT count(*) FROM pg_stat_activity WHERE datname='v3024_probe_b' AND usename IN ('v3024_runtime_login','v3024_tenant_a_login','v3024_tenant_b_login')"),
                    "0", "No pre-existing legacy runtime sessions in Cell B; will not terminate another actor's session");
            try {
                admin.setAutoCommit(false);
                try {
                    for (String owner : roles.subList(0, 2)) {
                        db.update(admin, "CREATE ROLE " + owner + " NOLOGIN NOINHERIT NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION");
                    }
                    for (String login : roles.subList(2, 6)) {
                        db.secretSql(admin, login, db.credential(login));
                    }
                    db.update(admin, "GRANT v3024_db12_owner_a TO v3024_db12_migration_a WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
                    db.update(admin, "GRANT v3024_db12_owner_b TO v3024_db12_migration_b WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
                    db.update(admin, "GRANT CONNECT ON DATABASE v3024_probe_a TO v3024_db12_runtime_a,v3024_db12_migration_a");
                    db.update(admin, "GRANT CONNECT ON DATABASE v3024_probe_b TO v3024_db12_runtime_b,v3024_db12_migration_b");
                    db.update(admin, "REVOKE CONNECT ON DATABASE v3024_probe_b FROM v3024_runtime");
                    admin.commit();
                    rolesCommitted = true;
                } catch (Exception | AssertionError failure) {
                    admin.rollback();
                    throw failure;
                } finally {
                    admin.setAutoCommit(true);
                }
                createCellFixture(true);
                fixtureA = true;
                createCellFixture(false);
                fixtureB = true;
                equal(db.dataFingerprint(), preserved, "Legacy Cell A data and mappings preserved by setup");
                emit(Map.of("type", "setup", "status", "PASS", "task", "V3-024-DB12-CELL-ISOLATION-REMEDIATION",
                        "rolesCreated", 6, "fixturesCreated", 2, "legacyChange", "Only v3024_runtime CONNECT on v3024_probe_b revoked"));
            } catch (Exception | AssertionError failure) {
                // Compensate only resources created by this invocation, never existing fixtures.
                if (fixtureB) { removeOwnedCellFixture(false); }
                if (fixtureA) { removeOwnedCellFixture(true); }
                if (rolesCommitted) {
                    db.update(admin, "GRANT CONNECT ON DATABASE v3024_probe_b TO v3024_runtime");
                    db.update(admin, "REVOKE CONNECT ON DATABASE v3024_probe_a FROM v3024_db12_runtime_a,v3024_db12_migration_a");
                    db.update(admin, "REVOKE CONNECT ON DATABASE v3024_probe_b FROM v3024_db12_runtime_b,v3024_db12_migration_b");
                    for (String role : List.of(CELL_A_MIGRATION, CELL_B_MIGRATION, CELL_A_RUNTIME, CELL_B_RUNTIME,
                            "v3024_db12_owner_a", "v3024_db12_owner_b")) {
                        db.update(admin, "DROP ROLE " + role);
                    }
                }
                throw failure;
            }
        }
    }

    private void createCellFixture(boolean cellA) throws Exception {
        String owner = cellA ? "v3024_db12_owner_a" : "v3024_db12_owner_b";
        String runtime = cellA ? CELL_A_RUNTIME : CELL_B_RUNTIME;
        String tenant = cellA ? A : B;
        String cell = cellA ? "cell-a" : "cell-b";
        try (Connection c = db.connect("postgres", cellA ? DATABASE : "v3024_probe_b")) {
            c.setAutoCommit(false);
            try {
                db.update(c, "CREATE SCHEMA cell_probe AUTHORIZATION " + owner);
                db.update(c, "SET LOCAL ROLE " + owner);
                db.update(c, "REVOKE ALL ON SCHEMA cell_probe FROM PUBLIC");
                db.update(c, "CREATE TABLE cell_probe.cell_records(record_key text PRIMARY KEY,tenant_id uuid NOT NULL,cell_id text NOT NULL,"
                        + "payload text NOT NULL,CHECK(tenant_id='" + tenant + "'::uuid AND cell_id='" + cell + "'))");
                db.update(c, "INSERT INTO cell_probe.cell_records VALUES ('db12-baseline',?::uuid,?,?)", tenant, cell, "db12-fixture-" + cell);
                db.update(c, "REVOKE ALL ON TABLE cell_probe.cell_records FROM PUBLIC");
                db.update(c, "GRANT USAGE ON SCHEMA cell_probe TO " + runtime);
                db.update(c, "GRANT SELECT,INSERT,UPDATE,DELETE ON cell_probe.cell_records TO " + runtime);
                db.update(c, "CREATE POLICY db12_cell_boundary ON cell_probe.cell_records TO " + runtime
                        + " USING(tenant_id='" + tenant + "'::uuid AND cell_id='" + cell + "')"
                        + " WITH CHECK(tenant_id='" + tenant + "'::uuid AND cell_id='" + cell + "')");
                db.update(c, "ALTER TABLE cell_probe.cell_records ENABLE ROW LEVEL SECURITY");
                db.update(c, "ALTER TABLE cell_probe.cell_records FORCE ROW LEVEL SECURITY");
                c.commit();
            } catch (Exception | AssertionError failure) {
                c.rollback();
                throw failure;
            }
        }
    }

    private void removeOwnedCellFixture(boolean cellA) throws SQLException {
        try (Connection c = db.connect("postgres", cellA ? DATABASE : "v3024_probe_b")) {
            c.setAutoCommit(false);
            try {
                db.update(c, "DROP TABLE cell_probe.cell_records");
                db.update(c, "DROP SCHEMA cell_probe");
                c.commit();
            } catch (SQLException failure) {
                c.rollback();
                throw failure;
            }
        }
    }

    private void connectionDenied(String login, String database, String state) throws SQLException {
        try (Connection unexpected = db.connect(login, database)) {
            throw new AssertionError("Unauthorized database CONNECT accepted: " + login + " -> " + database
                    + "; autoCommit=" + unexpected.getAutoCommit());
        } catch (SQLException exception) {
            equal(state(exception), state, "Connection denial SQLSTATE");
        }
    }

    private void db13() {
        scenario("DB13", "Stale, revoked or conflicting Context fails closed");
        check("Current trusted mapping positive and conflicting caller Context", "Existing active A SESSION_USER mapping",
                "Current mapping works; caller's conflicting custom Context cannot read/write B", () -> transactions.rollback(LOGIN_A, c -> {
                    visibility(c, LOGIN_A, A);
                    db.scalar(c, "SELECT set_config('app.tenant_id',?,true)", B);
                    visibility(c, LOGIN_A, A);
                    db.denied(c, "INSERT INTO isolation.tenant_records(id,tenant_id,payload) OVERRIDING SYSTEM VALUE VALUES (?,?::uuid,?)",
                            "42501", FIXTURE_ID, B, "forbidden-conflict");
                }));
        db13Case(1, "Active membership and CRUD", f -> {
            for (String login : List.of(LOGIN_A, LOGIN_B)) {
                try (var u = new TenantUnitOfWork.MembershipUnit(db, f, 0, login)) {
                    var e = u.refresh();
                    db13Positive(u, e, f.schema);
                    u.execute(e, s -> {
                        equal(s.update("INSERT INTO " + f.schema + ".records VALUES(-13,?::uuid,'positive')", e.tenant()), 1, "Authorized INSERT");
                        equal(s.update("UPDATE " + f.schema + ".records SET payload='updated' WHERE id=-13"), 1, "Authorized UPDATE");
                        equal(s.update("DELETE FROM " + f.schema + ".records WHERE id=-13"), 1, "Authorized DELETE");
                        s.complete();
                    });
                    db13Denied("42501", () -> u.execute(e, s -> s.update("INSERT INTO " + f.schema + ".records VALUES(-14,?::uuid,'forbidden')", e.tenant().equals(A) ? B : A)));
                }
            }
        });
        db13Case(2, "Revoked membership denies new connections", f -> {
            try (var u = new TenantUnitOfWork.MembershipUnit(db, f, 0, LOGIN_A)) {
                var e = u.refresh(); db13Positive(u, e, f.schema);
                f.request("local-revoke"); f.revoke(0, LOGIN_A, "local-revoke");
                db13Denied("42501", () -> u.execute(e, s -> { throw new AssertionError("Revoked callback reached"); }));
                try (var fresh = new TenantUnitOfWork.MembershipUnit(db, f, 0, LOGIN_A)) {
                    db13Denied("42501", fresh::refresh);
                }
            }
        });
        db13Case(3, "Suspension, expiry, user/Tenant disable and policy changes", f -> {
            try (var u = new TenantUnitOfWork.MembershipUnit(db, f, 0, LOGIN_A)) {
                for (String change : List.of("status='SUSPENDED'", "status='EXPIRED'", "user_enabled=false", "tenant_enabled=false", "policy_enabled=false")) {
                    var e = u.refresh(); db13Positive(u, e, f.schema);
                    f.mutate(0, "UPDATE " + f.schema + ".members SET " + change + ",epoch=epoch+1 WHERE principal=?", LOGIN_A);
                    db13Denied("42501", () -> u.execute(e, s -> { throw new AssertionError("Disabled callback reached"); }));
                    f.mutate(0, "UPDATE " + f.schema + ".members SET status='ACTIVE',user_enabled=true,tenant_enabled=true,policy_enabled=true,epoch=epoch+1 WHERE principal=?", LOGIN_A);
                }
                var e = u.refresh();
                f.mutate(0, "UPDATE " + f.schema + ".source SET clock=200 WHERE id");
                db13Denied("42501", () -> u.execute(e, s -> { throw new AssertionError("Admission at expiry E"); }));
                f.mutate(0, "UPDATE " + f.schema + ".members SET valid_until=300,epoch=epoch+1 WHERE principal=?", LOGIN_A);
                db13Positive(u, u.refresh(), f.schema);
            }
        });
        db13Case(4, "Revocation on live pooled PID and cache invalidation", f -> {
            try (var u = new TenantUnitOfWork.MembershipUnit(db, f, 0, LOGIN_A)) {
                var e = u.refresh(); db13Positive(u, e, f.schema); String pid = u.inspectPid();
                f.revoke(0, LOGIN_A, "pool-revoke");
                db13Denied("42501", () -> u.execute(e, s -> { throw new AssertionError("Pool retained permission"); }));
                require(u.cached() == null, "Denied cached decision invalidated");
                equal(u.inspectPid(), pid, "Healthy physical connection survives but authority does not");
            }
        });
        db13Case(5, "Subsequent transactions on same connection", f -> {
            try (var u = new TenantUnitOfWork.MembershipUnit(db, f, 0, LOGIN_A)) {
                var e = u.refresh(); var xid = new String[1];
                u.execute(e, s -> { xid[0] = s.scalar("SELECT txid_current()"); s.complete(); });
                String pid = u.inspectPid(); f.revoke(0, LOGIN_A, "between-transactions");
                db13Denied("42501", () -> u.execute(e, s -> { throw new AssertionError("Old transaction binding reused"); }));
                equal(u.inspectPid(), pid, "Same PID after transaction boundary denial");
                try (Connection c = db.connect("postgres")) {
                    equal(db.scalar(c, "SELECT count(*) FROM " + f.schema + ".leases WHERE pid=? AND xid=?", Integer.valueOf(pid), Long.valueOf(xid[0])), "1", "Old committed lease exists but cannot authorize a new transaction");
                }
            }
        });
        db13Case(6, "Direct SQL, forged context and replay cannot authorize", f -> {
            try (var u = new TenantUnitOfWork.MembershipUnit(db, f, 0, LOGIN_A)) {
                var e = u.refresh();
                for (var forged : List.of(
                        new TenantUnitOfWork.MembershipEvidence(e.membershipEpoch()+1, e.authorityEpoch(), A, e.cell(), 1),
                        new TenantUnitOfWork.MembershipEvidence(e.membershipEpoch(), e.authorityEpoch()+1, A, e.cell(), 1),
                        new TenantUnitOfWork.MembershipEvidence(e.membershipEpoch(), e.authorityEpoch(), B, e.cell(), 1),
                        new TenantUnitOfWork.MembershipEvidence(e.membershipEpoch(), e.authorityEpoch(), A, "cell-b", 1),
                        new TenantUnitOfWork.MembershipEvidence(e.membershipEpoch(), e.authorityEpoch(), A, e.cell(), 2))) {
                    db13Denied("42501", () -> u.execute(forged, s -> { throw new AssertionError("Forged admission"); }));
                }
                try (Connection raw = db.connect(LOGIN_A)) {
                    raw.setAutoCommit(false);
                    db.scalar(raw, "SELECT set_config('app.tenant_id',?,true)", A);
                    db.scalar(raw, "SELECT set_config('app.request_context','forged-membership-epoch-1',false)");
                    db.denied(raw, "SELECT * FROM " + f.schema + ".records", "42501");
                    db.denied(raw, "SELECT * FROM " + f.schema + ".members", "42501");
                    db.denied(raw, "UPDATE " + f.schema + ".fence SET tick=999", "42501");
                    db.denied(raw, "INSERT INTO " + f.schema + ".leases VALUES(pg_backend_pid(),txid_current(),SESSION_USER,?::uuid,1)", "42501", A);
                    db.denied(raw, "SET ROLE " + f.guard(0), "42501");
                    db.denied(raw, "SELECT " + f.schema + ".admit(1,1,?::uuid,NULL,1)", "42501", A);
                    raw.rollback();
                }
                f.revoke(0, LOGIN_A, "replay");
                db13Denied("42501", () -> u.execute(e, s -> { throw new AssertionError("Replay restored access"); }));
            }
        });
        db13Case(7, "Restoration requires a new epoch and fresh authorization", f -> {
            try (var u = new TenantUnitOfWork.MembershipUnit(db, f, 0, LOGIN_A)) {
                var old = u.refresh(); f.revoke(0, LOGIN_A, "restore");
                db13Denied("42501", () -> u.execute(old, s -> { throw new AssertionError("Revoked permit"); }));
                f.mutate(0, "UPDATE " + f.schema + ".members SET status='ACTIVE',epoch=epoch+1 WHERE principal=?", LOGIN_A);
                db13Denied("42501", () -> u.execute(old, s -> { throw new AssertionError("Old allow restored"); }));
                var fresh = u.refresh(); require(fresh.membershipEpoch() > old.membershipEpoch(), "Monotonic restored version");
                db13Positive(u, fresh, f.schema);
                f.mutate(0, "UPDATE " + f.schema + ".members SET policy_enabled=false,epoch=epoch+1 WHERE principal=?", LOGIN_A);
                db13Denied("42501", u::refresh);
            }
        });
        db13Case(8, "Unavailable, stale, conflicting and rolled-back authority", f -> {
            try (var u = new TenantUnitOfWork.MembershipUnit(db, f, 0, LOGIN_A)) {
                var e = u.refresh(); db13Positive(u, e, f.schema);
                for (String change : List.of("available=false", "available=true,head=2,applied=1", "head=2,applied=2,conflict=true", "head=1,applied=1,conflict=false")) {
                    f.mutate(0, "UPDATE " + f.schema + ".source SET " + change + " WHERE id");
                    db13Denied("55000", () -> u.execute(e, s -> { throw new AssertionError("Stale cached allow"); }));
                    require(u.cached() == null, "Freshness failure invalidates positive cache");
                }
                f.mutate(0, "UPDATE " + f.schema + ".source SET available=true,head=2,applied=2,conflict=false WHERE id");
                db13Positive(u, u.refresh(), f.schema);
                // Actual inaccessible authority, not only an unavailable Boolean.
                try (Connection c = db.connect("postgres")) { db.update(c, "REVOKE SELECT ON " + f.schema + ".source FROM " + f.guard(0)); }
                db13Denied("55000", u::refresh);
                try (Connection c = db.connect("postgres")) { db.update(c, "GRANT SELECT ON " + f.schema + ".source TO " + f.guard(0)); }
                db13Positive(u, u.refresh(), f.schema);
                f.mutate(0, "UPDATE " + f.schema + ".members SET epoch=epoch+1 WHERE principal=?", LOGIN_A);
                f.mutate(0, "UPDATE " + f.schema + ".members SET epoch=1 WHERE principal=?", LOGIN_A);
                db13Denied("55000", u::refresh);
            }
        });
        db13Case(9, "Concurrent revocation/access serialized in both orders", f -> db13Race(f, false));
        db13Case(10, "Long transaction drain/rollback and isolation rejection", f -> {
            for (int isolation : List.of(Connection.TRANSACTION_REPEATABLE_READ, Connection.TRANSACTION_SERIALIZABLE)) {
                // Hikari may discard 0A000 connections; cleanup then closes the exclusive pool fail-closed.
                // Independently prove each server rejection rather than issuing SQL through a closed pool.
                try (var u = new TenantUnitOfWork.MembershipUnit(db, f, 0, LOGIN_A)) {
                    var e = u.refresh();
                    db13Denied("0A000", () -> u.execute(e, isolation, s -> { throw new AssertionError("Unsupported isolation admitted"); }));
                }
            }
            try (var recovered = new TenantUnitOfWork.MembershipUnit(db, f, 0, LOGIN_A)) {
                db13Positive(recovered, recovered.refresh(), f.schema);
            }
            db13Race(f, true);
        });
        db13Case(11, "Independent Cells and requested/local/global revocation", f -> {
            try (var a = new TenantUnitOfWork.MembershipUnit(db, f, 0, CELL_A_RUNTIME);
                    var b = new TenantUnitOfWork.MembershipUnit(db, f, 1, CELL_B_RUNTIME)) {
                var ea = a.refresh(); var eb = b.refresh(); db13Positive(a, ea, f.schema); db13Positive(b, eb, f.schema);
                db13Denied("42501", () -> b.execute(ea, s -> { throw new AssertionError("Cross-Cell evidence forwarded"); }));
                db13Denied("42501", () -> { try (Connection ignored = db.connect(CELL_A_RUNTIME, f.database(1))) { require(!ignored.isClosed(), "Connection positive"); } });
                f.request("tenant-b-operation"); require(!f.complete("tenant-b-operation"), "Requested is not globally complete");
                f.revoke(0, LOGIN_B, "tenant-b-operation");
                require(!f.complete("tenant-b-operation"), "One local R is not global G");
                db13Positive(b, eb, f.schema);
                f.revoke(1, CELL_B_RUNTIME, "tenant-b-operation");
                db13Denied("42501", () -> b.execute(eb, s -> { throw new AssertionError("Cell B revoke ignored"); }));
                require(f.complete("tenant-b-operation"), "Both independently acknowledged local fences complete this fixed two-Cell operation");
                db13Positive(a, ea, f.schema);
            }
        });
        db13Case(12, "Failure injection, rollback and disposable cleanup", f -> {
            String roles;
            try (Connection c = db.connect("postgres")) { roles = db.scalar(c, "SELECT count(*) FROM pg_roles WHERE rolname LIKE 'v3024_db13_%'"); }
            try {
                try (var unexpected = db.new MembershipFixture(true)) { throw new AssertionError("Setup fault not injected: " + unexpected.schema); }
            } catch (IllegalStateException expected) { equal(expected.getMessage(), "DB13_SETUP_FAULT", "Intended partial setup failure"); }
            try (Connection c = db.connect("postgres")) { equal(db.scalar(c, "SELECT count(*) FROM pg_roles WHERE rolname LIKE 'v3024_db13_%'"), roles, "Failed setup leaked no owned roles"); }
            try (var u = new TenantUnitOfWork.MembershipUnit(db, f, 0, LOGIN_A)) {
                var e = u.refresh();
                try {
                    u.execute(e, s -> { s.update("UPDATE " + f.schema + ".records SET payload='must-rollback' WHERE id=1"); throw new IllegalStateException("DB13_APP_FAULT"); });
                    throw new AssertionError("Application fault swallowed");
                } catch (IllegalStateException expected) { equal(expected.getMessage(), "DB13_APP_FAULT", "Original application fault"); }
                try {
                    u.execute(e, s -> s.update("UPDATE " + f.schema + ".records SET payload='incomplete' WHERE id=1"));
                    throw new AssertionError("Incomplete work committed");
                } catch (TenantUnitOfWork.IncompleteRequest expected) { require(u.cached() == null, "Incomplete work invalidates cached permit"); }
                db13Denied("22012", () -> u.execute(e, s -> s.scalar("SELECT 1/0")));
                for (String status : List.of("REVOKED", "ACTIVE")) {
                    db13Denied("23502", () -> f.mutate(0, "UPDATE " + f.schema + ".members SET status=?,epoch=NULL WHERE principal=?", status, LOGIN_A));
                    db13Positive(u, u.refresh(), f.schema);
                }
            }
        });
    }

    @FunctionalInterface
    private interface MembershipCheck { void run(ProbeDatabase.MembershipFixture fixture) throws Exception; }

    private void db13Case(int number, String description, MembershipCheck action) {
        check("D13-%02d %s".formatted(number, description), "Unique service-owned two-Cell fixtures; current nonprivileged TCP identities",
                "Approved DB13 contract; deterministic barriers; no unauthorized effects; full owned-resource cleanup", () -> {
                    try (var f = db.membershipFixture()) {
                        String a = f.fingerprint(0); String b = f.fingerprint(1);
                        try { action.run(f); }
                        finally {
                            equal(f.fingerprint(0), a, "Independent Cell A fixture data unchanged");
                            equal(f.fingerprint(1), b, "Independent Cell B fixture data unchanged");
                            emit(Map.of("type", "db13-effects", "id", "DB13", "repeat", db.repetition, "case", number, "unauthorizedEffects", 0, "basis", "Real denials and independent unchanged two-Cell data snapshots"));
                        }
                    }
                });
    }

    private void db13Positive(TenantUnitOfWork.MembershipUnit unit, TenantUnitOfWork.MembershipEvidence evidence, String schema) throws Exception {
        unit.execute(evidence, s -> {
            equal(s.scalar("SELECT count(*) FROM " + schema + ".records WHERE tenant=?::uuid", evidence.tenant()), "1", "Nonempty authorized positive control");
            equal(s.scalar("SELECT count(*) FROM " + schema + ".records WHERE tenant<>?::uuid", evidence.tenant()), "0", "Other Tenant filtered");
            s.complete();
        });
    }

    private void db13Denied(String expectedState, Check operation) throws Exception {
        try { operation.run(); throw new AssertionError("Expected SQLSTATE " + expectedState + " was not raised"); }
        catch (SQLException expected) {
            equal(state(expected), expectedState, "Precise DB13 SQLSTATE");
            emit(Map.of("type", "db13-denial", "id", "DB13", "repeat", db.repetition, "sqlstate", state(expected), "category",
                    expectedState.equals("55000") ? "AUTHZ_FRESHNESS_UNAVAILABLE" : expectedState.equals("0A000") ? "AUTHZ_ISOLATION_UNSUPPORTED" : expectedState.equals("42501") ? "AUTHZ_DENIED_OR_CONTEXT_INVALID" : "INTENTIONAL_EXECUTION_FAULT"));
        }
    }

    private void db13Race(ProbeDatabase.MembershipFixture f, boolean abort) throws Exception {
        var admitted = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var revokerReady = new java.util.concurrent.CountDownLatch(1);
        var accessorPid = new java.util.concurrent.atomic.AtomicInteger();
        var revokerPid = new java.util.concurrent.atomic.AtomicInteger();
        f.request(abort ? "drain-abort" : "drain-commit");
        try (var unit = new TenantUnitOfWork.MembershipUnit(db, f, 0, LOGIN_A);
                var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var evidence = unit.refresh();
            var access = workers.submit(() -> {
                try {
                    unit.execute(evidence, s -> {
                        accessorPid.set(Integer.parseInt(s.scalar("SELECT pg_backend_pid()")));
                        if (abort) { s.update("UPDATE " + f.schema + ".records SET payload='abort-before-R' WHERE id=1"); }
                        admitted.countDown();
                        require(release.await(20, java.util.concurrent.TimeUnit.SECONDS), "Controller releases admitted transaction");
                        equal(s.scalar("SELECT count(*) FROM " + f.schema + ".records"), "1", "Old lease can read only before R");
                        if (abort) { throw new IllegalStateException("DB13_DRAIN_ABORT"); }
                        s.complete();
                    });
                    require(!abort, "Abort scenario cannot commit");
                } catch (IllegalStateException expected) { require(abort && expected.getMessage().equals("DB13_DRAIN_ABORT"), "Only intended drain abort"); }
                return null;
            });
            try {
                require(admitted.await(15, java.util.concurrent.TimeUnit.SECONDS), "Admission barrier established");
                var revoke = workers.submit(() -> {
                    try (Connection c = db.connect("postgres")) {
                        c.setAutoCommit(false);
                        try {
                            db.update(c, "SET LOCAL ROLE " + f.owner(0));
                            revokerPid.set(Integer.parseInt(db.scalar(c, "SELECT pg_backend_pid()")));
                            revokerReady.countDown();
                            equal(db.update(c, "UPDATE " + f.schema + ".fence SET tick=tick+1 WHERE id"), 1, "Revoker acquired incompatible fence");
                            equal(db.update(c, "UPDATE " + f.schema + ".members SET status='REVOKED',epoch=epoch+1 WHERE principal=?", LOGIN_A), 1, "Revocation update");
                            db.update(c, "UPDATE " + f.schema + ".membership_heads SET epoch=epoch+1 WHERE principal=?", LOGIN_A);
                            c.commit();
                            emit(Map.of("type", "revocation", "id", "DB13", "repeat", db.repetition, "phase", "LOCALLY_EFFECTIVE", "ordering", "AFTER_OLD_TRANSACTION_END", "abort", abort));
                        } catch (Exception | Error failure) { c.rollback(); throw failure; }
                    }
                    return null;
                });
                require(revokerReady.await(10, java.util.concurrent.TimeUnit.SECONDS), "Revoker PID barrier");
                boolean blocked = false;
                long watchdog = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
                try (Connection observer = db.connect("postgres")) {
                    while (!blocked && System.nanoTime() < watchdog) {
                        blocked = db.scalar(observer, "SELECT (? = ANY(pg_blocking_pids(?)))::text", accessorPid.get(), revokerPid.get()).equals("true");
                    }
                    require(blocked, "Actual PostgreSQL blocking edge, not elapsed time, proves drain");
                    require(!revoke.isDone(), "No R acknowledgement while old transaction owns fence");
                    equal(db.scalar(observer, "SELECT status FROM " + f.schema + ".members WHERE principal=?", LOGIN_A), "ACTIVE", "Pending intent is not effective revocation");
                    emit(Map.of("type", "db13-ordering", "id", "DB13", "repeat", db.repetition, "stage", "REVOCATION_BLOCKED_BY_ADMITTED_TX", "holderPid", accessorPid.get(), "revokerPid", revokerPid.get(), "abort", abort));
                }
                release.countDown();
                access.get(20, java.util.concurrent.TimeUnit.SECONDS);
                revoke.get(20, java.util.concurrent.TimeUnit.SECONDS);
                db13Denied("42501", () -> unit.execute(evidence, s -> { throw new AssertionError("Post-R old authorization"); }));
                try (var afterR = new TenantUnitOfWork.MembershipUnit(db, f, 0, LOGIN_A)) { db13Denied("42501", afterR::refresh); }
            } finally { release.countDown(); }
        }
    }

    private void db14() {
        scenario("DB14", "Reconnection, credential revocation and disposal");
        check("Real credential expiry, NOLOGIN, existing sessions and targeted termination", "Only fresh task-owned role v3024_formal_revocation; existing roles/data unchanged",
                "Valid login works; bad/expired credentials fail; NOLOGIN does not terminate old session; explicit targeted termination does; task role removed", () -> {
                    String role = "v3024_formal_revocation";
                    byte[] entropy = new byte[32];
                    new SecureRandom().nextBytes(entropy);
                    String password = Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
                    try (Connection administrator = db.connect("postgres")) {
                        if (!db.scalar(administrator, "SELECT count(*) FROM pg_roles WHERE rolname=?", role).equals("0")) {
                            throw new Blocked("Task-owned role name already exists; will not modify or drop an existing role");
                        }
                        boolean created = false;
                        try {
                            db.secretSql(administrator, role, password);
                            created = true;
                            db.update(administrator, "GRANT CONNECT ON DATABASE v3024_probe_a TO v3024_formal_revocation");
                            try (Connection live = db.authenticate(role, password, DATABASE)) {
                                equal(db.scalar(live, "SELECT 1"), "1", "Positive real-authentication control");
                                String pid = db.scalar(live, "SELECT pg_backend_pid()");
                                authenticationDenied(role, password + "-incorrect", "28P01");
                                db.update(administrator, "ALTER ROLE v3024_formal_revocation VALID UNTIL '2000-01-01T00:00:00Z'");
                                authenticationDenied(role, password, "28P01");
                                equal(db.scalar(live, "SELECT 1"), "1", "Expiry is not existing-session termination");
                                db.update(administrator, "ALTER ROLE v3024_formal_revocation VALID UNTIL 'infinity'");
                                try (Connection positive = db.authenticate(role, password, DATABASE)) {
                                    equal(db.scalar(positive, "SELECT 1"), "1", "Reconnection positive control");
                                }
                                db.update(administrator, "ALTER ROLE v3024_formal_revocation NOLOGIN");
                                authenticationDenied(role, password, "28000");
                                db.update(administrator, "REVOKE CONNECT ON DATABASE v3024_probe_a FROM v3024_formal_revocation");
                                equal(db.scalar(live, "SELECT 1"), "1", "NOLOGIN/CONNECT revocation alone leaves existing session alive");
                                equal(db.scalar(administrator, "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE pid=? AND usename=?",
                                        Integer.valueOf(pid), role), "t", "Terminate only owned role/session");
                                try {
                                    db.scalar(live, "SELECT 1");
                                    throw new AssertionError("Terminated session still accepted SQL");
                                } catch (SQLException expected) {
                                    require(List.of("57P01", "08006", "08003").contains(state(expected)), "Expected terminated-connection SQLSTATE");
                                }
                            }
                        } finally {
                            if (created) {
                                db.inventory(administrator, "SELECT pid,usename FROM pg_stat_activity WHERE usename='v3024_formal_revocation' ORDER BY pid");
                                db.inventory(administrator, "SELECT pg_terminate_backend(pid) AS terminated FROM pg_stat_activity WHERE usename='v3024_formal_revocation'");
                                db.update(administrator, "REVOKE CONNECT ON DATABASE v3024_probe_a FROM v3024_formal_revocation");
                                db.update(administrator, "DROP ROLE v3024_formal_revocation");
                                equal(db.scalar(administrator, "SELECT count(*) FROM pg_roles WHERE rolname=?", role), "0", "Task-owned role disposed");
                            }
                        }
                    }
                });
        check("Existing runtime reconnects after test cleanup", "No changes to pre-existing runtime roles",
                "A/B independently authenticate and still see only their own data", () -> {
                    try (Connection a = db.connect(LOGIN_A); Connection b = db.connect(LOGIN_B)) {
                        visibility(a, LOGIN_A, A);
                        visibility(b, LOGIN_B, B);
                    }
                });
    }

    private void authenticationDenied(String role, String password, String expectedState) throws SQLException {
        try (Connection unexpected = db.authenticate(role, password, DATABASE)) {
            throw new AssertionError("Invalid/revoked authentication accepted; autoCommit=" + unexpected.getAutoCommit());
        } catch (SQLException exception) {
            equal(state(exception), expectedState, "Real authentication denial SQLSTATE");
        }
    }
}
