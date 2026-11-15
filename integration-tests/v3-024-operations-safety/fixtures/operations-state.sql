\set ON_ERROR_STOP on
CREATE SCHEMA oref;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE TABLE oref.meta (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    store_id text NOT NULL, tenant text NOT NULL, cell text NOT NULL,
    pg_start text NOT NULL DEFAULT pg_postmaster_start_time()::text,
    boot_epoch bigint NOT NULL DEFAULT 0, fence bigint NOT NULL DEFAULT 1,
    policy bigint NOT NULL DEFAULT 1, commit_seq bigint NOT NULL DEFAULT 0,
    quarantined boolean NOT NULL DEFAULT false
);
INSERT INTO oref.meta(store_id,tenant,cell) VALUES (:'store',:'tenant',:'cell');
CREATE TABLE oref.grants (
    command_id text PRIMARY KEY, semantic_hash text NOT NULL,
    body text NOT NULL, state text NOT NULL CHECK (state IN ('ACTIVE','REVOKING','REVOKED')),
    epoch bigint NOT NULL DEFAULT 1
);
CREATE TABLE oref.revocations (command_id text PRIMARY KEY, epoch bigint NOT NULL);
CREATE TABLE oref.peer_revocations (fingerprint text PRIMARY KEY);
CREATE TABLE oref.commands (
    command_id text PRIMARY KEY, semantic_hash text NOT NULL,
    tenant text NOT NULL, cell text NOT NULL,
    state text NOT NULL CHECK (state IN ('PREPARED','COMMITTED')),
    outcome text, boot_epoch bigint NOT NULL
);
CREATE TABLE oref.resources (
    resource_id text PRIMARY KEY, tenant text NOT NULL, cell text NOT NULL,
    service text NOT NULL, version bigint NOT NULL DEFAULT 0,
    value text NOT NULL DEFAULT '', digest text NOT NULL DEFAULT ''
);
INSERT INTO oref.resources(resource_id,tenant,cell,service)
VALUES ('marker',:'tenant',:'cell','lab-service'),('config',:'tenant',:'cell','lab-service');
CREATE TABLE oref.effects (
    seq bigint PRIMARY KEY, command_id text NOT NULL UNIQUE,
    tenant text NOT NULL, cell text NOT NULL, resource_id text NOT NULL,
    operation text NOT NULL, semantic_hash text NOT NULL, version bigint NOT NULL,
    digest text NOT NULL
);
CREATE TABLE oref.outbox (
    command_id text PRIMARY KEY, tenant text NOT NULL, cell text NOT NULL,
    outcome text NOT NULL, semantic_hash text NOT NULL
);
ALTER TABLE oref.commands ENABLE ROW LEVEL SECURITY;
ALTER TABLE oref.commands FORCE ROW LEVEL SECURITY;
ALTER TABLE oref.resources ENABLE ROW LEVEL SECURITY;
ALTER TABLE oref.resources FORCE ROW LEVEL SECURITY;
ALTER TABLE oref.effects ENABLE ROW LEVEL SECURITY;
ALTER TABLE oref.effects FORCE ROW LEVEL SECURITY;
ALTER TABLE oref.outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE oref.outbox FORCE ROW LEVEL SECURITY;
CREATE POLICY fixed_scope ON oref.commands USING (tenant=(SELECT tenant FROM oref.meta) AND cell=(SELECT cell FROM oref.meta)) WITH CHECK (tenant=(SELECT tenant FROM oref.meta) AND cell=(SELECT cell FROM oref.meta));
CREATE POLICY fixed_scope ON oref.resources USING (tenant=(SELECT tenant FROM oref.meta) AND cell=(SELECT cell FROM oref.meta)) WITH CHECK (tenant=(SELECT tenant FROM oref.meta) AND cell=(SELECT cell FROM oref.meta));
CREATE POLICY fixed_scope ON oref.effects USING (tenant=(SELECT tenant FROM oref.meta) AND cell=(SELECT cell FROM oref.meta)) WITH CHECK (tenant=(SELECT tenant FROM oref.meta) AND cell=(SELECT cell FROM oref.meta));
CREATE POLICY fixed_scope ON oref.outbox USING (tenant=(SELECT tenant FROM oref.meta) AND cell=(SELECT cell FROM oref.meta)) WITH CHECK (tenant=(SELECT tenant FROM oref.meta) AND cell=(SELECT cell FROM oref.meta));
GRANT USAGE ON SCHEMA oref TO :runtime;
GRANT SELECT ON ALL TABLES IN SCHEMA oref TO :runtime;
GRANT INSERT, UPDATE ON oref.grants,oref.commands,oref.resources,oref.meta TO :runtime;
GRANT INSERT ON oref.effects,oref.outbox,oref.revocations,oref.peer_revocations TO :runtime;
ALTER ROLE :runtime SET search_path='pg_catalog';
