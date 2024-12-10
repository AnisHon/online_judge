package com.anishan.user.aspect;

import com.anishan.commons.enumeration.SseEvent;
import com.anishan.user.domain.dto.UserPoint;
import com.anishan.user.service.SysUserService;
import com.anishan.user.util.SseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class SysUserAspect {


}
