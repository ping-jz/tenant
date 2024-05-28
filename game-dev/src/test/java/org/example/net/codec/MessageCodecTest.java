package org.example.net.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.example.net.Message;
import org.example.net.codec.msg.CodecObject;
import org.example.net.codec.msg.CodecObjectSerde;
import org.example.serde.CollectionSerializer;
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

  @Test
  public void codecObjectTest() {

    ThreadLocalRandom random = ThreadLocalRandom.current();
    CodecObject object = new CodecObject();
    object.setMsg(List.of(String.valueOf(random.nextLong()), String.valueOf(random.nextLong()), String.valueOf(random.nextLong())));
    object.setId(random.nextInt());
    object.setAge(random.nextLong());
    object.setDatas(new long[]{random.nextLong(), random.nextLong(), random.nextLong()});
    object.setSigned(true);
    object.setType(random.nextInt());
    byte[] bytes = new byte[10];
    random.nextBytes(bytes);
    object.setBitArray(bytes);


    CommonSerializer commonSerializer = new CommonSerializer();
    commonSerializer.registerSerializer(CodecObject.class, new CodecObjectSerde(commonSerializer));
    commonSerializer.registerSerializer(List.class, new CollectionSerializer(commonSerializer));

    ByteBuf buf2 = commonSerializer.writeObject(object);
    CodecObject object2 = commonSerializer.read(buf2);


    assertEquals(object, object2);
    assertEquals(object.hashCode(), object2.hashCode());

  }

}
