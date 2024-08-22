package org.example.serde.array;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Array;
import org.example.serde.CommonSerializer;
import org.example.serde.CommonSerializer.SerializerPair;
import org.example.serde.NettyByteBufUtil;
import org.example.serde.Serializer;

/**
 * JAVA数组序列化
 *
 *
 * <pre>
 *   一维数组:
 *
 *    元素类型|长度|元素1|元素2|
 *
 *
 *  二维数组(都压缩成一维数组)
 *
 *    长度=N|类型ID|元素1|......|元素N
 *
 *    维度总数:1-5字节, 使用varint32和ZigZag编码
 *    维度1长:1-5字节, 使用varint32和ZigZag编码
 *    类型ID:1-5字节, 使用varint32和ZigZag编码
 *    元素:实现决定
 * </pre>
 * <p>2.暂时不支持PrimitiveWrapper数组，序列化时会全部转化为对应的基础类型</p>
 * <p>
 * 与{@link CommonSerializer} 组合使用
 *
 * @since 2021年07月18日 14:17:04
 **/
public class ShortArraySerializer implements Serializer<short[]> {

  /**
   * 序列化集合
   */
  private CommonSerializer serializer;

  public ShortArraySerializer(CommonSerializer serializer) {
    this.serializer = serializer;
  }


  @Override
  public short[] readObject(ByteBuf buf) {
    int length = NettyByteBufUtil.readInt32(buf);
    if (length == -1) {
      return null;
    } else {
      final int typeId = NettyByteBufUtil.readInt32(buf);
      Class<?> componentType = serializer.getClazz(typeId);
      if (componentType == null) {
        throw new RuntimeException("类型ID:" + typeId + ",未注册");
      }
      short[] array = new short[length];
      for (int i = 0; i < length; i++) {
        array[i] = buf.readShort();
      }
      return array;
    }
  }


  @Override
  public void writeObject(ByteBuf buf, short[] object) {
    SerializerPair pair = serializer.getSerializerPair(Short.TYPE);
    if (pair == null) {
      throw new RuntimeException("类型:" + Short.TYPE + ",未注册");
    }
    final int length = Array.getLength(object);
    NettyByteBufUtil.writeInt32(buf, length);
    NettyByteBufUtil.writeInt32(buf, pair.typeId());
    for (short o : object) {
      buf.writeShort(o);
    }
  }
}