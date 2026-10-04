package com.anishan.content.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Destructive only inside the explicitly allowlisted oj_it_* schemas. Enable with
 * The community-integration profile and OJ_COMMUNITY_IT_ALLOW_FIXTURES=true
 * are mandatory. Dedicated loopback MySQL 8 only; never points at db_*.
 */
class SolutionDomainMigrationIntegrationTest {
    private static final String PROBLEM = "oj_it_problem";
    private static final String CONTENT = "oj_it_content";
    private static final String USER = "oj_it_user";
    private static final long FIRST_ID = 9007199254740993L;
    private static String jdbcUrl;
    private static String user;
    private static String password;

    @BeforeAll
    static void requireExplicitIsolatedMySqlOptIn() throws Exception {
        com.anishan.api.integration.CommunityIntegrationSupport.validateEnvironment();
        String host = com.anishan.api.integration.CommunityIntegrationSupport.required("OJ_COMMUNITY_MYSQL_HOST");
        String port = com.anishan.api.integration.CommunityIntegrationSupport.required("OJ_COMMUNITY_MYSQL_PORT");
        user = com.anishan.api.integration.CommunityIntegrationSupport.required("OJ_COMMUNITY_MYSQL_USER");
        password = com.anishan.api.integration.CommunityIntegrationSupport.required("OJ_COMMUNITY_MYSQL_PASSWORD");
        jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try (Connection connection = connection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT VERSION()")) {
            assertTrue(result.next());
            assertTrue(result.getString(1).startsWith("8."), "migration integration tests require MySQL 8");
        }
    }

    @Test
    void legacyTransferResumesAfterCommittedBatchesDetectsConflictsAndCutsOverOnce() throws Exception {
        resetSchemas();
        createLegacySource();
        applyFrozenT02();
        runTool(true, "--phase", "prepare", "--source-mode", "LEGACY", "--confirm-source-backup");
        seedInteractions();

        assertNotEquals(0, runTool(false, "--phase", "copy", "--source-mode", "LEGACY",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped",
                "--test-fail-after-batches", "2"));

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE " + CONTENT + ".solution_explanation SET title='conflict' WHERE solution_id=" + FIRST_ID);
        }
        assertNotEquals(0, runTool(false, "--phase", "copy", "--source-mode", "LEGACY",
                "--confirm-source-backup", "--confirm-maintenance", "--confirm-source-writes-stopped",
                "--confirm-relays-stopped"));
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE " + CONTENT + ".solution_explanation SET title='题解0' WHERE solution_id=" + FIRST_ID);
        }

        runTool(true, "--phase", "copy", "--source-mode", "LEGACY",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped");
        runTool(true, "--phase", "copy", "--source-mode", "LEGACY",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped");
        runTool(true, "--phase", "verify", "--source-mode", "LEGACY",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped");

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            try (ResultSet row = statement.executeQuery("SELECT content FROM " + CONTENT
                    + ".solution_explanation_content WHERE solution_id=" + FIRST_ID)) {
                assertTrue(row.next());
                assertTrue(row.getString(1).contains("🚀"));
                assertTrue(row.getString(1).contains("$$"));
            }
            try (ResultSet row = statement.executeQuery("SELECT first_published_at FROM " + CONTENT
                    + ".solution_explanation WHERE solution_id=" + FIRST_ID)) {
                assertTrue(row.next());
                assertNotNull(row.getTimestamp(1));
            }
            try (ResultSet row = statement.executeQuery("SELECT first_published_at FROM " + CONTENT
                    + ".solution_explanation WHERE solution_id=" + (FIRST_ID + 500))) {
                assertTrue(row.next());
                assertNull(row.getTimestamp(1));
            }
            try (ResultSet row = statement.executeQuery("SELECT status,attempts FROM " + CONTENT
                    + ".content_event_outbox WHERE event_id='11111111-1111-4111-8111-111111111111'")) {
                assertTrue(row.next());
                assertEquals("SENT", row.getString(1));
                assertEquals(3, row.getInt(2));
            }
            try (ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM " + CONTENT
                    + ".solution_explanation WHERE solution_id=" + FIRST_ID)) {
                assertTrue(row.next());
                assertEquals(1, row.getInt(1));
            }
            try (ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM information_schema.key_column_usage "
                    + "WHERE table_schema='" + CONTENT + "' AND table_name='solution_explanation' "
                    + "AND referenced_table_name='problem'")) {
                assertTrue(row.next());
                assertEquals(0, row.getInt(1));
            }
            assertCheckConstraintRejects(connection);
        }

        runTool(true, "--phase", "cutover", "--source-mode", "LEGACY",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped");
        assertNotEquals(0, runTool(false, "--phase", "copy", "--source-mode", "LEGACY",
                "--confirm-source-backup", "--confirm-maintenance", "--confirm-source-writes-stopped",
                "--confirm-relays-stopped"));
    }

    @Test
    void freshInstallVerifiesAnEmptySolutionSourceAndUnknownOutboxTypesStopLegacyCopy() throws Exception {
        resetSchemas();
        createEmptyProblemSchema();
        createSchema(CONTENT);
        createSchema(USER);
        runTool(true, "--phase", "prepare", "--source-mode", "FRESH");
        runTool(true, "--phase", "copy", "--source-mode", "FRESH",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped");
        runTool(true, "--phase", "verify", "--source-mode", "FRESH",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped");

        resetSchemas();
        createLegacySource();
        applyFrozenT02();
        runTool(true, "--phase", "prepare", "--source-mode", "LEGACY", "--confirm-source-backup");
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO " + PROBLEM + ".problem_event_outbox "
                    + "(event_id,dedupe_key,event_type,exchange_name,routing_key,payload,status,attempts,next_attempt_at,created_at) "
                    + "VALUES('22222222-2222-4222-8222-222222222222','unknown-event','UNEXPECTED','x','y','{}','PENDING',0,NOW(3),NOW(3))");
        }
        assertNotEquals(0, runTool(false, "--phase", "copy", "--source-mode", "LEGACY",
                "--confirm-source-backup", "--confirm-maintenance", "--confirm-source-writes-stopped",
                "--confirm-relays-stopped"));
    }

    @Test
    void interruptedLegacyT02BackfillIsResumedOnlyDuringConfirmedCopy() throws Exception {
        resetSchemas();
        createLegacySource();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + PROBLEM
                    + ".solution_explanation ADD COLUMN first_published_at DATETIME(3) NULL "
                    + "COMMENT 'T02_BACKFILL_PENDING'");
        }
        runTool(true, "--phase", "prepare", "--source-mode", "LEGACY");
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            try (ResultSet row = statement.executeQuery("SELECT column_comment FROM information_schema.columns "
                    + "WHERE table_schema='" + PROBLEM + "' AND table_name='solution_explanation' "
                    + "AND column_name='first_published_at'")) {
                assertTrue(row.next());
                assertEquals("T02_BACKFILL_PENDING", row.getString(1), "prepare must not mutate the old source");
            }
        }
        assertNotEquals(0, runTool(false, "--phase", "copy", "--source-mode", "LEGACY",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped"));
        runTool(true, "--phase", "copy", "--source-mode", "LEGACY", "--confirm-source-backup",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped");
        runTool(true, "--phase", "verify", "--source-mode", "LEGACY",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped");
        try (Connection connection = connection(); Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT column_comment FROM information_schema.columns "
                     + "WHERE table_schema='" + PROBLEM + "' AND table_name='solution_explanation' "
                     + "AND column_name='first_published_at'")) {
            assertTrue(row.next());
            assertEquals("First public publication time", row.getString(1));
        }
    }

    @Test
    void legacyWithoutT02IsNormalizedDuringCopyAfterBackupConfirmation() throws Exception {
        resetSchemas();
        createLegacySource();
        runTool(true, "--phase", "prepare", "--source-mode", "LEGACY");
        try (Connection connection = connection()) {
            assertFalse(columnExists(connection, PROBLEM, "solution_explanation", "first_published_at"),
                    "prepare must leave an un-migrated legacy source untouched");
        }
        assertNotEquals(0, runTool(false, "--phase", "copy", "--source-mode", "LEGACY",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped"));
        runTool(true, "--phase", "copy", "--source-mode", "LEGACY", "--confirm-source-backup",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped");
        runTool(true, "--phase", "verify", "--source-mode", "LEGACY",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped");
        try (Connection connection = connection(); Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT first_published_at FROM " + PROBLEM
                     + ".solution_explanation WHERE solution_id=" + FIRST_ID)) {
            assertTrue(row.next());
            assertNotNull(row.getTimestamp(1), "legacy public solution receives the one-time historical marker");
        }
    }

    @Test
    void alreadyAppliedFrozenT02SchemaIsDetectedWithoutReplayingItsBackfill() throws Exception {
        resetSchemas();
        createLegacySource();
        applyFrozenT02();
        runTool(true, "--phase", "prepare", "--source-mode", "LEGACY");
        seedInteractions();
        runTool(true, "--phase", "copy", "--source-mode", "LEGACY",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped");
        runTool(true, "--phase", "verify", "--source-mode", "LEGACY",
                "--confirm-maintenance", "--confirm-source-writes-stopped", "--confirm-relays-stopped");
    }

    private static void createLegacySource() throws Exception {
        createSchema(PROBLEM);
        createSchema(CONTENT);
        createSchema(USER);
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE " + PROBLEM + ".problem (problem_id BIGINT PRIMARY KEY,title VARCHAR(255) NOT NULL,auth INT NOT NULL DEFAULT 1,del_flag BOOLEAN NOT NULL DEFAULT 0) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
            statement.execute("INSERT INTO " + PROBLEM + ".problem VALUES(1,'迁移测试题',1,0)");
            statement.execute("CREATE TABLE " + PROBLEM + ".solution_explanation (solution_id BIGINT NOT NULL PRIMARY KEY,title VARCHAR(255) NOT NULL,problem_id BIGINT NOT NULL,user_id BIGINT NOT NULL,top_up BOOLEAN NOT NULL DEFAULT 0,`private` BOOLEAN NOT NULL DEFAULT 0,create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,CONSTRAINT legacy_solution_problem_fk FOREIGN KEY(problem_id) REFERENCES " + PROBLEM + ".problem(problem_id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
            statement.execute("CREATE TABLE " + PROBLEM + ".solution_explanation_content (solution_id BIGINT NOT NULL PRIMARY KEY,content TEXT NOT NULL,CONSTRAINT legacy_solution_content_fk FOREIGN KEY(solution_id) REFERENCES " + PROBLEM + ".solution_explanation(solution_id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
            try (PreparedStatement solution = connection.prepareStatement("INSERT INTO " + PROBLEM
                    + ".solution_explanation(solution_id,title,problem_id,user_id,top_up,`private`,create_time,update_time) VALUES(?,?,1,9007199254740993,0,?,?,?)");
                 PreparedStatement body = connection.prepareStatement("INSERT INTO " + PROBLEM
                         + ".solution_explanation_content(solution_id,content) VALUES(?,?)")) {
                for (int index = 0; index <= 500; index++) {
                    long id = FIRST_ID + index;
                    solution.setLong(1, id);
                    solution.setString(2, "题解" + index);
                    solution.setBoolean(3, index == 500);
                    solution.setString(4, "2026-10-03 12:34:56");
                    solution.setString(5, "2026-10-03 12:34:56");
                    solution.executeUpdate();
                    body.setLong(1, id);
                    body.setString(2, index == 0 ? "包含 emoji 🚀 和 $$公式$$ 的正文\nsecond line" : "body-" + index);
                    body.executeUpdate();
                }
            }
        }
    }

    private static void seedInteractions() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("UPDATE " + PROBLEM + ".solution_explanation SET like_count=1,comment_count=2 WHERE solution_id=" + FIRST_ID);
            statement.execute("INSERT INTO " + PROBLEM + ".solution_like VALUES(" + FIRST_ID + ",9007199254740994,1,1,NOW(3),NOW(3))");
            statement.execute("INSERT INTO " + PROBLEM + ".solution_comment(comment_id,solution_id,user_id,content,state,like_count,reply_count,client_request_id,created_at,updated_at) "
                    + "VALUES(9007199254741093," + FIRST_ID + ",9007199254740994,'root','VISIBLE',0,1,'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',NOW(3),NOW(3))");
            statement.execute("INSERT INTO " + PROBLEM + ".solution_comment(comment_id,solution_id,user_id,root_id,parent_id,reply_to_user_id,content,state,like_count,reply_count,client_request_id,created_at,updated_at) "
                    + "VALUES(9007199254741094," + FIRST_ID + ",9007199254740995,9007199254741093,9007199254741093,9007199254740994,'reply 🚀','VISIBLE',1,0,'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',NOW(3),NOW(3))");
            statement.execute("INSERT INTO " + PROBLEM + ".comment_like VALUES(9007199254741094,9007199254740994,1,1,NOW(3),NOW(3))");
            statement.execute("INSERT INTO " + PROBLEM + ".solution_moderation_action VALUES(9007199254741194," + FIRST_ID + ",'COMMENT',9007199254741094,9007199254740995,1,'REMOVE','test reason',NOW(3))");
            statement.execute("INSERT INTO " + PROBLEM + ".problem_event_outbox "
                    + "(event_id,dedupe_key,event_type,exchange_name,routing_key,payload,status,attempts,next_attempt_at,last_error,created_at,sent_at) "
                    + "VALUES('11111111-1111-4111-8111-111111111111','solution-published:" + FIRST_ID
                    + "','SOLUTION_PUBLISHED','community.events.v1','solution.published.v1',JSON_OBJECT('solutionId','"
                    + FIRST_ID + "'),'SENT',3,NOW(3),NULL,NOW(3),NOW(3))");
        }
    }

    private static void assertCheckConstraintRejects(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO " + CONTENT
                    + ".solution_explanation(solution_id,title,problem_id,user_id,moderation_state,create_time,update_time) "
                    + "VALUES(9007199254741999,'invalid',1,1,'INVALID',NOW(),NOW())"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO " + CONTENT
                    + ".solution_like VALUES(" + FIRST_ID + ",9007199254740994,1,1,NOW(3),NOW(3))"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("DELETE FROM " + CONTENT
                    + ".solution_explanation WHERE solution_id=" + FIRST_ID));
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO " + CONTENT
                    + ".solution_comment(comment_id,solution_id,user_id,content,state,client_request_id,created_at,updated_at) "
                    + "VALUES(9007199254741294," + FIRST_ID + ",9007199254740994,'duplicate','VISIBLE',"
                    + "'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',NOW(3),NOW(3))"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO " + CONTENT
                    + ".content_event_outbox(event_id,dedupe_key,event_type,exchange_name,routing_key,payload,"
                    + "next_attempt_at,created_at) VALUES('33333333-3333-4333-8333-333333333333',"
                    + "'invalid-type','UNKNOWN','x','y','{}',NOW(3),NOW(3))"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO " + CONTENT
                    + ".content_event_outbox(event_id,dedupe_key,event_type,exchange_name,routing_key,payload,"
                    + "next_attempt_at,created_at) VALUES('44444444-4444-4444-8444-444444444444',"
                    + "'solution-published:" + FIRST_ID + "','SOLUTION_LIKED','x','y','{}',NOW(3),NOW(3))"));
        }
    }

    private static void applyFrozenT02() throws Exception {
        String sql = java.nio.file.Files.readString(Paths.get("../resources/sql/migration/V20261004_2__solution_interactions.sql")
                .normalize(), StandardCharsets.UTF_8).replace("db_problem", PROBLEM).replace("db_content", CONTENT);
        ProcessBuilder builder = new ProcessBuilder("mysql", "--protocol=tcp", "--default-character-set=utf8mb4",
                "--host", com.anishan.api.integration.CommunityIntegrationSupport.required("OJ_COMMUNITY_MYSQL_HOST"),
                "--port", com.anishan.api.integration.CommunityIntegrationSupport.required("OJ_COMMUNITY_MYSQL_PORT"),
                "--user", user).redirectErrorStream(true);
        builder.environment().put("MYSQL_PWD", password);
        Process process = builder.start();
        process.getOutputStream().write(sql.getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().close();
        byte[] output = process.getInputStream().readAllBytes();
        int exit = process.waitFor();
        assertEquals(0, exit, "frozen T02 fixture failed: " + new String(output, StandardCharsets.UTF_8));
    }

    private static void createEmptyProblemSchema() throws Exception {
        createSchema(PROBLEM);
        createSchema(CONTENT);
        createSchema(USER);
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE " + PROBLEM + ".problem (problem_id BIGINT PRIMARY KEY,title VARCHAR(255) NOT NULL,auth INT NOT NULL DEFAULT 1,del_flag BOOLEAN NOT NULL DEFAULT 0) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            statement.execute("CREATE TABLE " + PROBLEM + ".problem_event_outbox (event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,dedupe_key VARCHAR(160) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,event_type VARCHAR(48) NOT NULL,exchange_name VARCHAR(100) NOT NULL,routing_key VARCHAR(100) NOT NULL,payload JSON NOT NULL,status VARCHAR(16) NOT NULL DEFAULT 'PENDING',attempts INT NOT NULL DEFAULT 0,next_attempt_at DATETIME(3) NOT NULL,lease_owner VARCHAR(64),lease_until DATETIME(3),last_error VARCHAR(1000),created_at DATETIME(3) NOT NULL,sent_at DATETIME(3),KEY idx_problem_event_outbox_due(status,next_attempt_at,lease_until)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        }
    }

    private static void resetSchemas() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            for (String schema : Arrays.asList(PROBLEM, CONTENT, USER)) {
                assertTrue(Arrays.asList(PROBLEM, CONTENT, USER).contains(schema));
                statement.execute("CREATE DATABASE IF NOT EXISTS " + schema + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                java.util.List<String> tables = new java.util.ArrayList<>();
                try (ResultSet result = statement.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='" + schema + "'")) {
                    while (result.next()) tables.add(result.getString(1));
                }
                statement.execute("SET FOREIGN_KEY_CHECKS=0");
                for (String table : tables) {
                    assertTrue(table.matches("[a-zA-Z0-9_]+"));
                    statement.execute("DROP TABLE " + schema + ".`" + table + "`");
                }
                statement.execute("SET FOREIGN_KEY_CHECKS=1");
            }
        }
    }

    private static void createSchema(String schema) throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS " + schema + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private static boolean columnExists(Connection connection, String schema, String table, String column)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema=? AND table_name=? AND column_name=?")) {
            statement.setString(1, schema);
            statement.setString(2, table);
            statement.setString(3, column);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getInt(1) > 0;
            }
        }
    }

    private static int runTool(boolean expectedSuccess, String... args) throws Exception {
        List<String> command = new java.util.ArrayList<>();
        command.add("bash");
        command.add("scripts/migrate-solution-domain.sh");
        command.addAll(Arrays.asList(args));
        ProcessBuilder builder = new ProcessBuilder(command).directory(Paths.get("..").toAbsolutePath().normalize().toFile());
        builder.redirectErrorStream(true);
        builder.environment().put("OJ_MIGRATION_MYSQL_HOST", com.anishan.api.integration.CommunityIntegrationSupport.required("OJ_COMMUNITY_MYSQL_HOST"));
        builder.environment().put("OJ_MIGRATION_MYSQL_PORT", com.anishan.api.integration.CommunityIntegrationSupport.required("OJ_COMMUNITY_MYSQL_PORT"));
        builder.environment().put("OJ_MIGRATION_MYSQL_USER", user);
        builder.environment().put("OJ_MIGRATION_MYSQL_PASSWORD", password);
        builder.environment().put("OJ_MIGRATION_PROBLEM_SCHEMA", PROBLEM);
        builder.environment().put("OJ_MIGRATION_CONTENT_SCHEMA", CONTENT);
        builder.environment().put("OJ_SOLUTION_MIGRATION_TEST_MODE", "1");
        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = process.getInputStream().read(buffer)) >= 0) output.write(buffer, 0, read);
        int exit = process.waitFor();
        if (expectedSuccess && exit != 0) {
            fail("migration tool failed: " + new String(output.toByteArray(), StandardCharsets.UTF_8));
        }
        return exit;
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

}
