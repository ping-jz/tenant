package org.example.game.facade.example;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.example.net.Connection;
import org.example.net.Message;
import org.example.net.handler.Handler;
import org.example.serde.CommonSerializer;

public class GameFacdeReq implements Handler {

  private GameFacade facade;
  private CommonSerializer serializer;

  @Override
  public byte[] invoke(Connection connection, Message message) {
    String res =  facade.req();
    ByteBuf buf = Unpooled.buffer();
    serializer.writeObject(buf, res);
    return buf.array();
  }

}
