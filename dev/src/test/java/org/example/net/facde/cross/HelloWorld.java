package org.example.net.facde.cross;

import org.example.net.proxy.RpcMethod;
import org.example.net.proxy.RpcModule;

@RpcModule(HelloWorld.HELLO_WORLD_MODULE)
public interface HelloWorld {

  int HELLO_WORLD_MODULE = 100;

  /** 回声协议 */
  int ECHO = 110;

  /**
   * 回声
   *
   * @since 2021年07月25日 19:00:22
   */
  @RpcMethod(ECHO)
  Object echo(Object o);

  //1
  void doNothing();


}
