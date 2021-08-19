package org.example.net;

@ReqModule(HelloWorld.HELLO_WORLD_MODULE)
public interface HelloWorld {

  int HELLO_WORLD_MODULE = 100;

  /** 回声协议 */
  int ECHO = 110;
  int DO_NOTHING = 100;

  /**
   * 回声
   *
   * @since 2021年07月25日 19:00:22
   */
  @ReqMethod(ECHO)
  Object echo(Object o);

  @ReqMethod(DO_NOTHING)
  void doNothing();


}
