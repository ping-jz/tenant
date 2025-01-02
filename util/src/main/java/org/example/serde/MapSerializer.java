package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.IntFunction;
import org.example.serde.CommonSerializer.SerializerPair;

/**
 * Map集合序列化，默认实现为{@link HashMap},反序列化不保持顺序
 *
 * <pre>
 *   一维数组:
 *
 *    元素数量|唯一key类型ID(非0需处理)|唯一val类型ID(非0需处理)|KEY1|VALUE1|KEY2|VALUE2|
 *
 *    元素数量:1-5字节, 使用varint32和ZigZga编码
 *    元素:实现决定
 * </pre>
 * <p>
 * 与{@link CommonSerializer} 组合使用
 *
 * @since 2021年07月18日 14:17:04
 **/
public class MapSerializer<K, V> implements Serializer<Map<K, V>> {

  /**
   * Map工厂
   */
  private IntFunction<Map<K, V>> supplier;

  public MapSerializer() {
    this(HashMap::new);
  }

  public MapSerializer(IntFunction<Map<K, V>> mapSupplier) {
    this.supplier = mapSupplier;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<K, V> readObject(CommonSerializer serializer, ByteBuf buf) {
    int length = NettyByteBufUtil.readInt32(buf);
    if (length < 0) {
      return null;
    }

    int keyTypeId = NettyByteBufUtil.readInt32(buf);
    int valueTypeId = NettyByteBufUtil.readInt32(buf);
    if (keyTypeId != 0 && valueTypeId != 0) {
      Serializer<Object> keySer = null;
      Serializer<Object> valSer = null;

      Class<?> kClz = Objects.requireNonNull(serializer.getClazz(keyTypeId),
          () -> "Map解析，未注册的Key类型ID:%s".formatted(keyTypeId));
      SerializerPair kPair = Objects.requireNonNull(serializer.getSerializerPair(kClz),
          () -> "Map解析，未注册的Key类型:%s".formatted(kClz));
      keySer = (Serializer<Object>) kPair.serializer();

      Class<?> vClz = Objects.requireNonNull(serializer.getClazz(valueTypeId),
          () -> "Map解析，未注册的Value类型ID:%s".formatted(valueTypeId));
      SerializerPair vPair = Objects.requireNonNull(serializer.getSerializerPair(vClz),
          () -> "Map解析，未注册的Value类型:%s".formatted(vClz));
      valSer = (Serializer<Object>) vPair.serializer();

      Map<K, V> map = supplier.apply(length);
      for (int i = 0; i < length; i++) {
        K key = (K) keySer.readObject(serializer, buf);
        V val = (V) valSer.readObject(serializer, buf);
        map.put(key, val);
      }
      return map;
    } else {
      Map<K, V> map = supplier.apply(length);
      for (int i = 0; i < length; i++) {
        K key = serializer.readObject(buf);
        V val = serializer.readObject(buf);
        map.put(key, val);
      }
      return map;
    }
  }

  @Override
  public void writeObject(CommonSerializer serializer, ByteBuf buf, Map<K, V> object) {
    if (object == null) {
      NettyByteBufUtil.writeInt32(buf, -1);
      return;
    }

    int length = object.size();
    NettyByteBufUtil.writeInt32(buf, length);
    NettyByteBufUtil.writeInt32(buf, 0);
    NettyByteBufUtil.writeInt32(buf, 0);

    for (Entry<K, V> e : object.entrySet()) {
      Object key = e.getKey();
      Object val = e.getValue();
      serializer.writeObject(buf, key);
      serializer.writeObject(buf, val);
    }
  }

}
