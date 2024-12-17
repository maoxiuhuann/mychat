package com.ezchat.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 通过在方法上添加 @GlobalInterceptor 注解，指示该方法需要进行登录校验或管理员权限校验。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalInterceptor {

    //校验管理员
    boolean checkAdmin() default false;
    //校验登录
    boolean checkLogin() default true;


}
