package org.example.common.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.ArrayUtils;
import org.example.common.model.ReqMove;
import org.example.common.model.ReqMoveSerde;
import org.example.common.model.ResMove;
import org.example.common.model.ResMoveSerde;
import org.example.common.net.generated.callback.GameFacadeCallBack;
import org.example.common.net.generated.invoker.GameFacadeInvoker;
import org.example.game.facade.example.GameFacade;
import org.example.game.facade.example.GameFacadeHandler;
import org.example.net.Connection;
import org.example.net.DefaultDispatcher;
import org.example.net.codec.MessageCodec;
import org.example.net.handler.DispatcherHandler;
import org.example.serde.CommonSerializer;
import org.example.serde.NettyByteBufUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;

public class GameFacdeTest {

  private static EmbeddedChannel embeddedChannel;
  private static CommonSerializer commonSerializer;
  private static GameFacadeInvoker invoker;
  private static final int ECHO = 200;
  private static final int OK = 202;

  @BeforeAll
  public static void beforeAll() {
    commonSerializer = new CommonSerializer();
    commonSerializer.registerSerializer(ReqMove.class, new ReqMoveSerde(commonSerializer));
    commonSerializer.registerSerializer(ResMove.class, new ResMoveSerde(commonSerializer));

    DefaultDispatcher handlerRegistry = handlerRegistry();

    embeddedChannel = new EmbeddedChannel();
    embeddedChannel.attr(Connection.CONNECTION).set(new Connection(embeddedChannel, 1));
    embeddedChannel.pipeline()
        .addLast(new MessageCodec())
        .addLast(new DispatcherHandler(handlerRegistry));

    invoker = new GameFacadeInvoker(new ConnectionManager()::connection, commonSerializer);
  }

  private static DefaultDispatcher handlerRegistry() {
    GameFacade facade = new GameFacade();

    DefaultDispatcher handlerRegistry = new DefaultDispatcher();
    GameFacadeHandler handler = new GameFacadeHandler(facade, commonSerializer);
    for (int id : GameFacadeHandler.protos) {
      handlerRegistry.registeHandler(id, handler);
    }

    GameFacadeCallBack gameFacadeCallBack = new GameFacadeCallBack(commonSerializer);
    for (int id : GameFacadeCallBack.protos) {
      handlerRegistry.registeHandler(id, gameFacadeCallBack);
    }
    return handlerRegistry;
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
      ByteBuf buf = Unpooled.buffer();
      commonSerializer.writeObject(buf, str);
      Assertions.assertArrayEquals(NettyByteBufUtil.readBytes(buf),
          NettyByteBufUtil.readBytes(reqBuf));
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
      Assertions.assertEquals("ok", commonSerializer.read(resBuf));

      Assertions.assertFalse(resBuf.isReadable());
      Assertions.assertNull(embeddedChannel.readOutbound());
    }
  }

  @RepeatedTest(10)
  public void all() {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    boolean boolean1 = random.nextBoolean();
    byte[] byte1 = new byte[random.nextInt(10)];
    random.nextBytes(byte1);
    short short1 = (short) random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
    char char1 = (char) random.nextInt();
    int int1 = random.nextInt();
    long long1 = random.nextLong();
    float float1 = random.nextFloat();
    double double1 = random.nextDouble();

    ReqMove reqMove = new ReqMove();
    reqMove.setId(random.nextInt());
    reqMove.setX(random.nextFloat());
    reqMove.setY(random.nextFloat());

    ResMove resMove = new ResMove();
    resMove.setId(random.nextInt());
    resMove.setX(random.nextFloat());
    resMove.setY(random.nextFloat());
    resMove.setDir(random.nextInt());

    int hashcode = Objects.hash(boolean1, Arrays.hashCode(byte1), short1, char1, int1, long1,
        float1, double1, reqMove, resMove);

    invoker.of(embeddedChannel.attr(Connection.CONNECTION).get())
        .all(boolean1, byte1, short1, char1, int1, long1, float1, double1, reqMove, resMove);

    //验证请求的信息
    {
      ByteBuf reqBuf = embeddedChannel.readOutbound();
      embeddedChannel.writeInbound(reqBuf);
    }

    //验证返回的结果
    {
      ByteBuf resBuf = embeddedChannel.readOutbound();
      resBuf.skipBytes(Integer.BYTES);
      //511882096是自动生成的，自己看下代码里的值
      Assertions.assertTrue(NettyByteBufUtil.readInt32(resBuf) < 0);
      Assertions.assertEquals(hashcode, NettyByteBufUtil.readInt32(resBuf));

      Assertions.assertFalse(resBuf.isReadable());
      Assertions.assertNull(embeddedChannel.readOutbound());
    }
  }

  @RepeatedTest(10)
  public void callBack() throws Exception {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    boolean boolean1 = random.nextBoolean();
    byte[] byte1 = new byte[random.nextInt(10)];
    random.nextBytes(byte1);
    short short1 = (short) random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
    char char1 = (char) random.nextInt();
    int int1 = random.nextInt();
    long long1 = random.nextLong();
    float float1 = random.nextFloat();
    double double1 = random.nextDouble();

    ReqMove reqMove = new ReqMove();
    reqMove.setId(random.nextInt());
    reqMove.setX(random.nextFloat());
    reqMove.setY(random.nextFloat());

    ResMove resMove = new ResMove();
    resMove.setId(random.nextInt());
    resMove.setX(random.nextFloat());
    resMove.setY(random.nextFloat());
    resMove.setDir(random.nextInt());

    int hashcode = Objects.hash(boolean1, Arrays.hashCode(byte1), short1, char1, int1, long1,
        float1, double1, reqMove, resMove);

    CompletableFuture<Integer> callback = invoker.of(
            embeddedChannel.attr(Connection.CONNECTION).get())
        .callback(boolean1, byte1, short1, char1, int1, long1, float1, double1, reqMove, resMove);

    {
      //第一次向callback发送消息，然后callback返回结果
      ByteBuf reqBuf1 = embeddedChannel.readOutbound();
      embeddedChannel.writeInbound(reqBuf1);

      //然后在将结果发送一次
      ByteBuf resBuf2 = embeddedChannel.readOutbound();
      embeddedChannel.writeInbound(resBuf2);
    }

    Assertions.assertEquals(hashcode, callback.get(1, TimeUnit.SECONDS));
  }


}
