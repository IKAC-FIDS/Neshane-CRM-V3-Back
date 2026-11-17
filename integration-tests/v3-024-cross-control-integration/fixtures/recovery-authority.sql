\set ON_ERROR_STOP on
CREATE SCHEMA completion_state;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE TABLE completion_state.checkpoints (
 fixture_id text NOT NULL,tenant_id text NOT NULL,revision bigint NOT NULL,
 checkpoint bigint NOT NULL,incarnation text NOT NULL,PRIMARY KEY(fixture_id,tenant_id)
);
CREATE TABLE completion_state.completions (
 fixture_id text NOT NULL,tenant_id text NOT NULL,operation_id text NOT NULL,
 sequence bigint NOT NULL,semantic_digest text NOT NULL,owner_receipt text NOT NULL,
 owner_signature text NOT NULL,PRIMARY KEY(fixture_id,tenant_id,operation_id),
 UNIQUE(fixture_id,tenant_id,sequence)
);
CREATE TABLE completion_state.grants (
 grant_id text PRIMARY KEY,body text NOT NULL,signature text NOT NULL,
 state text NOT NULL,consumed_attempt text
);
CREATE TABLE completion_state.acknowledgements (
 attempt_id text PRIMARY KEY,body text NOT NULL,signature text NOT NULL
);
CREATE TABLE completion_state.documents (fixture_id text NOT NULL,tenant_id text NOT NULL,body text NOT NULL,PRIMARY KEY(fixture_id,tenant_id));
GRANT USAGE ON SCHEMA completion_state TO xref_recovery;
GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA completion_state TO xref_recovery;
