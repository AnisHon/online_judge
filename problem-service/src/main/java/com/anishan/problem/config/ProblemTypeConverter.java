package com.anishan.problem.config;

import com.anishan.commons.enumeration.ProblemType;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ProblemTypeConverter implements Converter<String, ProblemType> {

    @Override
    public ProblemType convert(@NotNull String source) {
        String normalized = source.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        // URL 查询参数始终是字符串；兼容前端发送的数字枚举值和旧页面发送的枚举名。
        try {
            int code = Integer.parseInt(normalized);
            for (ProblemType problemType : ProblemType.values()) {
                if (problemType.getValue() == code) {
                    return problemType;
                }
            }
        } catch (NumberFormatException ignored) {
            // 继续按枚举名称解析，非法值最终由 Spring 按 400 参数错误处理。
        }

        for (ProblemType problemType : ProblemType.values()) {
            if (problemType.name().equalsIgnoreCase(valueOf(normalized))) {
                return problemType;
            }
        }

        throw new IllegalArgumentException("Unsupported problem type: " + source);
    }

    private static String valueOf(String source) {
        return source.trim().toUpperCase(Locale.ROOT);
    }
}
