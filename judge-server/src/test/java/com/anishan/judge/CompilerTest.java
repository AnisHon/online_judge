package com.anishan.judge;

import com.anishan.judge.config.LanguageConfigLoader;
import com.anishan.judge.entity.LanguageConfig;
import com.anishan.judge.judge.impl.CompilerImpl;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class CompilerTest {

    @Resource
    private LanguageConfigLoader languageConfigLoader;
    @Resource
    private CompilerImpl compilerImpl;
    @SneakyThrows
    @Test
    public void compileTest() {
        LanguageConfig languageConfig = languageConfigLoader.getLanguageConfigByName("C++");
        System.out.println(languageConfig);
        String compile = compilerImpl.compile(
                languageConfig,
                "#include <iostream>\nusing namespace std;\nint main() {\nint a, b;\ncin >> a >> b;\ncout << a + b << endl;\n}",
                "C++",
                null
        );
        System.out.println(compile);


    }

}
