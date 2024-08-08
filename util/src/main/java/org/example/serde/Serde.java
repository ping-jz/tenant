package org.example.serde;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 解析器生成标记
 * 所有非final和static字段都会被作为序列化目标，并且用户必须提供对应的getter和setter
 * */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Serde {

  /** 类型ID,默认完整类名的HashCode */
  int value() default 0;
}
