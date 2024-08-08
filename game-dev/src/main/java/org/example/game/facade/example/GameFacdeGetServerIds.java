package org.example.game.facade.example;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.example.net.Connection;
import org.example.net.Message;
import org.example.net.handler.Handler;
import org.example.serde.CommonSerializer;

public class GameFacdeGetServerIds implements Handler {

  private GameFacade facade;
  private CommonSerializer serializer;

  @Override
  public byte[] invoke(Connection connection, Message message) {

    ByteBuf resBuf = Unpooled.buffer();
    List<Integer> res = facade.getServerIds();
    serializer.writeObject(resBuf, res);
    return resBuf.array();
  }
}
