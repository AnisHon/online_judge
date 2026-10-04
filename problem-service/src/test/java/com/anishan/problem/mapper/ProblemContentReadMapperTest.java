package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.Problem;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemContentReadMapperTest {

    @Test
    void contentReadQueryIncludesDeletedRowsAndSelectsOnlySafeMetadata() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mapper/ProblemMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "mapper/ProblemMapper.xml", configuration.getSqlFragments()).parse();
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("problemIds", Arrays.asList(1L, 2L));
        BoundSql boundSql = configuration.getMappedStatement(
                ProblemMapper.class.getName() + ".selectContentReadByProblemIds").getBoundSql(parameters);
        String sql = boundSql.getSql().toLowerCase().replaceAll("\\s+", "");

        assertTrue(sql.contains("selectproblem_id,title,auth,del_flagfromproblem"));
        assertTrue(sql.contains("whereproblem_idin(?,?)"));
        assertFalse(sql.contains("del_flag = 0"));
        assertFalse(sql.contains("description"));
        assertFalse(sql.contains("hint"));
        assertFalse(sql.contains("answer"));
        assertEquals(Problem.class, configuration.getMappedStatement(
                ProblemMapper.class.getName() + ".selectContentReadByProblemIds").getResultMaps().get(0)
                .getType());
    }
}
