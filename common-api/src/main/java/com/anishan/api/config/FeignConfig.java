package com.anishan.api.config;


import com.anishan.api.domain.LoginUser;
import com.anishan.api.util.AuthUtil;
import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

public class FeignConfig {
    @Bean
    public Logger.Level feignLogLevel(){
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor userInfoRequestInterceptor(){
        return template -> {

            // 获取登录用户
            LoginUser loginUser = AuthUtil.getNonThrowUser();

            if(loginUser == null) {
                // 如果为空则直接跳过
                return;
            }
            Long userId = loginUser.getUser().getUserId();
            if (userId == null) {
                return;
            }
            // 如果不为空则放入请求头中，传递给下游微服务
            template.header("user-id", userId.toString());
        };
    }
}
