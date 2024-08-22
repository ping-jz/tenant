package org.example.serde.array;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Array;
import java.util.Objects;
import org.example.serde.CommonSerializer;
import org.example.serde.NettyByteBufUtil;
import org.example.serde.Serializer;

public class StringArraySerializer implements Serializer<String[]> {

  /**
   * 序列化集合
   */
  private CommonSerializer serializer;

  public StringArraySerializer(CommonSerializer serializer) {

    this.serializer = serializer;
  }


  @Override
  public String[] readObject(ByteBuf buf) {
    int length = NettyByteBufUtil.readInt32(buf);
    if (length == -1) {
      return null;
    }

    String[] array = new String[length];
    Serializer<String> serializer = Objects.requireNonNull(
        this.serializer.getSerializer(String.class), "类型: java.lang.String, 未注册");
    for (int i = 0; i < length; ++i) {
      array[i] = serializer.readObject(buf);
    }

    return array;
  }


  @Override
  public void writeObject(ByteBuf buf, String[] object) {
    if (!object.getClass().isArray()) {
      throw new RuntimeException("类型:" + object.getClass() + ",不是数组");
    }

    final int length = Array.getLength(object);
    NettyByteBufUtil.writeInt32(buf, length);

    Serializer<Object> serializer = Objects.requireNonNull(
        this.serializer.getSerializer(String.class), "类型: java.lang.String, 未注册");
    for (String s : object) {
      serializer.writeObject(buf, s);
    }
  }
}
