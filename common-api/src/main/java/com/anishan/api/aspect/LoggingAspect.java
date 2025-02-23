package com.anishan.api.aspect;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.anishan.api.annotation.ControllerLog;
import com.anishan.api.annotation.MethodLog;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;


@Slf4j
@Component
@Aspect
public class LoggingAspect {

    // 异步日志线程池
    private static final ExecutorService executorService;

    static {
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;  // 核心线程数
        int maxPoolSize = corePoolSize * 5;  // 最大线程数

        executorService = ThreadUtil.newExecutor(corePoolSize, maxPoolSize, 1000);
    }

    @Pointcut("@annotation(com.anishan.api.annotation.MethodLog)")
    public void methodLoggingJoinPoint() {}

    @Pointcut("@annotation(com.anishan.api.annotation.ControllerLog)")
    public void controllerLoggingJoinPoint() {}

    @Value("${spring.config.name}")
    private String serviceName;

    private String getMethodSignature(MethodSignature methodSignature) {
        String methodName = methodSignature.getName();
        Class<?>[] parameterTypes = methodSignature.getParameterTypes();

        String returnType = methodSignature.getReturnType().getName();

        String className = methodSignature
                .getMethod()
                .getDeclaringClass()
                .getName();

        StringBuilder signatureInfo = new StringBuilder();
        signatureInfo
                .append(returnType)
                .append(StrUtil.SPACE)
                .append(className)
                .append(StrUtil.DOT)
                .append(methodName)
                .append("(");

        for (Class<?> type : parameterTypes) {
            signatureInfo.append(type.getSimpleName()).append(", ");
        }

        if (parameterTypes.length > 0) {
            signatureInfo.setLength(signatureInfo.length() - 2);  // 移除最后的 ", "
        }

        signatureInfo.append(")");

        return signatureInfo.toString();
    }

    private void logBegin() {
        log.info("=================================== START ===================================");
    }

    private void logEnd() {
        log.info("===================================  END  ===================================");
    }

    private void logRuntime(long runtime) {
        log.info("runtime:{}ms", runtime);
    }

    private void logMethodLog(ProceedingJoinPoint joinPoint, long runtime) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        method.setAccessible(true);

        MethodLog logAnnotation = method.getAnnotation(MethodLog.class);

        String description = logAnnotation.desc();

        String methodSignature = getMethodSignature(signature);

        Object[] args = joinPoint.getArgs();

        synchronized (LoggingAspect.class) {
            logBegin();

            log.info("method: {}", methodSignature);
            log.info("desc:{}", description);
            logRuntime(runtime);

            if (ArrayUtil.isNotEmpty(args)) {
                log.info("args:");
                for (Object arg : args) {
                    log.info(" - {}", arg.toString());
                }
            }

            logEnd();
        }

    }

    private String argsToString(ProceedingJoinPoint joinPoint) {
        return ArrayUtil.toString(joinPoint.getArgs());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个代理 IP 的情况（取第一个）
        return ip != null ? ip.split(",")[0].trim() : "";
    }

    @SneakyThrows
    private void logControllerPoint(ServletRequestAttributes attributes, ProceedingJoinPoint joinPoint, long runtime) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        method.setAccessible(true);

        ControllerLog logAnnotation = method.getAnnotation(ControllerLog.class);

        String api = logAnnotation.api();
        String operation = logAnnotation.operation();
        String service = logAnnotation.service();
        String description = logAnnotation.desc();
        String uri = "";
        String userId = null;
        String params = argsToString(joinPoint);
        String ip = null;

        if (StrUtil.isEmpty(service)) {
            service = serviceName;
        }
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ip = getClientIp(request);
            userId = request.getHeader("user-id");
        }

        // 保证顺序
        synchronized (LoggingAspect.class) {
            logBegin();

            log.info("service: {}", service);
            log.info("uri: {}", uri);
            log.info("path: /{}/{}", api, operation);
            logRuntime(runtime);
            log.info("description: {}", description);
            log.info("ip: {}", ip);
            log.info("Id: {}", userId);
            log.info("params: {}", params);

            logEnd();
        }
    }

    @Around("methodLoggingJoinPoint()")
    public Object methodLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long begin = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long end = System.currentTimeMillis();

        Runnable runnable = () -> logMethodLog(joinPoint, end - begin);

        executorService.submit(runnable);

        return result;
    }

    @Around("controllerLoggingJoinPoint()")
    public Object controllerLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long begin = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long end = System.currentTimeMillis();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        Runnable runnable = () -> logControllerPoint(attributes, joinPoint, end - begin);

        executorService.submit(runnable);

        return result;
    }

}
