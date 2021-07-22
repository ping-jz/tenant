package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

/**
 * Map集合序列化，默认实现为{@link HashMap},反序列化不保持顺序
 *
 * <pre>
 *   一维数组:
 *
 *    元素数量|KEY1|VALUE1|KEY2|VALUE2|
 *
 *    元素数量:1-5字节, 使用varint32和ZigZga编码
 *    元素:实现决定
 * </pre>
 *
 * 与{@link CommonSerializer} 组合使用
 *
 * @since 2021年07月18日 14:17:04
 **/
public class MapSerializer implements Serializer<Map<Object, Object>> {

  /** 序列化实现集合 */
  private CommonSerializer serializer;
  /** Map工厂 */
  private Supplier<Map<Object, Object>> supplier;

  public MapSerializer(CommonSerializer serializer) {
    this(serializer, HashMap::new);
  }

  public MapSerializer(CommonSerializer serializer, Supplier<Map<Object, Object>> mapSupplier) {
    this.serializer = serializer;
    this.supplier = mapSupplier;
  }

  @Override
  public Map<Object, Object> readObject(ByteBuf buf) {
    int length = NettyByteBufUtil.readInt32(buf);
    Map<Object, Object> map = supplier.get();
    for (int i = 0; i < length; i++) {
      Object key = serializer.readObject(buf);
      Object val = serializer.readObject(buf);
      map.put(key, val);
    }
    return map;
  }

  @Override
  public void writeObject(ByteBuf buf, Map<Object, Object> object) {
    int length = object.size();
    NettyByteBufUtil.writeInt32(buf, length);

    for (Entry<Object, Object> e : object.entrySet()) {
      Object key = e.getKey();
      Object val = e.getValue();
      serializer.writeObject(buf, key);
      serializer.writeObject(buf, val);
    }
  }
}
