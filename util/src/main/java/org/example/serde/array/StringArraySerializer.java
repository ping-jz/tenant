package org.example.serde.array;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import org.example.serde.CommonSerializer;
import org.example.serde.NettyByteBufUtil;
import org.example.serde.Serializer;

public class StringArraySerializer implements Serializer<String[]> {

  public StringArraySerializer() {

  }


  @Override
  public String[] readObject(CommonSerializer serializer, ByteBuf buf) {
    int length = NettyByteBufUtil.readInt32(buf);
    if (length < 0) {
      return null;
    }

    String[] array = new String[length];
    for (int i = 0; i < length; ++i) {
      int strLength = NettyByteBufUtil.readInt32(buf);
      if (0 <= strLength) {
        array[i] = buf.readCharSequence(strLength, StandardCharsets.UTF_8).toString();
      }
    }

    return array;
  }


  @Override
  public void writeObject(CommonSerializer serializer, ByteBuf buf, String[] object) {
    if (object == null) {
      NettyByteBufUtil.writeInt32(buf, Integer.MIN_VALUE);
      return;
    }

    final int length = Array.getLength(object);
    NettyByteBufUtil.writeInt32(buf, length);

    for (String s : object) {
      if (s == null) {
        NettyByteBufUtil.writeInt32(buf, Integer.MIN_VALUE);
      } else {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        NettyByteBufUtil.writeInt32(buf, bytes.length);
        buf.writeBytes(bytes);
      }
    }
  }
}
