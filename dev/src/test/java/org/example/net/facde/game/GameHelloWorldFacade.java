package org.example.net.facde.game;

import org.example.net.facde.cross.HelloWorld;
import org.example.net.proxy.RpcMethod;

/**
 * 游戏服HelloWorld(只是用来接收结果而已)
 *
 * @author ZJP
 * @since 2021年07月25日 19:07:06
 **/
public class GameHelloWorldFacade {

  @RpcMethod(res = HelloWorld.ECHO)
  public void echoRes(Object o) {

  }

}
