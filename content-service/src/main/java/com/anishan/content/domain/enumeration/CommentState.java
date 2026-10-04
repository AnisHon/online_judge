package com.anishan.content.domain.enumeration;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.anishan.commons.enumeration.Enumerator;

public enum CommentState implements IEnum<String>, Enumerator<String> {
    VISIBLE,
    AUTHOR_DELETED,
    ADMIN_DELETED;

    @Override
    public String getValue() { return name(); }

    @Override
    public String value() { return name(); }
}
