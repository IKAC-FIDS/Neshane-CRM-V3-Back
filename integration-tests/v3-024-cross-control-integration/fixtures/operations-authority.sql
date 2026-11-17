\set ON_ERROR_STOP on
CREATE SCHEMA approval_state;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE TABLE approval_state.policies (
 tenant_id text PRIMARY KEY,revision bigint NOT NULL,state text NOT NULL,
 incarnation text NOT NULL
);
CREATE TABLE approval_state.approvals (
 approval_id text PRIMARY KEY,fixture_id text NOT NULL,tenant_id text NOT NULL,
 principal_id text NOT NULL,operation_id text NOT NULL,semantic_digest text NOT NULL,
 body text NOT NULL,signature text NOT NULL,state text NOT NULL
);
CREATE TABLE approval_state.documents (fixture_id text NOT NULL,tenant_id text NOT NULL,body text NOT NULL,PRIMARY KEY(fixture_id,tenant_id));
GRANT USAGE ON SCHEMA approval_state TO xref_operations;
GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA approval_state TO xref_operations;
