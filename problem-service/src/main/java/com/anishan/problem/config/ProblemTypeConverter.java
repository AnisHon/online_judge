package com.anishan.problem.config;

import com.anishan.commons.enumeration.ProblemType;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProblemTypeConverter implements Converter<Integer, ProblemType> {
    @Override
    public ProblemType convert(@NotNull Integer source) {
        for (ProblemType value : ProblemType.values()) {
            if (value.getValue().equals(source)) {
                return value;
            }
        }
        return null;
    }
}
