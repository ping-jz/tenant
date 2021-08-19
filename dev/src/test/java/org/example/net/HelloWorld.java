package org.example.net;

@ReqModule(HelloWorld.HELLO_WORLD_MODULE)
public interface HelloWorld {

  int HELLO_WORLD_MODULE = 100;

  /** 回声协议 */
  int ECHO = 110;

  /**
   * 回声
   *
   * @since 2021年07月25日 19:00:22
   */
  @ReqMethod(ECHO)
  Object echo(Object o);

  //1
  void doNothing();


}
