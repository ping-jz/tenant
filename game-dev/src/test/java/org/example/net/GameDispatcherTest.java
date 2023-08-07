package org.example.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.example.net.anno.Req;
import org.example.net.anno.RpcModule;
import org.example.net.codec.MessageCodec;
import org.example.net.handler.HandlerRegistry;
import org.example.serde.CommonSerializer;
import org.example.serde.NettyByteBufUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class GameDispatcherTest {

  /**
   * 分发器
   */
  private static Dispatcher dispatcher;
  /**
   * 序列化实现
   */
  private static CommonSerializer serializer;

  @BeforeAll
  public static void init() {
    HandlerRegistry registry = new HandlerRegistry();
    registry.registerHandlers(new HelloWorldFacade());
    dispatcher = new DefaultDispatcher(LoggerFactory.getLogger(Dispatcher.class), registry);
    serializer = new CommonSerializer();
  }

  @Test
  public void helloWorldTest() {
    EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec(serializer));
    new Connection(channel, Connection.IdGenerator.incrementAndGet());

    Message echoRequest = new Message();
    echoRequest.proto(HelloWorldFacade.ECHO);
    echoRequest.msgId(0);
    echoRequest.packet("HelloWorld");

    dispatcher.dispatcher(channel, echoRequest);
    channel.flush();

    ByteBuf buf = channel.readOutbound();

    assertTrue(0 < buf.readInt());
    assertEquals(0, NettyByteBufUtil.readInt32(buf));
    assertEquals(0, NettyByteBufUtil.readInt32(buf));
    assertEquals(Math.negateExact(echoRequest.proto()), NettyByteBufUtil.readInt32(buf));
    assertEquals(echoRequest.msgId(), NettyByteBufUtil.readInt32(buf));
    assertEquals(echoRequest.packet(), serializer.readObject(buf));

    channel.finishAndReleaseAll();
  }

  @Test
  public void multiHelloWorldTest() {
    EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec(serializer));
    new Connection(channel, Connection.IdGenerator.incrementAndGet());

    for (int i = 0; i < 5; i++) {
      Message echoRequest = new Message();
      echoRequest.proto(HelloWorldFacade.ECHO);
      echoRequest.msgId(0);
      echoRequest.wrapArray(new String[]{"Hello", "World", Integer.toString(i)});

      dispatcher.dispatcher(channel, echoRequest);
      channel.flush();

      ByteBuf buf = channel.readOutbound();

      assertTrue(0 < buf.readInt());
      assertEquals(0, NettyByteBufUtil.readInt32(buf));
      assertEquals(0, NettyByteBufUtil.readInt32(buf));
      assertEquals(Math.negateExact(echoRequest.proto()), NettyByteBufUtil.readInt32(buf));
      assertEquals(echoRequest.msgId(), NettyByteBufUtil.readInt32(buf));
      assertArrayEquals((Object[]) echoRequest.packet(), new Object[]{serializer.read(buf)});
    }

    channel.finishAndReleaseAll();
  }

  /**
   * 世界你好，门面
   *
   * @author ZJP
   * @since 2021年07月22日 21:58:02
   **/
  @RpcModule
  private static class HelloWorldFacade {

    private static final int ECHO = 1;

    /**
     * 回声
     *
     * @param str 内容
     * @since 2021年07月22日 21:58:45
     */

    @Req(ECHO)
    public Object echo(Object str) {
      return str;
    }

    public void doNothing() {

    }

  }


}
