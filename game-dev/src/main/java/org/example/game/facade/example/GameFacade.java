package org.example.game.facade.example;

import org.example.net.anno.Req;
import org.example.net.anno.RpcModule;

/**
 * 游戏门面(文档生产插件测试)
 *
 * @author ZJP
 * @since 2021年09月27日 15:54:54
 **/
@RpcModule
public class GameFacade {


  /**
   * 回声
   *
   * @author ZJP
   * @since 2021年09月27日 16:01:08
   **/
  @Req(200)
  public String echo(String str) {
    return str;
  }

  /**
   * 请求我
   *
   * @author ZJP
   * @since 2021年09月28日 12:16:00
   **/
  @Req(202)
  public String ok() {
    return "ok";
  }



}
