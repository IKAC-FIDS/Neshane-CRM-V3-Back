package ir.neshane.isolationprobe;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static ir.neshane.isolationprobe.ProbeDatabase.*;

/** Test-only helpers. Secured DB09 access owns its private pool; no production integration. */
final class TenantUnitOfWork {
    @FunctionalInterface
    interface Action {
        void run(Connection connection) throws Exception;
    }

    private final ProbeDatabase database;

    TenantUnitOfWork(ProbeDatabase database) {
        this.database = database;
    }

    void rollback(String user, Action action) throws Exception {
        try (Connection connection = database.connect(user)) {
            connection.setAutoCommit(false);
            try {
                action.run(connection);
            } finally {
                connection.rollback();
            }
        }
    }

    void pooledRollback(HikariDataSource pool, Action action) throws Exception {
        Connection connection = pool.getConnection();
        try {
            connection.setAutoCommit(false);
            try {
                action.run(connection);
            } finally {
                try {
                    connection.rollback();
                    connection.setAutoCommit(true);
                } catch (Exception cleanupFailure) {
                    pool.evictConnection(connection);
                    throw cleanupFailure;
                }
            }
        } finally {
            connection.close();
        }
    }

    Secured secured(String principal) {
        return new Secured(database, principal);
    }

    @FunctionalInterface
    interface Request {
        void run(RequestSession session) throws Exception;
    }

    static final class IncompleteRequest extends Exception {
        private static final long serialVersionUID = 1L;

        IncompleteRequest() { super("Logical request did not declare completion; rolled back"); }
    }

    static final class ContextViolation extends Exception {
        private static final long serialVersionUID = 1L;

        ContextViolation() { super("Request-local metadata changed unexpectedly; rolled back"); }
    }

    static final class CleanupFailure extends SQLException {
        private static final long serialVersionUID = 1L;

        CleanupFailure(SQLException cause) {
            super("Secured connection cleanup failed; connection evicted; never automatically retry committed work",
                    cause.getSQLState(), cause);
        }
    }

    record IdleState(String pid, String principal, String currentRole, String tenant,
                     String requestContext, String tenantSetting, String searchPath, boolean autoCommit) {}

    /** A scoped value/SQL view, not a Connection or a pool handle. */
    static final class RequestSession {
        private final ProbeDatabase db;
        private final Connection connection;
        private final long thread = Thread.currentThread().threadId();
        private volatile boolean active = true;
        private boolean completed;

        private RequestSession(ProbeDatabase db, Connection connection) {
            this.db = db;
            this.connection = connection;
        }

        private void guard() {
            if (!active || completed || thread != Thread.currentThread().threadId()) {
                throw new IllegalStateException("Request lease closed, completed or used from another thread");
            }
        }

        private void sqlShape(String sql, boolean read) {
            guard();
            String normalized = sql.stripLeading().toUpperCase(Locale.ROOT);
            // Trusted test SQL only, not a general SQL sandbox. Transaction control is never delegated.
            if (sql.contains(";") || !(read ? normalized.startsWith("SELECT ")
                    : normalized.startsWith("INSERT ") || normalized.startsWith("UPDATE ") || normalized.startsWith("DELETE "))) {
                throw new IllegalArgumentException("Only one trusted SELECT or DML statement; transaction control belongs to Unit of Work");
            }
        }

        String scalar(String sql, Object... parameters) throws SQLException {
            sqlShape(sql, true);
            return db.scalar(connection, sql, parameters);
        }

        int update(String sql, Object... parameters) throws SQLException {
            sqlShape(sql, false);
            return db.update(connection, sql, parameters);
        }

        boolean autoCommit() throws SQLException { guard(); return connection.getAutoCommit(); }

        void complete() { guard(); completed = true; }

        void untrustedSetting(String key, String value, boolean transactionLocal) throws SQLException {
            guard();
            require(List.of("app.tenant_id", "app.request_context", "search_path").contains(key), "Test setting allowlist");
            db.scalar(connection, "SELECT pg_catalog.set_config(?,?,?)", key, value, transactionLocal);
        }

        void rejectRole(String role) throws SQLException {
            guard();
            require(List.of("postgres", "v3024_owner", "v3024_migration", "v3024_runtime",
                    CELL_A_RUNTIME, CELL_B_RUNTIME).contains(role), "Role-negative allowlist");
            db.denied(connection, "SET ROLE " + role, "42501");
        }

        void rejectCrossTenantInsert(String tenant) throws SQLException {
            guard();
            db.denied(connection, "INSERT INTO isolation.tenant_records(id,tenant_id,payload) OVERRIDING SYSTEM VALUE VALUES (?,?::uuid,?)",
                    "42501", FIXTURE_ID - 9, tenant, "db09-forbidden");
        }
    }

    /**
     * Test-only secured contract: private one-connection pool, exclusive leases, fixed
     * authenticated principal, transaction-local metadata, DISCARD ALL on entry/exit.
     * Reflection, unmanaged JDBC and global role/database configuration are outside this contract.
     */
    static final class Secured implements AutoCloseable {
        private final ProbeDatabase db;
        private final String principal;
        private final String tenant;
        private final HikariDataSource pool;
        private final AtomicBoolean terminateAtCleanup = new AtomicBoolean();
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger evictions = new AtomicInteger();
        private final AtomicBoolean closed = new AtomicBoolean();

        private Secured(ProbeDatabase db, String principal) {
            require(List.of(LOGIN_A, LOGIN_B).contains(principal), "Secured pool principal allowlist");
            this.db = db;
            this.principal = principal;
            tenant = principal.equals(LOGIN_A) ? A : B;
            pool = db.pool(principal);
        }

        void execute(String requestContext, Request request) throws Exception {
            if (closed.get()) { throw new IllegalStateException("Secured Unit of Work closed"); }
            require(requestContext != null && !requestContext.isBlank(), "Explicit request metadata required; not Tenant authority");
            active.incrementAndGet();
            Connection c = null;
            RequestSession view = null;
            Throwable failure = null;
            boolean committed = false;
            boolean entered = false;
            boolean discarded = false;
            String pid = "NOT_ACQUIRED";
            try {
                c = pool.getConnection();
                verifyLogin(c);
                pid = db.scalar(c, "SELECT pg_catalog.pg_backend_pid()");
                sanitize(c, pid, "ENTRY");
                entered = true;
                c.setAutoCommit(false);
                db.scalar(c, "SELECT pg_catalog.set_config('app.request_context',?,true)", requestContext);
                view = new RequestSession(db, c);
                event("BEGIN", pid, false, "00000");
                request.run(view);
                if (!view.completed) { throw new IncompleteRequest(); }
                verifyIdentity(c);
                if (!requestContext.equals(db.scalar(c, "SELECT pg_catalog.current_setting('app.request_context',true)"))) {
                    throw new ContextViolation();
                }
                c.commit();
                committed = true;
                event("COMMIT", pid, true, "00000");
            } catch (Exception | Error exception) {
                failure = exception;
            } finally {
                if (view != null) { view.active = false; }
                if (c != null) {
                    try {
                        if (terminateAtCleanup.getAndSet(false)) {
                            // Harness-only real fault, aimed at this currently leased pool connection.
                            try (Connection admin = db.connect("postgres")) {
                                equal(db.scalar(admin, "SELECT pg_catalog.pg_terminate_backend(pid) FROM pg_catalog.pg_stat_activity "
                                        + "WHERE pid=? AND usename=? AND application_name='v3024-formal-isolation'",
                                        Integer.valueOf(pid), principal), "t", "Terminate only our leased connection");
                            }
                            event("OWNED_BACKEND_TERMINATED_FOR_CLEANUP_FAULT", pid, committed, "00000");
                        }
                        if (!c.getAutoCommit()) {
                            c.rollback();
                            if (!committed) { event("ROLLBACK", pid, false, "00000"); }
                            c.setAutoCommit(true);
                        }
                        if (!entered) { throw new SQLException("Entry validation failed; discard lease", "08006"); }
                        sanitize(c, pid, "EXIT");
                    } catch (Exception | Error cleanup) {
                        discardLease(c, cleanup);
                        discarded = true;
                        SQLException sql = cleanup instanceof SQLException s ? s : new SQLException("Cleanup validation failed", "08006", cleanup);
                        CleanupFailure surfaced = new CleanupFailure(sql);
                        event("EVICTED_CLEANUP_FAILURE", pid, committed, state(sql));
                        if (failure == null) { failure = surfaced; } else { failure.addSuppressed(surfaced); }
                    } finally {
                        try { c.close(); }
                        catch (Exception closeFailure) {
                            if (!discarded) { discardLease(c, closeFailure); }
                            SQLException sql = closeFailure instanceof SQLException s ? s
                                    : new SQLException("Lease close failed", "08006", closeFailure);
                            if (failure == null) { failure = new CleanupFailure(sql); }
                            else { failure.addSuppressed(closeFailure); }
                        }
                    }
                }
                active.decrementAndGet();
            }
            if (failure instanceof Error error) { throw error; }
            if (failure != null) { throw (Exception) failure; }
        }

        private void discardLease(Connection c, Throwable original) {
            try {
                pool.evictConnection(c);
            } catch (Exception evictionFailure) {
                original.addSuppressed(evictionFailure);
                // If targeted eviction cannot be established, fail closed for the entire private pool.
                closed.set(true);
                try { pool.close(); }
                catch (Exception poolFailure) { original.addSuppressed(poolFailure); }
            } finally {
                evictions.incrementAndGet();
            }
        }

        private void verifyLogin(Connection c) throws SQLException {
            equal(db.scalar(c, "SELECT SESSION_USER"), principal, "Actual private-pool login");
            equal(db.scalar(c, "SELECT pg_catalog.current_database()"), DATABASE, "Intended authenticated database");
        }

        private void verifyIdentity(Connection c) throws SQLException {
            verifyLogin(c);
            equal(db.scalar(c, "SELECT CURRENT_USER"), principal, "No role escalation");
            equal(db.scalar(c, "SELECT isolation.current_tenant_id()::text"), tenant, "Protected SESSION_USER mapping, never GUC authority");
        }

        private void sanitize(Connection c, String pid, String stage) throws SQLException {
            if (!c.getAutoCommit()) { c.rollback(); c.setAutoCommit(true); }
            emit(Map.of("type", "hygiene-state", "id", "DB09", "repeat", db.repetition, "stage", stage + "_BEFORE_RESET",
                    "backendPid", pid,
                    "requestContextEmpty", db.scalar(c, "SELECT coalesce(pg_catalog.current_setting('app.request_context',true),'')").isEmpty(),
                    "tenantSettingEmpty", db.scalar(c, "SELECT coalesce(pg_catalog.current_setting('app.tenant_id',true),'')").isEmpty(),
                    "searchPath", db.scalar(c, "SELECT pg_catalog.current_setting('search_path')")));
            db.update(c, "DISCARD ALL");
            db.scalar(c, "SELECT pg_catalog.set_config('application_name','v3024-formal-isolation',false)");
            db.scalar(c, "SELECT pg_catalog.set_config('search_path','pg_catalog',false)");
            // Explicit known-context baseline even if a server default were supplied.
            db.scalar(c, "SELECT pg_catalog.set_config('app.request_context','',false)");
            db.scalar(c, "SELECT pg_catalog.set_config('app.tenant_id','',false)");
            verifyIdentity(c);
            assertSafe(snapshot(c));
            equal(db.scalar(c, "SELECT pg_catalog.current_setting('application_name')"),
                    "v3024-formal-isolation", "Owned pool backend marker restored after DISCARD ALL");
            event(stage + "_SANITIZED_AND_VERIFIED", pid, false, "00000");
        }

        private IdleState snapshot(Connection c) throws SQLException {
            return new IdleState(db.scalar(c, "SELECT pg_catalog.pg_backend_pid()"),
                    db.scalar(c, "SELECT SESSION_USER"), db.scalar(c, "SELECT CURRENT_USER"),
                    db.scalar(c, "SELECT isolation.current_tenant_id()::text"),
                    db.scalar(c, "SELECT coalesce(pg_catalog.current_setting('app.request_context',true),'')"),
                    db.scalar(c, "SELECT coalesce(pg_catalog.current_setting('app.tenant_id',true),'')"),
                    db.scalar(c, "SELECT pg_catalog.current_setting('search_path')"), c.getAutoCommit());
        }

        private void assertSafe(IdleState value) {
            equal(value.principal(), principal, "Safe login");
            equal(value.currentRole(), principal, "Safe role");
            equal(value.tenant(), tenant, "Safe trusted mapping");
            equal(value.requestContext(), "", "No leaked request metadata");
            equal(value.tenantSetting(), "", "No leaked caller Tenant setting");
            equal(value.searchPath(), "pg_catalog", "Fixed safe search_path");
            require(value.autoCommit(), "Clean idle auto-commit state");
        }

        IdleState inspectIdle() throws SQLException {
            // Observes without cleaning first: tests cannot mistake a repaired observation for safety.
            try (Connection c = pool.getConnection()) {
                try {
                    IdleState value = snapshot(c);
                    assertSafe(value);
                    event("IDLE_OBSERVED_WITHOUT_REPAIR", value.pid(), false, "00000");
                    return value;
                } catch (SQLException | Error unsafe) {
                    pool.evictConnection(c);
                    evictions.incrementAndGet();
                    throw unsafe;
                }
            }
        }

        void terminateOwnBackendAtNextCleanup() { terminateAtCleanup.set(true); }

        int evictions() { return evictions.get(); }

        private void event(String stage, String pid, boolean committed, String sqlstate) {
            emit(Map.of("type", "lifecycle", "id", "DB09", "repeat", db.repetition,
                    "stage", stage, "backendPid", pid, "principal", principal,
                    "committed", committed, "sqlstate", sqlstate));
        }

        @Override
        public void close() {
            if (active.get() != 0) { throw new IllegalStateException("Cannot close an active secured Unit of Work"); }
            closed.set(true);
            pool.close();
        }
    }

    record MembershipEvidence(long membershipEpoch, long authorityEpoch, String tenant, String cell, long placement) {
        static MembershipEvidence parse(String value) {
            String[] fields = value.split("\\|", -1);
            require(fields.length == 5, "Authority evidence shape");
            return new MembershipEvidence(Long.parseLong(fields[0]), Long.parseLong(fields[1]), fields[2], fields[3], Long.parseLong(fields[4]));
        }
    }

    /** DB13-only pool; authorization lives in protected PostgreSQL leases, never a GUC/cache. */
    static final class MembershipUnit implements AutoCloseable {
        private final ProbeDatabase db;
        private final String schema;
        private final String database;
        private final String principal;
        private final HikariDataSource pool;
        private MembershipEvidence cached;

        MembershipUnit(ProbeDatabase db, ProbeDatabase.MembershipFixture fixture, int cell, String principal) {
            this.db = db;
            schema = fixture.schema;
            database = fixture.database(cell);
            this.principal = principal;
            pool = db.pool(principal, database);
        }

        MembershipEvidence refresh() throws SQLException {
            try (Connection c = pool.getConnection()) {
                baseline(c);
                try {
                    cached = MembershipEvidence.parse(db.scalar(c, "SELECT " + schema + ".evidence()"));
                    return cached;
                } catch (SQLException failure) { cached = null; throw failure; }
            }
        }

        MembershipEvidence cached() { return cached; }

        String inspectPid() throws SQLException {
            try (Connection c = pool.getConnection()) {
                equal(db.scalar(c, "SELECT SESSION_USER"), principal, "DB13 pool principal");
                equal(db.scalar(c, "SELECT coalesce(current_setting('app.request_context',true),'')"), "", "DB13 metadata clean before observation");
                require(c.getAutoCommit(), "DB13 idle auto-commit");
                return db.scalar(c, "SELECT pg_backend_pid()");
            }
        }

        void execute(MembershipEvidence evidence, Request request) throws Exception {
            execute(evidence, Connection.TRANSACTION_READ_COMMITTED, request);
        }

        void execute(MembershipEvidence evidence, int isolation, Request request) throws Exception {
            Connection c = pool.getConnection();
            RequestSession view = null;
            Throwable failure = null;
            boolean discarded = false;
            try {
                baseline(c);
                c.setTransactionIsolation(isolation);
                c.setAutoCommit(false);
                equal(db.scalar(c, "SELECT " + schema + ".admit(?,?,?::uuid,?,?)", evidence.membershipEpoch(), evidence.authorityEpoch(), evidence.tenant(), evidence.cell(), evidence.placement()), "t", "Protected DB admission");
                db.scalar(c, "SELECT set_config('app.request_context','db13-non-authoritative',true)");
                view = new RequestSession(db, c);
                emit(Map.of("type", "db13-lease", "id", "DB13", "repeat", db.repetition, "stage", "ADMITTED", "pid", db.scalar(c, "SELECT pg_backend_pid()"), "xid", db.scalar(c, "SELECT txid_current()"), "principal", principal, "evidence", evidence.toString()));
                request.run(view);
                if (!view.completed) { throw new IncompleteRequest(); }
                c.commit();
                emit(Map.of("type", "db13-lease", "id", "DB13", "repeat", db.repetition, "stage", "COMMITTED", "principal", principal));
            } catch (Exception | Error exception) {
                failure = exception;
                cached = null;
            } finally {
                if (view != null) { view.active = false; }
                try {
                    if (!c.getAutoCommit()) { c.rollback(); c.setAutoCommit(true); }
                    c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    baseline(c);
                } catch (Exception cleanup) {
                    cached = null;
                    discarded = true;
                    // Closing this exclusive pool is fail-closed even if a proxy was already broken.
                    pool.close();
                    if (failure == null) { failure = cleanup; } else { failure.addSuppressed(cleanup); }
                }
                if (!discarded) {
                    try { c.close(); }
                    catch (Exception closeFailure) {
                        pool.close();
                        if (failure == null) { failure = closeFailure; } else { failure.addSuppressed(closeFailure); }
                    }
                }
            }
            if (failure instanceof Error error) { throw error; }
            if (failure != null) { throw (Exception) failure; }
        }

        private void baseline(Connection c) throws SQLException {
            require(c.getAutoCommit(), "Cleanup outside transaction");
            db.update(c, "DISCARD ALL");
            db.scalar(c, "SELECT set_config('application_name','v3024-formal-isolation',false)");
            db.scalar(c, "SELECT set_config('search_path','pg_catalog',false)");
            db.scalar(c, "SELECT set_config('app.request_context','',false)");
            db.scalar(c, "SELECT set_config('app.tenant_id','',false)");
            equal(db.scalar(c, "SELECT SESSION_USER"), principal, "DB13 authenticated principal");
            equal(db.scalar(c, "SELECT CURRENT_USER"), principal, "DB13 no role escalation");
            equal(db.scalar(c, "SELECT current_database()"), database, "DB13 intended database");
        }

        @Override
        public void close() { cached = null; pool.close(); }
    }
}
