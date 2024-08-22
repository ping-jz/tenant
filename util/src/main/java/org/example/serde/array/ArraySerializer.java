package org.example.serde.array;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import org.example.serde.CommonSerializer;
import org.example.serde.CommonSerializer.SerializerPair;
import org.example.serde.NettyByteBufUtil;
import org.example.serde.Serializer;

/**
 * JAVA数组序列化 //TODO 参考下Kyro，团队处理更加出彩啊！！！！！！！！！！！！！！
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
 *    长度=N|元素1|......|元素N
 *

 *    长度:1-5字节, 使用varint32和ZigZag编码
 *    元素:实现决定
 * </pre>
 * <p>1.数组长宽必须一致</p>
 * <p>2.暂时不支持PrimitiveWrapper数组，序列化时会全部转化为对应的基础类型</p>
 * <p>
 * 与{@link CommonSerializer} 组合使用
 *
 * @since 2021年07月18日 14:17:04
 **/
public class ArraySerializer implements Serializer<Object> {

  /**
   * 序列化集合
   */
  private CommonSerializer serializer;
  /** 数据类型 */
  private final Class componentType;

  public ArraySerializer(Class componentType, CommonSerializer serializer) {
    this.componentType = componentType;
    this.serializer = serializer;
  }


  @Override
  public Object readObject(ByteBuf buf) {
    int length = NettyByteBufUtil.readInt32(buf);
    if (length == -1) {
      return null;
    }

    Object array = Array.newInstance(componentType, length);
    Serializer<Object> serializer = this.serializer;
    if (Modifier.isFinal(componentType.getModifiers())) {
      SerializerPair pair = this.serializer.getSerializerPair(componentType);
      if (pair == null) {
        throw new RuntimeException("类型:" + componentType + ",未注册");
      }
      serializer = (Serializer<Object>) pair.serializer();
    }

    for (int i = 0; i < length; ++i) {
      Array.set(array, i, serializer.readObject(buf));
    }

    return array;
  }


  @Override
  public void writeObject(ByteBuf buf, Object object) {
    if (!object.getClass().isArray()) {
      throw new RuntimeException("类型:" + object.getClass() + ",不是数组");
    }
    final int length = Array.getLength(object);
    NettyByteBufUtil.writeInt32(buf, length);

    Serializer<Object> serializer = this.serializer;
    if (Modifier.isFinal(componentType.getModifiers())) {
      SerializerPair pair = this.serializer.getSerializerPair(componentType);
      if (pair == null) {
        throw new RuntimeException("类型:" + componentType + ",未注册");
      }

      serializer = (Serializer<Object>) pair.serializer();
    }

    for (int i = 0; i < length; i++) {
      serializer.writeObject(buf, Array.get(object, i));
    }
  }
}
