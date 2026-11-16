\set ON_ERROR_STOP on
CREATE SCHEMA lab_data;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE TABLE lab_data.identity (tenant_id text PRIMARY KEY,cell_id text NOT NULL,deployment_id text NOT NULL);
CREATE TABLE lab_data.markers (tenant_id text NOT NULL,marker_id text NOT NULL,version bigint NOT NULL,value text NOT NULL,PRIMARY KEY(tenant_id,marker_id));
CREATE TABLE lab_data.operations (tenant_id text NOT NULL,operation_id text NOT NULL,semantic_digest text NOT NULL,sequence bigint NOT NULL,payload text NOT NULL,PRIMARY KEY(tenant_id,operation_id),UNIQUE(tenant_id,sequence));
CREATE TABLE lab_data.outbox (tenant_id text NOT NULL,operation_id text NOT NULL,receipt text NOT NULL,PRIMARY KEY(tenant_id,operation_id));
INSERT INTO lab_data.identity VALUES (:'tenant',:'cell','lab-deployment');
INSERT INTO lab_data.markers VALUES (:'tenant','marker',0,'GENESIS');
ALTER TABLE lab_data.identity ENABLE ROW LEVEL SECURITY;
ALTER TABLE lab_data.identity FORCE ROW LEVEL SECURITY;
ALTER TABLE lab_data.markers ENABLE ROW LEVEL SECURITY;
ALTER TABLE lab_data.markers FORCE ROW LEVEL SECURITY;
ALTER TABLE lab_data.operations ENABLE ROW LEVEL SECURITY;
ALTER TABLE lab_data.operations FORCE ROW LEVEL SECURITY;
ALTER TABLE lab_data.outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE lab_data.outbox FORCE ROW LEVEL SECURITY;
