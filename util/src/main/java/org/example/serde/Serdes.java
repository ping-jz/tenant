package org.example.serde;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.example.serde.array.ArraySerializer;

/**
 * 序列化组合实现,业务主要入口
 * <p>
 *
 * @since 2021年07月17日 16:30:05
 **/
public final class Serdes {

  /**
   * [类型ID, 具体类型]
   */
  private Int2ObjectOpenHashMap<Class<?>> id2Clazz;
  /**
   * 序列化注册 [目标类型 -> 序列化实现]
   */
  private Map<Class<?>, SerializerPair> serializers;

  public record SerializerPair(int typeId, Serializer<?> serializer) {

  }

  public Serdes() {
    serializers = new HashMap<>();
    id2Clazz = new Int2ObjectOpenHashMap<>();
  }


  /**
   * 注册序列化
   *
   * @param id         类型ID
   * @param clazz      类型
   * @param serializer 序列化实现
   * @since 2021年07月18日 11:37:14
   */
  public void registerSerializer(int id, Class<?> clazz, Serializer<?> serializer) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(serializer);

    if (id2Clazz.containsKey(id)) {
      throw new RuntimeException(String.format("%s,%s 类型ID发生冲突", id2Clazz.get(id), clazz));
    }

    id2Clazz.put(id, clazz);
    serializers.put(clazz, new SerializerPair(id, serializer));
  }

  /**
   * 根据类型获取类型ID
   *
   * @param cls 类型
   * @since 2021年07月18日 16:18:08
   */
  public SerializerPair getSerializerPair(Class<?> cls) {
    return serializers.get(cls);
  }

  /**
   * 根据类型ID获取类型
   *
   * @param typeId 类型ID
   * @since 2021年07月18日 16:18:08
   */
  public Class<?> getClazz(int typeId) {
    return id2Clazz.get(typeId);
  }

  /**
   * 注册普通序列化
   *
   * @param clazz 类型
   * @since 2021年07月18日 11:37:14
   */
  public void registerObject(Class<?> clazz) {
    registerObject(clazz.getName().hashCode(), clazz);
  }

  /**
   * 注册对象序列化
   *
   * @param id    类型ID
   * @param clazz 类型
   * @since 2021年07月18日 11:37:14
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void registerObject(Integer id, Class clazz) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(clazz);

    if (clazz.isArray()) {
      registerSerializer(id, clazz, new ArraySerializer(clazz.getComponentType()));
    } else if (clazz.isRecord()) {
      Serializer<?> serializer = new RecordSerializer(clazz);
      registerSerializer(id, clazz, serializer);
    } else if (clazz.isEnum()) {
      registerSerializer(id, clazz, new EnumSerializer<>(clazz));
    } else {
      ObjectSerializer.checkClass(clazz);
      Serializer<?> serializer = new ObjectSerializer(clazz);
      registerSerializer(id, clazz, serializer);
    }
  }


  /**
   * 注册序列化
   *
   * @param clazz      类型
   * @param serializer 序列化实现
   * @since 2021年07月18日 11:37:14
   */
  public void registerSerializer(Class<?> clazz, Serializer<?> serializer) {
    registerSerializer(clazz.getName().hashCode(), clazz, serializer);
  }

  @SuppressWarnings("unchecked")
  public <V> V readObject(ByteBuf buf) {
    return (V) read(buf);
  }

  private Object read(ByteBuf buf) {
    int readerIndex = buf.readerIndex();
    try {
      int typeId = NettyByteBufUtil.readInt32(buf);
      Class<?> clazz = getClazz(typeId);
      if (clazz == null) {
        throw new NullPointerException("类型ID:" + typeId + "，未注册");
      }
      SerializerPair pair = getSerializerPair(clazz);
      if (pair == null) {
        throw new NullPointerException("类型ID:" + typeId + "，未注册");
      }
      return pair.serializer.readObject(this, buf);
    } catch (Exception e) {
      buf.readerIndex(readerIndex);
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public void writeObject(ByteBuf buf, Object object) {
    Class<?> clazz = object == null ? NullSerializer.class : object.getClass();
    SerializerPair pair = getSerializerPair(clazz);

    int writeIdx = buf.writerIndex();
    try {
      if (pair == null) {
        throw new RuntimeException("类型:" + clazz + "，未注册");
      }

      Serializer<Object> serializer = (Serializer<Object>) pair.serializer;
      NettyByteBufUtil.writeInt32(buf, pair.typeId);
      serializer.writeObject(this, buf, object);
    } catch (Exception e) {
      buf.writerIndex(writeIdx);
      throw new RuntimeException("类型:" + clazz + ",序列化错误", e);
    }
  }
}
