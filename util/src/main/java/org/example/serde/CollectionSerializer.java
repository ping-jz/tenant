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
 *
 * 与{@link CommonSerializer} 组合使用
 *
 * @since 2021年07月18日 14:17:04
 **/
public class CollectionSerializer implements Serializer<Object> {

  /** 序列化入口 */
  private CommonSerializer serializer;
  /** 集合提供者 */
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
    Collection<Object> collection = factory.get();

    for (int i = 0; i < length; i++) {
      collection.add(serializer.readObject(buf));
    }
    return collection;
  }

  @Override
  public void writeObject(ByteBuf buf, Object object) {
    if (!(object instanceof Collection)) {
      throw new RuntimeException("类型:" + object.getClass() + ",不是集合");
    }
    @SuppressWarnings("unchecked cast")
    Collection<Object> collection = (Collection<Object>) object;
    int length = collection.size();
    NettyByteBufUtil.writeInt32(buf, length);
    for (Object o : collection) {
      serializer.writeObject(buf, o);
    }
  }
}
