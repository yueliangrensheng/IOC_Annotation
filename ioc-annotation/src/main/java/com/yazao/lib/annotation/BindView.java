package com.yazao.lib.annotation;

import androidx.annotation.IdRes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)//编译时被保留的时间长短
@Target(ElementType.FIELD)// 注解范围为类成员（构造方法、方法、成员变量）
public @interface BindView {
    /** View ID to which the field will be bound. */
    @IdRes int value();
}
