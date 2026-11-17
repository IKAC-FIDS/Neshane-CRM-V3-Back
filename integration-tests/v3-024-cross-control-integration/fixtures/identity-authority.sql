\set ON_ERROR_STOP on
CREATE SCHEMA identity_state;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE TABLE identity_state.principals (
 principal_id text PRIMARY KEY,tenant_id text NOT NULL,service_id text NOT NULL,
 credential_epoch bigint NOT NULL,revision bigint NOT NULL,status text NOT NULL,
 fingerprint text NOT NULL,incarnation text NOT NULL
);
CREATE TABLE identity_state.transitions (
 transition_id text PRIMARY KEY,principal_id text NOT NULL,state text NOT NULL,
 revision bigint NOT NULL,required_ack text NOT NULL,observed_ack text
);
CREATE TABLE identity_state.documents (fixture_id text NOT NULL,tenant_id text NOT NULL,body text NOT NULL,PRIMARY KEY(fixture_id,tenant_id));
GRANT USAGE ON SCHEMA identity_state TO xref_identity;
GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA identity_state TO xref_identity;
