package com.anishan.problem;

import com.anishan.api.config.FeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableRabbit
@EnableFeignClients(defaultConfiguration = FeignConfig.class, basePackages = "com.anishan.api.client")
@SpringBootApplication
@MapperScan("com.anishan.problem.mapper")
public class ProblemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProblemApplication.class, args);
    }

}
