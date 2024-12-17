package com.anishan.api.filter;

import com.anishan.api.domain.LoginUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.config.SharedConfig;
import com.anishan.commons.exception.IllegalTokenException;
import com.anishan.commons.domain.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class UserAuthenticationFilter extends OncePerRequestFilter {
    private final AuthUtil authUtil;

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("user-id");
        if (header == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = Long.parseLong(header);
        if (!authUtil.isUserExisted(userId)) {
            throw new IllegalTokenException("令牌过期");
        }
        LoginUser loginUser = authUtil.getLoginUser(userId);

        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        filterChain.doFilter(request, response);

    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        try {
            doFilter(request, response, filterChain);

        } catch (IllegalTokenException e) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            R<String> rest = R.unauthorized(e.getMessage());

            new ObjectMapper().writeValue(response.getWriter(), rest);
        }
    }



}
