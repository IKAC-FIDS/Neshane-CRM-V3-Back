package ir.neshane.customer.persistence.migration;

import org.flywaydb.core.Flyway;

/**
 * Explicit local migration entry point.
 * Not part of ordinary Customer Service runtime.
 */
public final class CustomerMigrationRunner {

    private static final String URL =
            "jdbc:postgresql://127.0.0.1:15433/customer_test";

    private static final String USER = "customer_migration";

    private CustomerMigrationRunner() {
    }

    public static void main(String[] args) {

        if (args.length != 1
                || (!"migrate".equals(args[0])
                && !"validate".equals(args[0]))) {

            throw new IllegalArgumentException(
                    "MIGRATION_OPERATION_NOT_ALLOWED");
        }

        String password = System.getenv(
                "NESHANE_IP02_MIGRATION_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "MIGRATION_CREDENTIALS_REQUIRED");
        }

        Flyway flyway = Flyway.configure()
                .dataSource(URL, USER, password)
                .defaultSchema("flyway_history")
                .schemas("flyway_history", "customer")
                .locations("classpath:db/migration")
                .createSchemas(false)
                .cleanDisabled(true)
                .baselineOnMigrate(false)
                .outOfOrder(false)
                .validateOnMigrate(true)
                .load();

        switch (args[0]) {
            case "migrate" -> {
                var result = flyway.migrate();

                System.out.println(
                        "MIGRATIONS_EXECUTED: "
                        + result.migrationsExecuted);
            }

            case "validate" -> {
                flyway.validate();

                System.out.println(
                        "PASS: Migration validation.");
            }

            default -> throw new IllegalStateException(
                    "MIGRATION_OPERATION_NOT_ALLOWED");
        }
    }
}
