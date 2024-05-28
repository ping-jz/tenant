package org.example.net.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 远程调用方法标记
 *
 * @author ZJP
 * @since 2021年07月25日 15:00:25
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Req {

  /**
   *
   * @return 方法ID
   * <p>手动分配:范围内选一个 </p>
   * <p>随机分配:类型加方法的HashCode</p>
   * @since 2021年07月25日 14:29:21
   */
  int value() default 0;

}
