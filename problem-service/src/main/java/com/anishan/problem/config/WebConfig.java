package com.anishan.problem.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WebConfig implements WebMvcConfigurer {

    private final ProblemTypeConverter problemTypeConverter;


    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(problemTypeConverter);  // 注册自定义转换器
    }

}
