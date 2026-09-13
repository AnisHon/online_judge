package com.anishan.api.handler;

import com.anishan.commons.domain.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {


    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        R<String> rest;

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // 认证异常可能包含过滤器、JWT 或底层组件的实现细节，不能直接返回给客户端。
        rest = R.unauthorized("请先登录或重新登录");


        new ObjectMapper().writeValue(response.getWriter(), rest);


    }
}
