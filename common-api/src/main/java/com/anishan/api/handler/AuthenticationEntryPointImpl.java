package com.anishan.api.handler;

import com.anishan.commons.exception.IllegalTokenException;
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
        rest = R.unauthorized(authException.getMessage());


        new ObjectMapper().writeValue(response.getWriter(), rest);


    }
}
