package com.anishan.content.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ContentSolutionMigrationStateMapper {
    @Select("SELECT status FROM content_solution_migration_state WHERE migration_key = #{migrationKey}")
    String selectStatus(@Param("migrationKey") String migrationKey);
}
