package com.anishan.content.mapper;

import com.anishan.content.domain.entity.*;
import com.anishan.content.domain.enumeration.CommentState;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InteractionSchemaTest {
    private static final List<String> MAPPERS = Arrays.asList(
            "SolutionExplanationMapper", "SolutionExplanationContentMapper", "SolutionLikeMapper",
            "SolutionCommentMapper", "CommentLikeMapper", "SolutionModerationActionMapper",
            "ContentEventOutboxMapper", "SolutionProblemReferenceMapper");

    @Test
    void contentMapperXmlParsesAndKeepsContentNamespaces() throws Exception {
        for (String name : MAPPERS) {
            String resource = "/mapper/" + name + ".xml";
            Configuration configuration = new Configuration();
            try (InputStream input = getClass().getResourceAsStream(resource)) {
                assertNotNull(input, resource);
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
            assertTrue(configuration.hasMapper(Class.forName("com.anishan.content.mapper." + name)), resource);
        }
        assertFalse(MAPPERS.stream().anyMatch(name -> name.contains("ProblemEventOutbox")));
    }

    @Test
    void entitiesUseContentTablesExactLongIdsAndSingleVersionField() throws Exception {
        assertTable(SolutionExplanation.class, "solution_explanation");
        assertTable(SolutionExplanationContent.class, "solution_explanation_content");
        assertTable(SolutionLike.class, "solution_like");
        assertTable(SolutionComment.class, "solution_comment");
        assertTable(CommentLike.class, "comment_like");
        assertTable(SolutionModerationAction.class, "solution_moderation_action");
        assertTable(ContentEventOutbox.class, "content_event_outbox");
        assertTable(SolutionProblemReference.class, "solution_problem_reference");
        assertEquals(IdType.INPUT, SolutionExplanationContent.class.getDeclaredField("solutionId")
                .getAnnotation(TableId.class).type());
        assertEquals(IdType.ASSIGN_ID, SolutionComment.class.getDeclaredField("commentId")
                .getAnnotation(TableId.class).type());
        assertEquals(IdType.ASSIGN_ID, SolutionModerationAction.class.getDeclaredField("actionId")
                .getAnnotation(TableId.class).type());
        assertEquals(Long.class, SolutionExplanation.class.getDeclaredField("version").getType());
        assertNull(SolutionExplanation.class.getDeclaredField("updateTime").getAnnotation(Version.class));
        assertArrayEquals(new String[]{"VISIBLE", "AUTHOR_DELETED", "ADMIN_DELETED"},
                Arrays.stream(CommentState.values()).map(Enum::name).toArray(String[]::new));
        assertArrayEquals(new String[]{"NORMAL", "AUTHOR_ONLY"},
                Arrays.stream(SolutionModerationState.values()).map(Enum::name).toArray(String[]::new));
    }

    @Test
    void freshSchemaPlacesSolutionDataInContentAndLeavesProblemOutboxInProblem() throws Exception {
        String content = read("../resources/sql/db_content.sql").toLowerCase();
        String problem = read("../resources/sql/db_problem.sql").toLowerCase();
        String schemaMigration = read("../resources/sql/migration/V20261004_4__content_solution_schema.sql").toLowerCase();
        for (String table : Arrays.asList("solution_explanation", "solution_explanation_content", "solution_like",
                "solution_comment", "comment_like", "solution_moderation_action", "content_event_outbox",
                "solution_problem_reference", "content_solution_migration_state")) {
            assertTrue(content.contains("create table if not exists " + table), "init: " + table);
            assertTrue(schemaMigration.contains("create table if not exists " + table), "migration: " + table);
        }
        assertFalse(problem.contains("create table solution_explanation"));
        assertFalse(problem.contains("create table solution_like"));
        assertTrue(problem.contains("create table problem_event_outbox"));
        assertTrue(content.contains("references solution_explanation (solution_id) on delete restrict on update restrict"));
        assertFalse(schemaMigration.contains("references problem"));
        assertTrue(read("../resources/sql/migration/V20261004_5__content_solution_transfer.sql")
                .contains("LIMIT 500"));
    }

    @Test
    void manifestFreezesHistoricalMigrationsAndCoversBoth20261003Scripts() throws Exception {
        Path dir = Paths.get("../resources/sql/migration").normalize();
        String manifest = Files.readString(dir.resolve("manifest.tsv"), StandardCharsets.UTF_8);
        assertTrue(manifest.contains("V20261003_1\tV20261003_1__account_policies.sql"));
        assertTrue(manifest.contains("V20261003_2\tV20261003_2__profile_heatmap_index.sql"));
        assertChecksum(dir, "V20261004_1__user_social.sql",
                "667063a2bd0f1d8ca8d52eb01b5e5842509b6b18c07c5fad0172b7523bf64751");
        assertChecksum(dir, "V20261004_2__solution_interactions.sql",
                "54dcaf7e041bd73a37044e9ca10ef4ed2806e530ee6519b12e05d3d68c383d32");
        assertChecksum(dir, "V20261004_3__interaction_permissions.sql",
                "7613129c62eb4260ceafaec6a64a941385413cf6f9a99fbc65ff09061c9d4802");
    }

    private static void assertTable(Class<?> type, String table) {
        assertEquals(table, type.getAnnotation(TableName.class).value());
    }

    private void assertChecksum(Path dir, String file, String expected) throws Exception {
        byte[] bytes = Files.readAllBytes(dir.resolve(file));
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) hex.append(String.format("%02x", value));
        assertEquals(expected, hex.toString(), file);
    }

    private String read(String relative) throws Exception {
        return Files.readString(Paths.get(relative).normalize(), StandardCharsets.UTF_8);
    }
}
