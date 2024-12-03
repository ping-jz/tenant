package org.example.net.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.concurrent.CompletableFuture;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.example.net.Message;
import org.example.net.Util;
import org.example.serde.CommonSerializer;
import org.example.serde.NettyByteBufUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacadeCallBack implements Handler {

  static final Logger logger = LoggerFactory.getLogger("CallBackLogger");

  private final CommonSerializer serializer;
  private final ConnectionManager manager;

  public FacadeCallBack(ConnectionManager manager, CommonSerializer serializer) {
    this.serializer = serializer;
    this.manager = manager;
  }

  void callback(Connection c, Message m) throws Exception {
    final int msgId = NettyByteBufUtil.readInt32(m.packet());
    final CompletableFuture<Integer> futureVar = manager.removeInvokeFuture(msgId);
    if (futureVar == null) {
      logger.error(
          "寻找回调函数失败, 可能原因：【回调函数过期/回调函数不存在】, 消息ID:{}, 链接地址:{} ",
          msgId,
          c.channel().remoteAddress());
      return;
    }
    ByteBuf buf = Unpooled.wrappedBuffer(m.packet());
    futureVar.complete(serializer.read(buf));
  }

  public int id() {
    return Util.CALL_BACK_ID;
  }

  @Override
  public void invoke(Connection c, Message m) throws Exception {
    callback(c, m);
  }
}