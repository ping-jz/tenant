package org.example.net.facde.cross;

import org.example.net.HelloWorld;

/**
 * 世界你好，门面
 *
 * @author ZJP
 * @since 2021年07月22日 21:58:02
 **/
public class CrossHelloWorldFacade implements HelloWorld {

  /**
   * 回声
   *
   * @param str 内容
   * @since 2021年07月22日 21:58:45
   */
  @Override
  public Object echo(Object str) {
    return str;
  }

  @Override
  public void doNothing() {

  }

}
