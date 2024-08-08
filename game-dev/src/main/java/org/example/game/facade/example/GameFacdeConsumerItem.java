package org.example.game.facade.example;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.example.common.model.CommonRes;
import org.example.net.Connection;
import org.example.net.Message;
import org.example.net.handler.Handler;
import org.example.serde.Serializer;

public class GameFacdeConsumerItem implements Handler {

  private GameFacade facade;
  private Serializer<Object> commonSerializer;


  @Override
  public byte[] invoke(Connection connection, Message message) {
    ByteBuf byteBuf = Unpooled.wrappedBuffer(message.packet());
    List<Long> ids = commonSerializer.read(byteBuf);

    CommonRes<Integer> res = facade.consumerItem(ids);
    ByteBuf resBuf = Unpooled.buffer();
    commonSerializer.writeObject(resBuf, res);
    return resBuf.array();
  }
}
