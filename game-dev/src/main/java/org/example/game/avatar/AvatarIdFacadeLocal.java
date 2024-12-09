package org.example.game.avatar;

import java.util.Arrays;
import java.util.Objects;
import org.example.common.model.AvatarId;
import org.example.common.model.ReqMove;
import org.example.common.model.ResMove;
import org.example.net.anno.LocalRpc;
import org.example.net.anno.Req;
import org.example.net.handler.IdExecSupplier;

/**
 * 游戏门面(文档生产插件测试)
 *
 * @author ZJP
 * @since 2021年09月27日 15:54:54
 **/
@LocalRpc
public class AvatarIdFacadeLocal implements IdExecSupplier<AvatarId> {

  public AvatarId id;

  public AvatarIdFacadeLocal() {
  }

  @Req
  public AvatarId echo(AvatarId id) {
    return id;
  }

  @Req
  public void set(AvatarId id) {
    this.id = id;
  }

  @Req
  public int callback(AvatarId id, boolean boolean1, byte[] byte1, short short1,
      char char1, int int1, long long1,
      float float1, double double1, ReqMove reqMove, ResMove resMove) {
    return Objects.hash(id, boolean1, Arrays.hashCode(byte1), short1, char1, int1, long1, float1,
        double1, reqMove, resMove);
  }
}


