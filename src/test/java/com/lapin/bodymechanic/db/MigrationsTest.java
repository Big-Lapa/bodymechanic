package com.lapin.bodymechanic.db;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real Flyway migrations on a throw-away PostgreSQL and checks the state they leave behind.
 * Skipped when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
class MigrationsTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15");

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();
    }

    private static long queryLong(String sql) throws Exception {
        try (Connection c = DriverManager.getConnection(
                     POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    /** The id the next insert will get, read without consuming a value from the sequence. */
    private static long nextGeneratedId() throws Exception {
        return queryLong("select case when is_called then last_value + 1 else last_value end from hibernate_sequence");
    }

    @Test
    void seededAdminExistsWithIdOne() throws Exception {
        assertEquals(1L, queryLong("select id from m_user where username = 'admin'"));
    }

    @Test
    void defaultAdminIsDisabled() throws Exception {
        assertEquals(0L, queryLong("select count(*) from m_user where username = 'admin' and active"));
    }

    @Test
    void idSequenceIsAheadOfTheSeededAdmin_soTheFirstRegisteredUserDoesNotCollideWithIt() throws Exception {
        long maxUserId = queryLong("select coalesce(max(id), 0) from m_user");
        long next = nextGeneratedId();

        assertTrue(next > maxUserId,
                "first generated id (" + next + ") must not reuse an existing user id (" + maxUserId + ")");
    }

    @Test
    void idSequenceIsAheadOfExistingMessages() throws Exception {
        long maxMessageId = queryLong("select coalesce(max(id), 0) from message");
        long next = nextGeneratedId();

        assertTrue(next > maxMessageId);
    }
}
