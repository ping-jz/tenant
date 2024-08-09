package org.example.common.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.ArrayUtils;
import org.example.common.net.proxy.invoker.GameFacdeInvoker;
import org.example.game.facade.example.GameFacade;
import org.example.game.facade.example.GameFacdeHandler;
import org.example.net.Connection;
import org.example.net.DefaultDispatcher;
import org.example.net.codec.MessageCodec;
import org.example.net.handler.DispatcherHandler;
import org.example.net.handler.HandlerRegistry;
import org.example.serde.CommonSerializer;
import org.example.serde.NettyByteBufUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;

public class GameFacdeTest {

  private static EmbeddedChannel embeddedChannel;
  private static CommonSerializer commonSerializer;
  private static GameFacdeInvoker invoker;
  private static final int ECHO = 200;
  private static final int OK = 202;

  @BeforeAll
  public static void beforeAll() {
    commonSerializer = new CommonSerializer();

    GameFacade facade = new GameFacade();

    HandlerRegistry handlerRegistry = new HandlerRegistry();
    GameFacdeHandler handler = new GameFacdeHandler(facade, commonSerializer);
    handlerRegistry.registeHandler(ECHO, handler);
    handlerRegistry.registeHandler(OK, handler);

    embeddedChannel = new EmbeddedChannel();
    embeddedChannel.attr(Connection.CONNECTION).set(new Connection(embeddedChannel, 1));
    embeddedChannel.pipeline()
        .addLast(new MessageCodec())
        .addLast(new DispatcherHandler(new DefaultDispatcher(handlerRegistry)));

    invoker = new GameFacdeInvoker(new ConnectionManager(), commonSerializer);
  }

  @RepeatedTest(10)
  public void echo() {
    String str = String.valueOf(ThreadLocalRandom.current().nextLong());
    invoker.of(embeddedChannel.attr(Connection.CONNECTION).get())
        .echo(str);

    //验证请求的信息
    {
      ByteBuf reqBuf = embeddedChannel.readOutbound();
      reqBuf.markReaderIndex();

      reqBuf.skipBytes(Integer.BYTES);
      Assertions.assertEquals(ECHO, NettyByteBufUtil.readInt32(reqBuf));
      Assertions.assertEquals(0, NettyByteBufUtil.readInt32(reqBuf));
      ByteBuf buf = Unpooled.buffer();
      commonSerializer.writeObject(buf, str);
      Assertions.assertArrayEquals(NettyByteBufUtil.readBytes(buf), NettyByteBufUtil.readBytes(reqBuf));
      Assertions.assertFalse(reqBuf.isReadable());
      Assertions.assertNull(embeddedChannel.readOutbound());

      reqBuf.resetReaderIndex();
      embeddedChannel.writeInbound(reqBuf);
    }

    //验证返回的结果
    {
      ByteBuf resBuf = embeddedChannel.readOutbound();
      resBuf.skipBytes(Integer.BYTES);
      Assertions.assertEquals(-ECHO, NettyByteBufUtil.readInt32(resBuf));
      Assertions.assertEquals(0, NettyByteBufUtil.readInt32(resBuf));
      Assertions.assertEquals(str, commonSerializer.read(resBuf));

      Assertions.assertFalse(resBuf.isReadable());
      Assertions.assertNull(embeddedChannel.readOutbound());
    }
  }

  @RepeatedTest(10)
  public void ok() {
    invoker.of(embeddedChannel.attr(Connection.CONNECTION).get())
        .ok();

    //验证请求的信息
    {
      ByteBuf reqBuf = embeddedChannel.readOutbound();
      reqBuf.markReaderIndex();
      reqBuf.skipBytes(Integer.BYTES);
      Assertions.assertEquals(OK, NettyByteBufUtil.readInt32(reqBuf));
      Assertions.assertEquals(0, NettyByteBufUtil.readInt32(reqBuf));
      Assertions.assertArrayEquals(ArrayUtils.EMPTY_BYTE_ARRAY, NettyByteBufUtil.readBytes(reqBuf));
      Assertions.assertFalse(reqBuf.isReadable());
      Assertions.assertNull(embeddedChannel.readOutbound());

      reqBuf.resetReaderIndex();
      embeddedChannel.writeInbound(reqBuf);
    }

    //验证返回的结果
    {
      ByteBuf resBuf = embeddedChannel.readOutbound();
      resBuf.skipBytes(Integer.BYTES);
      Assertions.assertEquals(-OK, NettyByteBufUtil.readInt32(resBuf));
      Assertions.assertEquals(0, NettyByteBufUtil.readInt32(resBuf));
      Assertions.assertEquals("ok", commonSerializer.read(resBuf));

      Assertions.assertFalse(resBuf.isReadable());
      Assertions.assertNull(embeddedChannel.readOutbound());
    }
  }


}
