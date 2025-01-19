package com.anishan.api.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ReflectUtil;
import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.commons.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NikeNameUtil {

    private static UserInternalClient userInternalClient = null;

    // 注入 ApplicationContext
    @Autowired
    public void setApplicationContext(UserInternalClient userInternalClient) {
        NikeNameUtil.userInternalClient = userInternalClient;
    }

    public static <T> void setNikeName(List<T> list, String nikeNameField, String userIdField) {

        List<Long> ids = list
                .stream()
                .map(item -> (Long)ReflectUtil.getFieldValue(item, userIdField))
                .distinct()
                .collect(Collectors.toList());

        if (CollectionUtil.isEmpty(ids)) {
            return;
        }

        Map<Long, String> map = userInternalClient.nikeName(ids).getData();

        list.forEach(item -> {
            Long id = (Long) ReflectUtil.getFieldValue(item, userIdField);
            ReflectUtil.setFieldValue(item, nikeNameField, map.get(id));
        });


    }

    public static <T> void setNikeName(List<T> list) {
        setNikeName(list, "nikeName", "userId");
    }

}
