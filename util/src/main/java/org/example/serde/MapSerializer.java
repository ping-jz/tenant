package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
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
   * 序列化实现集合
   */
  private CommonSerializer serializer;
  /**
   * Map工厂
   */
  private IntFunction<Map<K, V>> supplier;

  public MapSerializer(CommonSerializer serializer) {
    this(serializer, HashMap::new);
  }

  public MapSerializer(CommonSerializer serializer, IntFunction<Map<K, V>> mapSupplier) {
    this.serializer = serializer;
    this.supplier = mapSupplier;
  }

  @Override
  public Map<K, V> readObject(ByteBuf buf) {
    int length = NettyByteBufUtil.readInt32(buf);
    if (length < 0) {
      return null;
    }

    int keyTypeId = NettyByteBufUtil.readInt32(buf);
    int valueTypeId = NettyByteBufUtil.readInt32(buf);
    Serializer<Object> keySer = serializer;
    Serializer<Object> valSer = serializer;

    if (keyTypeId != 0) {
      Class<?> clz = serializer.getClazz(keyTypeId);
      if (clz != null) {
        SerializerPair pair = serializer.getSerializerPair(clz);
        if (pair != null) {
          keySer = (Serializer<Object>) pair.serializer();
        }
      }
    }

    if (valueTypeId != 0) {
      Class<?> clz = serializer.getClazz(valueTypeId);
      if (clz != null) {
        SerializerPair pair = serializer.getSerializerPair(clz);
        if (pair != null) {
          valSer = (Serializer<Object>) pair.serializer();
        }
      }
    }


    Map<K, V> map = supplier.apply(length);
    for (int i = 0; i < length; i++) {
      K key = (K) keySer.readObject(buf);
      V val = (V) valSer.readObject(buf);
      map.put(key, val);
    }
    return map;
  }

  @Override
  public void writeObject(ByteBuf buf, Map<K, V> object) {
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
