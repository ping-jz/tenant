package org.example.game.facade.example;

import java.util.Arrays;
import java.util.Objects;
import org.example.common.model.ReqMove;
import org.example.common.model.ResMove;
import org.example.common.net.annotation.RpcModule;
import org.example.common.net.generated.invoker.GameFacadeInvoker;
import org.example.net.Connection;
import org.example.net.anno.Req;

/**
 * 游戏门面(文档生产插件测试)
 *
 * @author ZJP
 * @since 2021年09月27日 15:54:54
 **/
@RpcModule
public class GameFacade {

  private GameFacadeInvoker facadeInvoker;

  public GameFacade(GameFacadeInvoker facadeInvoker) {
    this.facadeInvoker = facadeInvoker;
  }

  /**
   * 回声
   *
   * @author ZJP
   * @since 2021年09月27日 16:01:08
   **/
  @Req
  public void echo(Connection connection, String str) {
    facadeInvoker.of(connection).echo(str);
  }

  @Req
  public void nothing() {
  }


  @Req
  public int callback(boolean boolean1, byte[] byte1, short short1,
      char char1, int int1, long long1,
      float float1, double double1, ReqMove reqMove, ResMove resMove) {
    return Objects.hash(boolean1, Arrays.hashCode(byte1), short1, char1, int1, long1, float1,
        double1, reqMove, resMove);
  }
}
