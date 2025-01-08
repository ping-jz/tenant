package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/**
 * String序列化实现,UTF_8编码
 * <p>
 * 长度|内容
 * <p>
 * 长度:varint和ZigZag编码 内容:bytes
 * <p>
 * 与{@link Serdes} 组合使用
 *
 * @since 2021年07月17日 16:16:14
 **/
public class StringSerializer implements Serializer<String> {

  @Override
  public String readObject(Serdes serializer, ByteBuf buf) {
    int length = NettyByteBufUtil.readInt32(buf);
    if (length < 0) {
      return null;
    }
    return buf.readCharSequence(length, StandardCharsets.UTF_8).toString();
  }

  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, String object) {
    if (object == null) {
      NettyByteBufUtil.writeInt32(buf, -1);
    } else {
      byte[] bytes = object.getBytes(StandardCharsets.UTF_8);
      NettyByteBufUtil.writeInt32(buf, bytes.length);
      buf.writeBytes(bytes);
    }
  }
}
