\set ON_ERROR_STOP on
CREATE SCHEMA placement_state;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE TABLE placement_state.placements (
 tenant_id text PRIMARY KEY,cell_id text NOT NULL,deployment_id text NOT NULL,
 generation bigint NOT NULL,revision bigint NOT NULL,state text NOT NULL,
 incarnation text NOT NULL
);
CREATE TABLE placement_state.handoffs (
 transition_id text PRIMARY KEY,tenant_id text NOT NULL,source_cell text NOT NULL,
 target_cell text NOT NULL,state text NOT NULL,source_fence_ack text,target_ack text
);
CREATE TABLE placement_state.documents (fixture_id text NOT NULL,tenant_id text NOT NULL,body text NOT NULL,PRIMARY KEY(fixture_id,tenant_id));
GRANT USAGE ON SCHEMA placement_state TO xref_placement;
GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA placement_state TO xref_placement;
