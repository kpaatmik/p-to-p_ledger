package com.paymentledger;

import org.junit.jupiter.api.Test;

/**
 * Smoke test placeholder for T-010.
 *
 * Full Spring context loading is deferred to T-015 because it requires a
 * running PostgreSQL instance with DB_URL, DB_USERNAME, DB_PASSWORD, and
 * JWT_SECRET environment variables set.
 *
 * This no-op test exists so that mvn clean compile and mvn test succeed
 * without a database connection. Replace with a real context test in T-015.
 */
class PaymentLedgerApplicationTests {

    @Test
    void placeholderPassesWithoutDatabase() {
        // Intentionally empty.
        // Context load test added in T-015 once database is configured.
    }
}
