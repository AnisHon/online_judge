package com.anishan.api.aspect;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.anishan.api.annotation.EnableCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Aspect
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CacheAspect {

    private final RedisTemplate<String, Object> template;

    private String getKey(String name, Method method, Object[] args) {
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();

        String data = DigestUtil.md5Hex(ArrayUtil.toString(args));

        return name + ":" + className + ":" + methodName + ":" + data;
    }

    @Pointcut("@annotation(com.anishan.api.annotation.EnableCache)")
    public void simpleCacheJoinPoint() {}


    @Around("simpleCacheJoinPoint()")
    public Object simpleCacheAround(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        method.setAccessible(true);

        Object[] args = joinPoint.getArgs();

        EnableCache cache = method.getAnnotation(EnableCache.class);

        long expire = cache.expire();
        String key = getKey(cache.name(), method, args);

        Object obj = template.opsForValue().get(key);

        if (obj != null) {
            log.debug("使用了缓存");
            return obj;
        }

        Object result = joinPoint.proceed();

        if (ArrayUtil.isNotEmpty(args)) {
            template.opsForValue().set(key, result, expire, TimeUnit.SECONDS);
        }

        return result;
    }

}
