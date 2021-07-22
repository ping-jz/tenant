package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/**
 * String序列化实现,UTF_8编码
 *
 * 长度|内容
 *
 * 长度:varint和ZigZag编码
 * 内容:bytes
 *
 * 与{@link CommonSerializer} 组合使用
 *
 * @since 2021年07月17日 16:16:14
 **/
public class StringSerializer implements Serializer<String> {

  @Override
  public String readObject(ByteBuf buf) {
    int length = NettyByteBufUtil.readInt32(buf);
    String str = buf.toString(buf.readerIndex(), length, StandardCharsets.UTF_8);
    buf.skipBytes(length);
    return str;
  }

  @Override
  public void writeObject(ByteBuf buf, String object) {
    byte[] bytes = object.getBytes(StandardCharsets.UTF_8);
    NettyByteBufUtil.writeInt32(buf, bytes.length);
    buf.writeBytes(bytes);
  }
}
