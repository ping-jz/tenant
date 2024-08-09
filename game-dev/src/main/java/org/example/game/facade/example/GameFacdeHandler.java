package org.example.game.facade.example;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.example.net.Connection;
import org.example.net.Message;
import org.example.net.handler.Handler;
import org.example.serde.CommonSerializer;
import org.example.serde.NettyByteBufUtil;

public class GameFacdeHandler implements Handler {

  private GameFacade facade;
  private CommonSerializer serializer;

  public GameFacdeHandler(GameFacade facade, CommonSerializer serializer) {
    this.facade = facade;
    this.serializer = serializer;
  }

  @Override
  public byte[] invoke(Connection connection, Message message) {
    return switch (message.proto()) {
      case 200 -> echo(connection, message);
      case 202 -> ok(connection, message);
      default -> throw new UnsupportedOperationException(
          String.format("【RPC】GameFacade 无法处理消息，原因:【缺少对应方法】，消息ID:【%s】",
              message.proto()));
    };
  }

  public byte[] echo(Connection connection, Message message) {
    ByteBuf packet = Unpooled.wrappedBuffer(message.packet());

    String str = serializer.read(packet);
    String res = facade.echo(str);

    ByteBuf resBuf = Unpooled.buffer();
    serializer.writeObject(resBuf, res);
    return NettyByteBufUtil.readBytes(resBuf);
  }


  public byte[] ok(Connection connection, Message message) {
    String res = facade.ok();
    ByteBuf buf = Unpooled.buffer();
    serializer.writeObject(buf, res);

    return NettyByteBufUtil.readBytes(buf);
  }
}
