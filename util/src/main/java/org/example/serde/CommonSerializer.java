package org.example.serde;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.example.serde.array.ArraySerializer;
import org.example.serde.array.BooleanArraySerializer;
import org.example.serde.array.ByteArraySerializer;
import org.example.serde.array.CharArraySerializer;
import org.example.serde.array.DoubleArraySerializer;
import org.example.serde.array.FloatArraySerializer;
import org.example.serde.array.IntArraySerializer;
import org.example.serde.array.LongArraySerializer;
import org.example.serde.array.ShortArraySerializer;
import org.example.serde.array.StringArraySerializer;

/**
 * 序列化组合实现,业务主要入口
 * <p>
 * TODO 删除linkto, 基础类和包装类各自注册
 * TODO 删除findSerilaizer, 使用饱和式注册替代，这样更加清晰
 * TODO 各种神奇的集合注册
 * TODO 全部类型都要显示注册
 *
 * @since 2021年07月17日 16:30:05
 **/
public class CommonSerializer implements Serializer<Object> {

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

  public CommonSerializer() {
    serializers = new ConcurrentHashMap<>();
    id2Clazz = new Int2ObjectOpenHashMap<>();
    commonType();
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
   * 根据类型获取序列化实现
   *
   * @param cls 类型
   * @since 2021年07月18日 16:18:08
   */
  public <T> Serializer<T> getSerializer(Class<?> cls) {
    return (Serializer<T>) getSerializerPair(cls).serializer();
  }

  /**
   * 注册九种基础类型,九种类型应该在大部分都会有相似的概念
   *
   * @since 2021年07月19日 23:00:35
   */
  private void commonType() {
    registerSerializer(Integer.MIN_VALUE, NullSerializer.class, NullSerializer.INSTANCE);
    {
      ByteSerializer serializer = new ByteSerializer();
      registerSerializer(1, Byte.TYPE, serializer);
      registerSerializer(11, Byte.class, serializer);
    }

    {
      BooleanSerializer serializer = new BooleanSerializer();
      registerSerializer(2, Boolean.TYPE, serializer);
      registerSerializer(12, Boolean.class, serializer);
    }

    {
      ShortSerializer serializer = new ShortSerializer();
      registerSerializer(3, Short.TYPE, serializer);
      registerSerializer(13, Short.class, serializer);
    }

    {
      IntegerSerializer serializer = new IntegerSerializer();
      registerSerializer(4, Integer.TYPE, serializer);
      registerSerializer(14, Integer.class, serializer);
    }

    {
      LongSerializer serializer = new LongSerializer();
      registerSerializer(5, Long.TYPE, serializer);
      registerSerializer(15, Long.class, serializer);
    }

    {
      FloatSerializer serializer = new FloatSerializer();
      registerSerializer(6, Float.TYPE, serializer);
      registerSerializer(16, Float.class, serializer);
    }

    {
      DoubleSerializer serializer = new DoubleSerializer();
      registerSerializer(7, Double.TYPE, serializer);
      registerSerializer(17, Double.class, serializer);
    }

    {
      CharacterSerializer serializer = new CharacterSerializer();
      registerSerializer(9, Character.TYPE, serializer);
      registerSerializer(19, Character.class, serializer);
    }

    registerSerializer(21, String.class, new StringSerializer());
    registerSerializer(22, byte[].class, new ByteArraySerializer());
    registerSerializer(23, boolean[].class, new BooleanArraySerializer());
    registerSerializer(24, short[].class, new ShortArraySerializer());
    registerSerializer(25, char[].class, new CharArraySerializer());
    registerSerializer(26, float[].class, new FloatArraySerializer());
    registerSerializer(27, double[].class, new DoubleArraySerializer());
    registerSerializer(28, int[].class, new IntArraySerializer());
    registerSerializer(29, long[].class, new LongArraySerializer());
    registerSerializer(30, String[].class, new StringArraySerializer());
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
  @SuppressWarnings({"unchecked","rawtypes"})
  public void registerObject(Integer id, Class clazz) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(clazz);

    if (clazz.isArray()) {
      registerSerializer(id, clazz, new ArraySerializer(clazz.getComponentType(), this));
    } else if (clazz.isRecord()) {
      Serializer<?> serializer = new RecordSerializer(clazz, this);
      registerSerializer(id, clazz, serializer);
    } else if (clazz.isEnum()) {
      registerSerializer(id, clazz, new EnumSerializer<>(clazz));
    } else {
      ObjectSerializer.checkClass(clazz);
      Serializer<?> serializer = new ObjectSerializer(clazz, this);
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

    Object old;
    if ((old = id2Clazz.put(id, clazz)) != null) {
      throw new RuntimeException(String.format("%s,%s 类型ID发生冲突", old, clazz));
    }
    serializers.put(clazz, new SerializerPair(id, serializer));
  }


  @Override
  public Object readObject(ByteBuf buf) {
    int readerIndex = buf.readerIndex();
    try {
      int typeId = NettyByteBufUtil.readInt32(buf);
      Class<?> clazz = id2Clazz.get(typeId);
      if (clazz == null) {
        throw new NullPointerException("类型ID:" + typeId + "，未注册");
      }
      SerializerPair pair = serializers.get(clazz);
      if (pair == null) {
        throw new NullPointerException("类型ID:" + typeId + "，未注册");
      }
      return pair.serializer.readObject(buf);
    } catch (Exception e) {
      buf.readerIndex(readerIndex);
      throw new RuntimeException(e);
    }
  }


  public ByteBuf writeObject(Object object) {
    ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer();
    ;
    try {
      writeObject(buf, object);
    } catch (Exception e) {
      ReferenceCountUtil.release(buf);
      throw new RuntimeException(e);
    }
    return buf;
  }

  @Override
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
      serializer.writeObject(buf, object);
    } catch (Exception e) {
      buf.writerIndex(writeIdx);
      throw new RuntimeException("类型:" + clazz + ",序列化错误", e);
    }
  }
}
