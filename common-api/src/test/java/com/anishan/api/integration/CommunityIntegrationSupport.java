package com.anishan.api.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** Only disposable resources explicitly named by the T24 contract are accepted. */
public final class CommunityIntegrationSupport {
    public static final String VHOST = "/oj-community-it";
    private CommunityIntegrationSupport() {}
    public static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) throw new IllegalStateException("Missing integration setting: " + name);
        return value;
    }
    public static void validateEnvironment() {
        if (!Boolean.getBoolean("oj.community.integration.required")) throw new IllegalStateException("Use the explicit community-integration profile");
        if (!"true".equals(required("OJ_COMMUNITY_IT_ALLOW_FIXTURES"))) throw new IllegalStateException("Fixture consent required");
        if (!Arrays.asList("127.0.0.1", "localhost").contains(required("OJ_COMMUNITY_MYSQL_HOST"))) throw new IllegalStateException("Test MySQL must be loopback");
        required("OJ_COMMUNITY_MYSQL_USER"); required("OJ_COMMUNITY_MYSQL_PASSWORD"); port("OJ_COMMUNITY_MYSQL_PORT");
        if (!VHOST.equals(required("OJ_COMMUNITY_RABBIT_VHOST"))) throw new IllegalStateException("Unsafe Rabbit vhost");
        if (!required("OJ_COMMUNITY_REDIS_PREFIX").matches("oj:community:it:[a-zA-Z0-9_-]+:")) throw new IllegalStateException("Unsafe Redis prefix");
        loopback("OJ_COMMUNITY_RABBIT_HOST"); port("OJ_COMMUNITY_RABBIT_PORT");
        required("OJ_COMMUNITY_RABBIT_USER"); required("OJ_COMMUNITY_RABBIT_PASSWORD");
        loopback("OJ_COMMUNITY_REDIS_HOST"); port("OJ_COMMUNITY_REDIS_PORT");
    }
    private static void loopback(String name) {
        if (!Arrays.asList("127.0.0.1", "localhost").contains(required(name))) {
            throw new IllegalStateException("Integration middleware must be loopback: " + name);
        }
    }
    public static int port(String name) {
        int port = Integer.parseInt(required(name));
        if (port < 1 || port > 65535) throw new IllegalStateException("Invalid test port: " + name);
        return port;
    }
    public static void schema(String value) {
        if (!Arrays.asList("oj_it_user", "oj_it_problem", "oj_it_content").contains(value)) throw new IllegalArgumentException("Unsafe fixture schema");
    }
    public static DataSource dataSource(String schema) {
        validateEnvironment(); schema(schema);
        return new DriverManagerDataSource("jdbc:mysql://" + required("OJ_COMMUNITY_MYSQL_HOST") + ":"
            + port("OJ_COMMUNITY_MYSQL_PORT") + "/" + schema + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8",
            required("OJ_COMMUNITY_MYSQL_USER"), required("OJ_COMMUNITY_MYSQL_PASSWORD"));
    }
    public static SqlSessionTemplate sessions(DataSource source, String... names) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean(); factory.setDataSource(source);
        MybatisConfiguration configuration = new MybatisConfiguration(); configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);
        GlobalConfig global = new GlobalConfig(); global.setIdentifierGenerator(new DefaultIdentifierGenerator()); factory.setGlobalConfig(global);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/{" + String.join(",", names) + "}.xml"));
        return new SqlSessionTemplate(factory.getObject());
    }
    public static TransactionTemplate transactions(DataSource source) { return new TransactionTemplate(new DataSourceTransactionManager(source)); }
    public static Path root() {
        Path path = Paths.get("").toAbsolutePath();
        while (path != null && !Files.exists(path.resolve("resources/sql/migration/manifest.tsv"))) path = path.getParent();
        if (path == null) throw new IllegalStateException("Repository migration manifest not found");
        return path;
    }
    public static void sqlFile(String relativePath) throws Exception {
        validateEnvironment();
        Path path = root().resolve(relativePath).normalize();
        if (!path.getParent().equals(root().resolve("resources/sql/migration"))
                || !Arrays.asList("V20261004_1__user_social.sql", "V20261004_2__solution_interactions.sql",
                "V20261004_3__interaction_permissions.sql", "V20261004_4__content_solution_schema.sql",
                "V20261004_6__contest_final_rank.sql", "V20261004_7__contest_list_restrict.sql")
                .contains(path.getFileName().toString())) throw new IllegalArgumentException("Fixture file is not allowlisted");
        String sql = Files.readString(path).replace("db_user", "oj_it_user").replace("db_problem", "oj_it_problem").replace("db_content", "oj_it_content");
        if (sql.toUpperCase().contains("DROP DATABASE") || sql.toUpperCase().contains("TRUNCATE")) throw new IllegalArgumentException("Whole database destruction prohibited");
        ProcessBuilder command = new ProcessBuilder("mysql", "--protocol=tcp", "-h" + required("OJ_COMMUNITY_MYSQL_HOST"),
            "-P" + port("OJ_COMMUNITY_MYSQL_PORT"), "-u" + required("OJ_COMMUNITY_MYSQL_USER"), "--default-character-set=utf8mb4");
        command.environment().put("MYSQL_PWD", required("OJ_COMMUNITY_MYSQL_PASSWORD"));
        command.redirectOutput(ProcessBuilder.Redirect.DISCARD); command.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process process = command.start(); process.getOutputStream().write(sql.getBytes(StandardCharsets.UTF_8)); process.getOutputStream().close();
        if (!process.waitFor(90, TimeUnit.SECONDS)) {process.destroyForcibly(); throw new IllegalStateException("Fixture migration timed out");}
        if (process.exitValue() != 0) throw new IllegalStateException("Fixture migration failed: " + path.getFileName());
    }
    public static JdbcTemplate jdbc(String schema) {return new JdbcTemplate(dataSource(schema));}
}
