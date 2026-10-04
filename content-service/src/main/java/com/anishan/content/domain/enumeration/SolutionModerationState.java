package com.anishan.content.domain.enumeration;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.anishan.commons.enumeration.Enumerator;

public enum SolutionModerationState implements IEnum<String>, Enumerator<String> {
    NORMAL,
    AUTHOR_ONLY;

    @Override
    public String getValue() { return name(); }

    @Override
    public String value() { return name(); }
}
