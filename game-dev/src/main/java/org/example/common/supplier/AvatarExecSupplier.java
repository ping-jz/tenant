package org.example.common.supplier;

import io.netty.util.AttributeKey;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.example.common.model.AvatarId;
import org.example.exec.VirutalExecutors;
import org.example.net.Connection;
import org.example.net.Message;
import org.example.net.handler.RpcExecSupplier;

/**
 * 根据网络通道提供的信息，返回玩家对应的Executor
 *
 * @author zhongjianping
 * @since 2024/12/4 13:37
 */
public interface AvatarExecSupplier extends RpcExecSupplier {

  @Override
  default Executor get(Connection c, Message m) {
    AttributeKey<AvatarId> key = AttributeKey.valueOf("AvatarId");
    AvatarId avatarIdentity = Objects
        .requireNonNull(c.channel().attr(key).get(), "玩家ID为空，请检查");
    return VirutalExecutors.commonPool().getExecutor(avatarIdentity);
  }

}
