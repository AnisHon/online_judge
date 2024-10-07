package com.anishan.commons.util;

import cn.hutool.core.util.StrUtil;
import com.anishan.commons.annotation.ConditionColumn;
import com.anishan.commons.annotation.SortedColumn;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class MysqlMappingUtils {
    /**
     * 将成员应映射一个map，只映射有SortedColumn注解的
     * @param clazz 需要映射的类
     * @return 映射结果默认Camel -> Underline
     */
    public static Map<String, String> mapColumn(Class<?> clazz) {
        Map<String, String> map = new HashMap<>();
        for (Field declaredField : clazz.getDeclaredFields()) {
            declaredField.setAccessible(true);

            boolean annotationPresent = declaredField.isAnnotationPresent(SortedColumn.class);

            if (annotationPresent) {
                String name = declaredField.getName();
//                String name_underline = StringUtils.camelToUnderline(name);
                String mappingName = doMapColumn(declaredField);

                map.put(name, mappingName);
            }
        }
        return map;
    }


    private static String doMapColumn(Field declaredField) {
        // 获取SortedColumn的value
        SortedColumn annotation = declaredField.getAnnotation(SortedColumn.class);
        String value = annotation.value();
        String name = declaredField.getName();

        // value不为空就用value的值，为空就做camelToUnderline映射
        return StrUtil.isAllBlank(value) ?
                StringUtils.camelToUnderline(name) : value;
    }


    /**
     * 生成Wrapper，专门用于多条件查询，查找非Null，只会针对有ConditionColumn注解的成员
     * @param t 参数类对象
     * @param type 用与获取实体类类型
     * @return 返回一个构建好的Wrapper
     * @param <T> 参数的类类型
     * @param <K> 最终结果的实体类类型
     */
    public static <T, K> Wrapper<K> buildWrapper(T t, Class<K> type) {

        Class<?> clazz = t.getClass();
        Field[] declaredFields = clazz.getDeclaredFields();

        return doBuildWrapper(declaredFields, t);
    }

    private static <T> Wrapper<T> doBuildWrapper(Field[] declaredFields, Object obj) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        // 遍历成员
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);

            // 判断获取ConditionColumn的成员
            if (!declaredField.isAnnotationPresent(ConditionColumn.class)) {
                continue;
            }
            ConditionColumn annotation = declaredField.getAnnotation(ConditionColumn.class);

            loadByAnnotation(annotation, declaredField, obj, queryWrapper);
        }
        return queryWrapper;
    }


    private static <T> void loadByAnnotation(ConditionColumn annotation, Field declaredField, Object obj, QueryWrapper<T> queryWrapper) {
        String value = annotation.value();
        try {
            Object val = declaredField.get(obj);

            // 判断成员的annotation.name()是否存在，空的就不存在直接自动映射
            String column =
                    StrUtil.isAllBlank(annotation.name()) ?
                            StringUtils.camelToUnderline(declaredField.getName())
                            :
                            annotation.name();

            // 判断用like还是其他的
            if ("like".equals(value)) {
                queryWrapper.like(val != null, column, val);
            } else {
                queryWrapper.eq(val != null, column, val);
            }

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    @SafeVarargs
    public static <T> LocalDateTime getUpdateTime(IService<T> service, SFunction<T, ?> idColumn, Long id, SFunction<T, ?>... columns) {
        return service.getObj(new LambdaQueryWrapper<T>()
                        .select(columns)
                        .eq(idColumn, id),
                k -> (LocalDateTime) k
        );
    }

}
