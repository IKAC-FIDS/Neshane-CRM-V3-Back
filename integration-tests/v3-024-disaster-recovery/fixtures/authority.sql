\set ON_ERROR_STOP on
CREATE SCHEMA authority;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE TABLE authority.current_state (
 tenant_id text PRIMARY KEY, cell_id text NOT NULL, placement_epoch bigint NOT NULL,
 credential_epoch bigint NOT NULL, fence_generation bigint NOT NULL,
 version bigint NOT NULL, suspended boolean NOT NULL DEFAULT false
);
CREATE TABLE authority.completions (
 tenant_id text NOT NULL, operation_id text NOT NULL, semantic_digest text NOT NULL,
 sequence bigint NOT NULL, payload text, outcome text NOT NULL,
 PRIMARY KEY (tenant_id,operation_id), UNIQUE(tenant_id,sequence)
);
CREATE TABLE authority.recovery_grants (
 grant_id text PRIMARY KEY, body text NOT NULL, signature text NOT NULL,
 consumed_by text, state text NOT NULL CHECK(state IN ('ISSUED','CONSUMED','REVOKED'))
);
CREATE TABLE authority.activations (
 tenant_id text PRIMARY KEY, attempt_id text NOT NULL, destination_id text NOT NULL,
 incarnation text NOT NULL, version bigint NOT NULL, state text NOT NULL
);
INSERT INTO authority.current_state VALUES ('tenant-a','cell-a',1,1,1,1,false),('tenant-b','cell-b',1,1,1,1,false);
GRANT USAGE ON SCHEMA authority TO dr_authority;
GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA authority TO dr_authority;
