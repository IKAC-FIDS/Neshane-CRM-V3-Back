\set ON_ERROR_STOP on
CREATE SCHEMA recovery;
CREATE TABLE recovery.gate (
 singleton boolean PRIMARY KEY CHECK(singleton),
 tenant_id text NOT NULL,cell_id text NOT NULL,destination_id text NOT NULL,
 incarnation text NOT NULL,attempt_id text,
 state text NOT NULL CHECK(state IN ('QUARANTINED','VALIDATING','RECONCILING','ACTIVE','BLOCKED')),
 placement_epoch bigint NOT NULL DEFAULT 0,credential_epoch bigint NOT NULL DEFAULT 0,
 fence_generation bigint NOT NULL DEFAULT 0,authority_version bigint NOT NULL DEFAULT 0,
 checkpoint bigint NOT NULL DEFAULT 0,data_digest text,acknowledged boolean NOT NULL DEFAULT false
);
INSERT INTO recovery.gate(singleton,tenant_id,cell_id,destination_id,incarnation,state)
 VALUES(true,:'tenant',:'cell',:'destination',:'incarnation','QUARANTINED');
CREATE TABLE recovery.audit (
 event_id text PRIMARY KEY,attempt_id text NOT NULL,tenant_id text NOT NULL,
 category text NOT NULL CHECK(category IN ('HISTORICAL','RECONSTRUCTION','NEW','DENIAL','STATE')),
 operation_id text,state text NOT NULL,digest text NOT NULL
);
GRANT USAGE ON SCHEMA recovery TO :owner;
GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA recovery TO :owner;
