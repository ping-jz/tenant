package org.example.net.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import org.example.net.Message;
import org.example.serde.CommonSerializer;
import org.example.serde.NettyByteBufUtil;
import org.junit.jupiter.api.Test;

public class MessageCodecTest {

  @Test
  public void outBoundTest() {
    CommonSerializer serializer = new CommonSerializer();
    EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec(serializer));

    Message message = new Message();
    message.proto(ThreadLocalRandom.current().nextInt());
    message.msgId(ThreadLocalRandom.current().nextInt());
    message.packet("Hello World".getBytes(StandardCharsets.UTF_8));
    channel.writeOutbound(message);

    ByteBuf out = channel.readOutbound();

    int length = out.readInt();
    assertTrue(0 < length);

    assertEquals(message.proto(), NettyByteBufUtil.readInt32(out));
    assertEquals(message.msgId(), NettyByteBufUtil.readInt32(out));
    byte[] bytes = new byte[out.readableBytes()];
    out.readBytes(bytes);
    assertArrayEquals(message.packet(), bytes);
    assertEquals(0, out.readableBytes());

    channel.finishAndReleaseAll();
  }

  @Test
  public void inBoundTest() {
    CommonSerializer serializer = new CommonSerializer();
    EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec(serializer));

    int proto = ThreadLocalRandom.current().nextInt();
    int optIdx = ThreadLocalRandom.current().nextInt();
    byte[] helloWorld = "Hello World".getBytes(StandardCharsets.UTF_8);

    ByteBuf inBuf = Unpooled.buffer();
    int start = inBuf.writerIndex();
    inBuf.writerIndex(Integer.BYTES);
    NettyByteBufUtil.writeInt32(inBuf, proto);
    NettyByteBufUtil.writeInt32(inBuf, optIdx);
    inBuf.writeBytes(helloWorld);

    inBuf.setInt(start, inBuf.readableBytes());
    channel.writeInbound(inBuf);

    Message out = channel.readInbound();
    assertEquals(proto, out.proto());
    assertEquals(optIdx, out.msgId());
    assertArrayEquals(helloWorld, out.packet());
    assertEquals(0, inBuf.readableBytes());

    channel.finishAndReleaseAll();
  }



}
