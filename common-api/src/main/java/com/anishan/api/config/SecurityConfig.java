package com.anishan.api.config;

import com.anishan.api.filter.UserAuthenticationFilter;
import com.anishan.api.handler.AccessDeniedHandlerImpl;
import com.anishan.api.handler.AuthenticationEntryPointImpl;
import com.anishan.commons.config.SharedConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Resource;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ComponentScan("com.anishan.api.filter")
public class SecurityConfig {

    @Resource
    private SharedConfig sharedConfig;

    private static final String[] SWAGGER_API_URL = {
            "/swagger-resources/**", "/v2/**", "/v3/**", "/doc.html", "/webjars/**"
    };
    private final String[] PERMIT_URI = {
            "/auth/login",
            "/auth/refresh",
            "/auth/registration",
            "/auth/forget-pass",
            "/auth/send-email-code",
            "/auth/send-forget-email-code",
            "/auth/captcha-code",
            "/site/config",
            "/user/username/**",
            "/user/email/**",
            "/version",
            "/test",
            "/internal/**",
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    @ConditionalOnBean(UserDetailsService.class)
    public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserAuthenticationFilter authenticationInterceptor
    ) throws Exception {
        return http
                .csrf(CsrfConfigurer::disable)
                .addFilterBefore(authenticationInterceptor, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(conf -> {

                    conf.antMatchers(PERMIT_URI).permitAll();
                    conf.antMatchers(HttpMethod.GET, "/avatar/**", "/image/**").permitAll();

                    if (sharedConfig.isProduct()) {
                        conf.antMatchers(SWAGGER_API_URL).denyAll();
                        conf.anyRequest().authenticated();
                    } else {
                        conf.antMatchers(SWAGGER_API_URL).permitAll();
                        conf.anyRequest().permitAll();
                    }

//                    conf.anyRequest().permitAll();
                })

                .exceptionHandling(conf -> {
                    conf.accessDeniedHandler(new AccessDeniedHandlerImpl());
                    conf.authenticationEntryPoint(new AuthenticationEntryPointImpl());

                }).build();


    }



}
