package com.anishan.api.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ControllerLog {


    /**
     * 微服务名
     */
    String service() default "";

    /**
     * controller -> RequestMapping
     * 前后不用加 /
     */
    String api() default "";

    /**
     * method -> Request/Get/Put/Delete Mapping
     * 前后不用加 /
     */
    String operation() default "";

    String desc() default "";


}
