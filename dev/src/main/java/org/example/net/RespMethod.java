package org.example.net;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 返回方法
 *
 * @author ZJP
 * @since 2021年07月25日 15:00:25
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RespMethod {

  /**
   * 对应的请求{@link ReqMethod#value()},代码会自动转为负数形成对应关系
   *
   * @since 2021年07月25日 14:29:21
   */
  int value();

}
