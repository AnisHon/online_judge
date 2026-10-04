package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.ProblemEventOutbox;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The problem service owns only its own points/judge outbox, not content-domain solution interactions. */
class InteractionSchemaTest {
    @Test
    void problemOutboxMapperAndEntityRemainInProblemDomain() throws Exception {
        String resource = "/mapper/ProblemEventOutboxMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        assertTrue(configuration.hasResultMap("com.anishan.problem.mapper.ProblemEventOutboxMapper"
                + ".ProblemEventOutboxResultMap"));
        assertEquals("problem_event_outbox", ProblemEventOutbox.class.getAnnotation(TableName.class).value());

        String problemSql = Files.readString(Paths.get("../resources/sql/db_problem.sql").normalize())
                .toLowerCase(java.util.Locale.ROOT);
        assertTrue(problemSql.contains("create table problem_event_outbox"));
        assertTrue(problemSql.contains("transactional problem-service message outbox"));
    }
}
