\set ON_ERROR_STOP on
CREATE SCHEMA recovery;
CREATE TABLE recovery.gate (
 singleton boolean PRIMARY KEY CHECK(singleton),tenant_id text NOT NULL,fixture_id text NOT NULL DEFAULT 'UNASSIGNED',
 cell_id text NOT NULL,deployment_id text NOT NULL,incarnation text NOT NULL,
 state text NOT NULL CHECK(state IN ('QUARANTINED','VALIDATING','RECONCILING','ACTIVE')),
 acknowledged boolean NOT NULL DEFAULT false,fence bigint NOT NULL DEFAULT 0,
 identity_revision bigint NOT NULL DEFAULT 0,placement_revision bigint NOT NULL DEFAULT 0,
 operations_revision bigint NOT NULL DEFAULT 0,recovery_revision bigint NOT NULL DEFAULT 0,
 attempt_id text,data_digest text
);
INSERT INTO recovery.gate(singleton,tenant_id,cell_id,deployment_id,incarnation,state)
 VALUES(true,:'tenant',:'cell',:'deployment',:'incarnation','QUARANTINED');
CREATE TABLE recovery.audit (
 event_id text PRIMARY KEY,fixture_id text NOT NULL,tenant_id text NOT NULL,
 operation_id text,attempt_id text,category text NOT NULL,state text NOT NULL,
 semantic_digest text,revision_vector text NOT NULL
);
CREATE TABLE recovery.fence_acknowledgements (
 transition_id text PRIMARY KEY,fence bigint NOT NULL,scope text NOT NULL,
 revision_vector text NOT NULL,incarnation text NOT NULL,
 completion_frontier bigint NOT NULL CHECK(completion_frontier>=0),
 pending_completions bigint NOT NULL CHECK(pending_completions>=0)
);
CREATE TABLE recovery.attempts (
 attempt_id text PRIMARY KEY,fixture_id text NOT NULL,tenant_id text NOT NULL,
 backup_digest text NOT NULL,validated_digest text NOT NULL,
 reconstructed_digest text,grant_id text NOT NULL,state text NOT NULL,
 revision_vector text NOT NULL,consumption_receipt text NOT NULL
);
GRANT USAGE ON SCHEMA recovery TO :runtime;
GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA recovery TO :runtime;
