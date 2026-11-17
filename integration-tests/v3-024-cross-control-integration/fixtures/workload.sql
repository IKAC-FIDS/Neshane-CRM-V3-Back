\set ON_ERROR_STOP on
CREATE SCHEMA lab_data;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE TABLE lab_data.markers (
 fixture_id text NOT NULL,tenant_id text NOT NULL,marker_id text NOT NULL,
 version bigint NOT NULL,value text NOT NULL,PRIMARY KEY(fixture_id,tenant_id,marker_id)
);
CREATE TABLE lab_data.operations (
 fixture_id text NOT NULL,tenant_id text NOT NULL,operation_id text NOT NULL,
 sequence bigint NOT NULL,semantic_digest text NOT NULL,receipt text NOT NULL,
 PRIMARY KEY(fixture_id,tenant_id,operation_id),UNIQUE(fixture_id,tenant_id,sequence)
);
CREATE TABLE lab_data.effects (
 fixture_id text NOT NULL,tenant_id text NOT NULL,operation_id text NOT NULL,
 category text NOT NULL CHECK(category IN ('HISTORICAL','NEW')),
 semantic_digest text NOT NULL,PRIMARY KEY(fixture_id,tenant_id,operation_id)
);
CREATE TABLE lab_data.outbox (
 fixture_id text NOT NULL,tenant_id text NOT NULL,operation_id text NOT NULL,
 receipt text NOT NULL,acknowledged boolean NOT NULL DEFAULT false,
 PRIMARY KEY(fixture_id,tenant_id,operation_id)
);
ALTER TABLE lab_data.markers ENABLE ROW LEVEL SECURITY;
ALTER TABLE lab_data.markers FORCE ROW LEVEL SECURITY;
ALTER TABLE lab_data.operations ENABLE ROW LEVEL SECURITY;
ALTER TABLE lab_data.operations FORCE ROW LEVEL SECURITY;
ALTER TABLE lab_data.effects ENABLE ROW LEVEL SECURITY;
ALTER TABLE lab_data.effects FORCE ROW LEVEL SECURITY;
ALTER TABLE lab_data.outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE lab_data.outbox FORCE ROW LEVEL SECURITY;
