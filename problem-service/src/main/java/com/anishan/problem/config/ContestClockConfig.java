package com.anishan.problem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ContestClockConfig {

    @Bean(name = "contestSubmissionClock")
    public Clock contestSubmissionClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }
}
