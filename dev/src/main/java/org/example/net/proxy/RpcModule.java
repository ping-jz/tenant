package org.example.net.proxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 远程调用模块标记
 *
 * @author ZJP
 * @since 2021年07月25日 14:27:59
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcModule {

  /**
   * RPC模块ID:每个模块ID相隔100，默认零开始计数(0,100,200)
   * <p>
   * 然后
   * <p>A模块:0</p>
   * <p>B模块:100</p>
   * <p>C模块:200</p>
   * </p>
   *
   * @since 2021年07月25日 14:29:21
   */
  int value();
}
