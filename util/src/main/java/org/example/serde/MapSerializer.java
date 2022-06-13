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
  private Supplier<Map<K, V>> supplier;

  public MapSerializer(CommonSerializer serializer) {
    this(serializer, HashMap::new);
  }

  public MapSerializer(CommonSerializer serializer, Supplier<Map<K, V>> mapSupplier) {
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
      Class<?> clazz = serializer.getClazz(keyTypeId);
      if (clazz == null) {
        throw new NullPointerException("类型ID:" + keyTypeId + "，未注册");
      }

      keySer = serializer.getSerializer(clazz);
      if (keySer == null) {
        throw new NullPointerException("类型ID:" + keyTypeId + "，未注册");
      }
    }

    if (valueTypeId != 0) {
      Class<?> clazz = serializer.getClazz(valueTypeId);
      if (clazz == null) {
        throw new NullPointerException("类型ID:" + valueTypeId + "，未注册");
      }

      valSer = serializer.getSerializer(clazz);
      if (valSer == null) {
        throw new NullPointerException("类型ID:" + valueTypeId + "，未注册");
      }
    }

    Map<K, V> map = supplier.get();
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
    Class<?> uniqueKeyType = uniqueType(object.keySet());
    Class<?> uniqueValueType = uniqueType(object.values());

    int keyTypeId = 0;
    int valueTypeId = 0;
    Serializer<Object> keySer = serializer;
    Serializer<Object> valSer = serializer;

    if (uniqueKeyType != null) {
      keySer = serializer.findSerilaizer(uniqueKeyType);
      if (keySer == null) {
        throw new RuntimeException("类型:" + uniqueKeyType + "，未注册");
      }
      keyTypeId = serializer.getTypeId(uniqueKeyType);
    }

    if (uniqueValueType != null) {
      valSer = serializer.findSerilaizer(uniqueValueType);
      if (valSer == null) {
        throw new RuntimeException("类型:" + uniqueValueType + "，未注册");
      }
      valueTypeId = serializer.getTypeId(uniqueValueType);
    }

    NettyByteBufUtil.writeInt32(buf, keyTypeId);
    NettyByteBufUtil.writeInt32(buf, valueTypeId);

    for (Entry<K, V> e : object.entrySet()) {
      Object key = e.getKey();
      Object val = e.getValue();
      keySer.writeObject(buf, key);
      valSer.writeObject(buf, val);
    }
  }


  /**
   * 如果集合里面都是统一类型，则返回唯一的类型,否则Null
   */
  public Class<?> uniqueType(Iterable<?> collection) {
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
