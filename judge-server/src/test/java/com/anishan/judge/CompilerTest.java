package com.anishan.judge;

import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.judge.config.LanguageConfigLoader;
import com.anishan.judge.domain.entity.LanguageConfig;
import com.anishan.judge.judge.Judge;
import com.anishan.judge.judge.SandboxRun;
import com.anishan.judge.judge.impl.CompilerImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@SpringBootTest
public class CompilerTest {

    @Resource
    private LanguageConfigLoader languageConfigLoader;
    @Resource
    private CompilerImpl compilerImpl;
    @Resource
    SandboxRun sandboxRun;

    @Resource
    Judge judge;

    @Resource
    ObjectMapper objectMapper;
    @SneakyThrows
    @Test
    public void compileTest() {
        LanguageConfig languageConfigByName = languageConfigLoader.getLanguageConfigByName("C++");

        System.out.println(languageConfigByName);

        String fileId = compilerImpl.compile(
                languageConfigByName,
                "#include <iostream>\nusing namespace std;\nint main() {\nint a, b;\ncin >> a >> b;\ncout << a + b << endl;\n}",
                "C++",
                null
        );
        List<RunResult> runResults = judge.judgeAll(
                fileId,
                languageConfigByName,
                1000L,
                1000L,
                1000,
                List.of("1 2", "1 3", "4 5", "5 6")
        );
        log.debug(runResults.toString());

        runResults.forEach(runResult -> {log.debug(runResult.getFiles().getStdout());});
//        sandboxRun.delFile(fileId);
    }

}
