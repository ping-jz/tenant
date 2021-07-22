package org.example.dispatcher.facde;

import org.example.dispatcher.Packet;

/**
 * 世界你好，门面
 *
 * @author ZJP
 * @since 2021年07月22日 21:58:02
 **/
public class HelloWorldFacade {

  /** 回声协议 */
  public static final int ECHO_REQ = 1;
  public static final int ECHO_RES = 1;


  /**
   * 回声
   *
   * @param str 内容
   * @since 2021年07月22日 21:58:45
   */
  @Packet(req = ECHO_REQ, res = ECHO_RES)
  public String echo(String str) {
    return str;
  }

}
