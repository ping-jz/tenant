package org.example.net.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.example.net.Message;
import org.example.serde.CommonSerializer;
import org.junit.jupiter.api.Test;

public class MessageCodecTest {

  @Test
  public void outBoundhelloWorldTest() {
    CommonSerializer serializer = new CommonSerializer();
    EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec(Integer.BYTES, serializer));

    Message message = new Message();
    message.proto(1);
    message.optIdx(2);
    message.packet("Hello World");
    channel.writeOutbound(message);

    ByteBuf out = channel.readOutbound();

    int length = out.readInt();
    assertTrue(0 < length);

    assertEquals(message.proto(), out.readInt());
    assertEquals(message.optIdx(), out.readInt());
    assertEquals(message.packet(), serializer.read(out));
    assertEquals(0, out.readableBytes());

    channel.finishAndReleaseAll();
  }

  @Test
  public void inBoundhelloWorldTest() {
    CommonSerializer serializer = new CommonSerializer();
    EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec(Integer.BYTES, serializer));

    int proto = 1;
    int optIdx = 2;
    String helloWorld = "Hello World";

    ByteBuf inBuf = Unpooled.buffer();
    inBuf.writerIndex(Integer.BYTES);
    inBuf.writeInt(proto);
    inBuf.writeInt(optIdx);
    serializer.writeObject(inBuf, helloWorld);

    inBuf.setInt(0, inBuf.readableBytes() - Integer.BYTES);
    channel.writeInbound(inBuf);

    Message out = channel.readInbound();
    assertEquals(proto, out.proto());
    assertEquals(optIdx, out.optIdx());
    assertEquals(helloWorld, out.packet());
    assertEquals(0, inBuf.readableBytes());

    channel.finishAndReleaseAll();
  }

}
