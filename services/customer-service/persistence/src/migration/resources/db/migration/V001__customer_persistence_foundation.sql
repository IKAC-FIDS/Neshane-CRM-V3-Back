-- Customer persistence foundation.
-- Business tables are introduced by subsequent reviewed migrations.

DO $migration$
BEGIN
    IF current_user <> 'customer_migration' THEN
        RAISE EXCEPTION 'INVALID_MIGRATION_IDENTITY';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_catalog.pg_roles
        WHERE rolname = 'customer_runtime'
          AND rolcanlogin
          AND NOT rolsuper
          AND NOT rolbypassrls
          AND NOT rolcreatedb
          AND NOT rolcreaterole
          AND NOT rolreplication
          AND NOT rolinherit
    ) THEN
        RAISE EXCEPTION 'INVALID_RUNTIME_ROLE';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_catalog.pg_roles
        WHERE rolname = current_user
          AND (
              rolsuper
              OR rolbypassrls
              OR rolcreatedb
              OR rolcreaterole
              OR rolreplication
          )
    ) THEN
        RAISE EXCEPTION 'INVALID_MIGRATION_PRIVILEGES';
    END IF;

    IF (
        SELECT count(*)
        FROM pg_catalog.pg_namespace
        WHERE nspname IN ('customer', 'flyway_history')
          AND nspowner = (
              SELECT oid
              FROM pg_catalog.pg_roles
              WHERE rolname = current_user
          )
    ) <> 2 THEN
        RAISE EXCEPTION 'INVALID_SCHEMA_OWNERSHIP';
    END IF;

    IF NOT has_schema_privilege(
        'customer_runtime', 'customer', 'USAGE'
    ) THEN
        RAISE EXCEPTION 'RUNTIME_SCHEMA_USAGE_REQUIRED';
    END IF;

    IF has_schema_privilege(
        'customer_runtime', 'customer', 'CREATE'
    ) THEN
        RAISE EXCEPTION 'RUNTIME_DDL_PRIVILEGE_FORBIDDEN';
    END IF;

    IF has_schema_privilege(
        'customer_runtime', 'flyway_history', 'USAGE'
    ) THEN
        RAISE EXCEPTION 'RUNTIME_HISTORY_ACCESS_FORBIDDEN';
    END IF;

    IF pg_has_role(
        'customer_runtime', 'customer_migration', 'MEMBER'
    ) THEN
        RAISE EXCEPTION 'RUNTIME_ROLE_ESCALATION_FORBIDDEN';
    END IF;
END
$migration$;
