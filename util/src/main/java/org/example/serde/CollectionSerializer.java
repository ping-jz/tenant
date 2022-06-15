package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;


/**
 * 通用集合序列化，默认实现为{@link ArrayList}
 *
 * <pre>
 *   一维数组:
 *
 *    元素数量|元素1|元素2|元素3|元素3|
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
  private Supplier<Collection<Object>> factory;

  public CollectionSerializer(CommonSerializer serializer) {
    this(serializer, ArrayList::new);
  }

  public CollectionSerializer(CommonSerializer serializer, Supplier<Collection<Object>> factory) {
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
    Collection<Object> collection = factory.get();
    Serializer<Object> ser = serializer;
    if (typeId != 0) {
      Class<?> clazz = serializer.getClazz(typeId);
      if (clazz == null) {
        throw new NullPointerException("类型ID:" + typeId + "，未注册");
      }

      ser = serializer.getSerializer(clazz);
      if (ser == null) {
        throw new NullPointerException("类型ID:" + typeId + "，未注册");
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
      @SuppressWarnings("unchecked cast") Collection<Object> collection = (Collection<Object>) object;

      NettyByteBufUtil.writeInt32(buf, collection.size());
      Class<?> type = typeId(collection);
      Serializer<Object> ser = serializer;
      int typeId = 0;
      if (type != null) {
        ser = serializer.findSerilaizer(type);
        if (ser == null) {
          throw new RuntimeException("类型:" + type + "，未注册");
        }
        typeId = serializer.getTypeId(type);
      }

      NettyByteBufUtil.writeInt32(buf, typeId);
      for (Object o : collection) {
        ser.writeObject(buf, o);
      }
    }
  }

  /**
   * 如果集合里面都是统一类型，则返回唯一的类型ID。否则0
   */
  public Class<?> typeId(Collection<Object> collection) {
    Class<?> type = null;
    for (Object o : collection) {
      if (o == null) {
        continue;
      }

      Class<?> temp = o.getClass();
      if (type == null) {
        type = temp;
      } else if (type != temp) {
        return null;
      }
    }

    return type;
  }
}
