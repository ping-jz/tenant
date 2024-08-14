package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.IntFunction;
import org.example.serde.CommonSerializer.SerializerPair;


/**
 * 通用集合序列化，默认实现为{@link ArrayList}
 *
 * //TODO 参考下Kyro，团队处理更加出彩啊！！！！！！！！！！！！！！
 *
 * <pre>
 *   一维数组:
 *
 *    元素数量|唯一类型ID(非0需处理)|元素1|元素2|元素3|元素3|
 *
 *    元素数量:1-5字节, 使用varint32和ZigZga编码
 *    元素:实现决定
 * </pre>
 * <p>
 * 与{@link CommonSerializer} 组合使用
 *
 * @since 2021年07月18日 14:17:04
 **/
public class CollectionSerializer implements Serializer<Object> {

  /**
   * 序列化入口
   */
  private CommonSerializer serializer;
  /**
   * 集合提供者
   */
  private IntFunction<Collection<Object>> factory;

  public CollectionSerializer(CommonSerializer serializer) {
    this(serializer, ArrayList::new);
  }

  /**
   *
   * @param factory 根据长度创建一个集合
   * @since 2024/8/8 22:36
   */
  public CollectionSerializer(CommonSerializer serializer,
      IntFunction<Collection<Object>> factory) {
    this.serializer = serializer;
    this.factory = factory;
  }

  @Override
  public Object readObject(ByteBuf buf) {
    int length = NettyByteBufUtil.readInt32(buf);
    if (length < 0) {
      return null;
    }

    int typeId = NettyByteBufUtil.readInt32(buf);


    Collection<Object> collection = factory.apply(length);
    Serializer<Object> ser = serializer;
    if (typeId != 0) {
      Class<?> clz = serializer.getClazz(typeId);
      if (clz != null) {
        SerializerPair pair = serializer.getSerializerPair(clz);
        if (pair != null) {
          ser = (Serializer<Object>) pair.serializer();
        }
      }
    }

    for (int i = 0; i < length; i++) {
      collection.add(ser.readObject(buf));
    }
    return collection;
  }

  @Override
  public void writeObject(ByteBuf buf, Object object) {
    if (object == null) {
      NettyByteBufUtil.writeInt32(buf, -1);
    } else {
      if (!(object instanceof Collection)) {
        throw new RuntimeException("类型:" + object.getClass() + ",不是集合");
      }
      @SuppressWarnings("unchecked") Collection<Object> collection = (Collection<Object>) object;

      NettyByteBufUtil.writeInt32(buf, collection.size());
      NettyByteBufUtil.writeInt32(buf, 0);

      Serializer<Object> ser = serializer;
      for (Object o : collection) {
        ser.writeObject(buf, o);
      }
    }
  }




}
