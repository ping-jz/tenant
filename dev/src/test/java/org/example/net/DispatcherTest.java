package org.example.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.example.handler.HandlerRegistry;
import org.example.handler.Packet;
import org.example.net.codec.MessageCodec;
import org.example.serde.CommonSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class DispatcherTest {

  /** 分发器 */
  private static Dispatcher dispatcher;
  /** 序列化实现 */
  private static CommonSerializer serializer;

  @BeforeAll
  static void init() {
    HandlerRegistry registry = new HandlerRegistry();
    registry.registeHandlers(registry.findHandler(new HelloWorldFacade()));
    dispatcher = new Dispatcher(LoggerFactory.getLogger(Dispatcher.class), registry);
    serializer = new CommonSerializer();
  }

  @Test
  void helloWorldTest() {
    EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec(Integer.BYTES, serializer));

    Message echoRequest = new Message();
    echoRequest.proto(HelloWorldFacade.ECHO);
    echoRequest.optIdx(0);
    echoRequest.packet("HelloWorld");

    dispatcher.doDispatcher(channel, echoRequest);
    channel.flush();

    ByteBuf buf = channel.readOutbound();

    assertTrue(0 < buf.readInt());
    assertEquals(Math.negateExact(echoRequest.proto()), buf.readInt());
    assertEquals(echoRequest.optIdx(), buf.readInt());
    assertEquals(echoRequest.packet(), serializer.readObject(buf));

    channel.finishAndReleaseAll();
  }

  @Test
  void multiHelloWorldTest() {
    EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec(Integer.BYTES, serializer));

    Message echoRequest = new Message();
    echoRequest.proto(HelloWorldFacade.ECHO);
    echoRequest.optIdx(0);
    echoRequest.packet(new String[]{"Hello", "World"});

    dispatcher.doDispatcher(channel, echoRequest);
    channel.flush();

    ByteBuf buf = channel.readOutbound();

    assertTrue(0 < buf.readInt());
    assertEquals(Math.negateExact(echoRequest.proto()), buf.readInt());
    assertEquals(echoRequest.optIdx(), buf.readInt());
    assertArrayEquals((Object[]) echoRequest.packet(), serializer.read(buf));

    channel.finishAndReleaseAll();
  }

  /**
   * 世界你好，门面
   *
   * @author ZJP
   * @since 2021年07月22日 21:58:02
   **/
  private static class HelloWorldFacade {

    private static final int ECHO = 1;

    /**
     * 回声
     *
     * @param str 内容
     * @since 2021年07月22日 21:58:45
     */

    @Packet(ECHO)
    public Object echo(Object str) {
      return str;
    }

    public void doNothing() {

    }

  }


}
