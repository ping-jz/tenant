package org.example.net;

import org.example.net.anno.Req;
import org.example.net.anno.RpcModule;

@RpcModule
public interface HelloWorld {

  /** 回声协议 */
  int ECHO = 110;
  int DO_NOTHING = 100;

  /**
   * 回声
   *
   * @since 2021年07月25日 19:00:22
   */
  @Req(ECHO)
  Object echo(Object o);

  @Req(DO_NOTHING)
  void doNothing();


}
