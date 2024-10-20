package com.anishan.api.filter;

import cn.hutool.core.util.StrUtil;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.api.exception.IllegalTokenException;
import com.anishan.api.util.JwtUtil;
import com.anishan.commons.entity.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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
public class UserAuthenticationFilter extends OncePerRequestFilter {


    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("token");
        if (StrUtil.isEmpty(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = JwtUtil.parseJwt(token);

        if (!AuthUtil.isUserExisted(redisTemplate, userId)) {
            throw new IllegalTokenException("令牌过期");
        }

        LoginUser loginUser = AuthUtil.getLoginUser(redisTemplate, userId);

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
