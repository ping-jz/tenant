package org.example.net.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

import org.example.net.Message;
import org.example.serde.CommonSerializer;
import org.example.serde.NettyByteBufUtil;
import org.junit.jupiter.api.Test;

public class MessageCodecTest {

  @Test
  public void outBoundhelloWorldTest() {
    CommonSerializer serializer = new CommonSerializer();
    EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec(serializer));

    Message message = new Message();
    message.target(123);
    message.source(321);
    message.proto(1);
    message.msgId(2);
    message.packet("Hello World");
    channel.writeOutbound(message);

    ByteBuf out = channel.readOutbound();

    int length = out.readInt();
    assertTrue(0 < length);

    assertEquals(message.target(), NettyByteBufUtil.readInt32(out));
    assertEquals(message.source(), NettyByteBufUtil.readInt32(out));
    assertEquals(message.proto(), NettyByteBufUtil.readInt32(out));
    assertEquals(message.msgId(), NettyByteBufUtil.readInt32(out));
    assertEquals(message.packet(), serializer.read(out));
    assertEquals(0, out.readableBytes());

    channel.finishAndReleaseAll();
  }

  @Test
  public void inBoundhelloWorldTest() {
    CommonSerializer serializer = new CommonSerializer();
    EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec(serializer));

    int source = 123;
    int target = 321;
    int proto = 1;
    int optIdx = 2;
    String helloWorld = "Hello World";

    ByteBuf inBuf = Unpooled.buffer();
    int start = inBuf.writerIndex();
    inBuf.writerIndex(Integer.BYTES);
    NettyByteBufUtil.writeInt32(inBuf, target);
    NettyByteBufUtil.writeInt32(inBuf, source);
    NettyByteBufUtil.writeInt32(inBuf, proto);
    NettyByteBufUtil.writeInt32(inBuf, optIdx);
    serializer.writeObject(inBuf, helloWorld);

    inBuf.setInt(start, inBuf.readableBytes());
    channel.writeInbound(inBuf);

    Message out = channel.readInbound();
    assertEquals(target, out.target());
    assertEquals(source, out.source());
    assertEquals(proto, out.proto());
    assertEquals(optIdx, out.msgId());
    assertEquals(helloWorld, out.packet());
    assertEquals(0, inBuf.readableBytes());

    channel.finishAndReleaseAll();
  }



}
