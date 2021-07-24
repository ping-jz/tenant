package org.example.handler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 根据提供的协议编号，通过{@link HandlerRegistry}将此方法注册为请求处理者
 *
 * @author ZJP
 * @since 2021年07月22日 21:59:25
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Packet {

  /**
   * 请求协议号，返回协议号为{@link Packet#req()}负数形式
   *
   * @since 2021年07月22日 22:00:32
   */
  int req();
}
