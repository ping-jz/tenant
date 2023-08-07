package org.example.game.facade.example;


import org.example.game.facade.example.model.ReqMove;
import org.example.game.facade.example.model.ResMove;
import org.example.net.anno.Req;
import org.example.net.anno.RpcModule;

/**
 * 世界门面(文档生产插件测试)
 *
 * @author ZJP
 * @since 2021年09月27日 15:04:09
 **/
@RpcModule
public class WorldFacade {

  /**
   * 你说什么我就说什么
   *
   * @param str 内容
   * @author ZJP
   * @since 2021年09月27日 15:15:19
   **/
  @Req(100)
  public String echo(String str) {
    return str;
  }

  /**
   * 移动请求
   *
   * @param move 移动数据
   * @since 2021年09月27日 15:33:00
   */
  @Req(101)
  public ResMove move(ReqMove move) {
    ResMove moveRes = new ResMove();
    moveRes.id(move.id());
    moveRes.x(move.x());
    moveRes.y(move.y());
    moveRes.dir(1);
    return moveRes;
  }
}
