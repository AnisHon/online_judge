package com.anishan.user;

import com.anishan.api.config.FeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients(defaultConfiguration = FeignConfig.class, basePackages = "com.anishan.api.client")
@SpringBootApplication
@EnableAspectJAutoProxy
@EnableScheduling
@MapperScan("com.anishan.user.mapper")
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
