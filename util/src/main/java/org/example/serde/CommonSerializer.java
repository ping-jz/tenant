package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化组合实现,业务主要入口
 *
 * <pre>
 * 基本类型ID(部分语言应该有类似的概念):
 * null:0
 * Byte:1
 * Short:2
 * Integer:3
 * Long:4
 * Float:5
 * Double:6
 * Character:7
 * String:8
 * Array:9
 * </pre>
 * <p>集合就按需注册吧，不写死了</p>
 * 此类提供整合功能和类型ID实现，内容的序列化和反序列化交给Serializer的子类去实现
 *
 * @since 2021年07月17日 16:30:05
 **/
public class CommonSerializer implements Serializer<Object> {

  /**
   * 通用类型ID
   */
  public static final int NULL_ID = 0;
  public static final int BYTE_ID = 1;
  public static final int SHORT_ID = 2;
  public static final int INTEGER_ID = 3;
  public static final int LONG_ID = 4;
  public static final int FLOAT_ID = 5;
  public static final int DOUBLE_ID = 6;
  public static final int CHARACTER_ID = 7;
  public static final int STRING_ID = 8;
  public static final int ARRAY_ID = 9;

  /**
   * [类型ID, 具体类型]
   */
  private Map<Integer, Class<?>> id2Clazz;
  /**
   * 序列化注册 [目标类型 -> 序列化实现]
   */
  private Map<Class<?>, Serializer<?>> serializers;
  /**
   * [具体类型, 类型ID]
   */
  private Map<Class<?>, Integer> clazz2Id;

  public CommonSerializer() {
    serializers = new ConcurrentHashMap<>();
    id2Clazz = new ConcurrentHashMap<>();
    clazz2Id = new ConcurrentHashMap<>();
    commonType();
    registerWrapperType();

  }

  /**
   * 根据类型获取类型ID
   *
   * @param cls 类型
   * @since 2021年07月18日 16:18:08
   */
  public Integer getTypeId(Class<?> cls) {
    return clazz2Id.getOrDefault(cls, 0);
  }

  /**
   * 根据类型ID获取类型
   *
   * @param typeId 类型ID
   * @since 2021年07月18日 16:18:08
   */
  public Class<?> getClazz(Integer typeId) {
    return id2Clazz.get(typeId);
  }

  /**
   * 根据类型获取序列化实现
   *
   * @param cls 类型
   * @since 2021年07月18日 16:18:08
   */
  public <T> Serializer<T> getSerializer(Class<?> cls) {
    return (Serializer<T>) serializers.get(cls);
  }

  /**
   * 注册包装类型
   * <p>
   * 如:把包装类导向基础类(包装类指向基础类类型的ID和序列化实现)
   * <p>Integer -> int</p>
   * <p>Double -> double</p>
   *
   * @since 2021年07月18日 10:31:21
   */
  private void registerWrapperType() {
    linkTo(Byte.class, Byte.TYPE);
    linkTo(Short.class, Short.TYPE);
    linkTo(Integer.class, Integer.TYPE);
    linkTo(Long.class, Long.TYPE);
    linkTo(Float.class, Float.TYPE);
    linkTo(Double.class, Double.TYPE);
    linkTo(Character.class, Character.TYPE);
  }

  /**
   * 注册九种基础类型,九种类型应该在大部分都会有相似的概念
   *
   * @since 2021年07月19日 23:00:35
   */
  private void commonType() {
    registerSerializer(NULL_ID, NullSerializer.class, NullSerializer.INSTANCE);
    registerSerializer(BYTE_ID, Byte.TYPE, new ByteSerializer());
    registerSerializer(SHORT_ID, Short.TYPE, new ShortSerializer());
    registerSerializer(INTEGER_ID, Integer.TYPE, new IntegerSerializer());
    registerSerializer(LONG_ID, Long.TYPE, new LongSerializer());
    registerSerializer(FLOAT_ID, Float.TYPE, new FloatSerializer());
    registerSerializer(DOUBLE_ID, Double.TYPE, new DoubleSerializer());
    registerSerializer(CHARACTER_ID, Character.TYPE, new CharacterSerializer());
    registerSerializer(STRING_ID, String.class, new StringSerializer());
    registerSerializer(ARRAY_ID, ArraySerializer.class, new ArraySerializer(this));
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
   * 注册普通序列化
   *
   * @param id    类型ID
   * @param clazz 类型
   * @since 2021年07月18日 11:37:14
   */
  public void registerObject(Integer id, Class<?> clazz) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(clazz);
    ObjectSerializer.checkClass(clazz);

    Serializer<?> serializer = new ObjectSerializer(clazz, this);
    registerSerializer(id, clazz, serializer);
  }

  /**
   * 注册Record序列化
   *
   * @param id    类型ID
   * @param clazz 类型
   * @since 2021年07月18日 11:37:14
   */
  public void registerRecord(Integer id, Class<?> clazz) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(clazz);
    RecordSerializer.checkClass(clazz);

    Serializer<?> serializer = new RecordSerializer(clazz, this);
    registerSerializer(id, clazz, serializer);
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
  public void registerSerializer(Integer id, Class<?> clazz, Serializer<?> serializer) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(serializer);

    serializers.put(clazz, serializer);
    Object old;
    if ((old = id2Clazz.put(id, clazz)) != null) {
      throw new RuntimeException(String.format("%s,%s 类型ID发生冲突", old, clazz));
    }
    clazz2Id.put(clazz, id);
  }

  /**
   * 类型绑定
   * <p>
   * 如:把包装类导向基础类(包装类指向基础类的ID和序列化实现)
   * <p>Integer -> int</p>
   * <p>Double -> double</p>
   * <p>ArrayList -> Collection</p>
   * <p>List -> Collection</p>
   * <p>
   * 注意:序列化之后，原有信息会被抛弃。如何LinkHashMap会变成HashMap。如果想保留，请使用{@link CommonSerializer#registerObject}
   *
   * @since 2021年07月18日 10:31:21
   */
  public void linkTo(Class<?> type, Class<?> targetType) {
    Serializer<?> serializer = serializers.get(targetType);
    Objects.requireNonNull(serializer);

    Integer targetTypeId = clazz2Id.get(targetType);
    Objects.requireNonNull(targetTypeId);

    serializers.put(type, serializer);
    clazz2Id.put(type, targetTypeId);
  }


  /**
   * readObject 包装方法
   *
   * @param buf 目标buff
   * @since 2021年07月17日 16:36:14
   */
  @SuppressWarnings("unchecked")
  public <T> T read(ByteBuf buf) {
    return (T) readObject(buf);
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
      Serializer<?> serializer = serializers.get(clazz);
      if (serializer == null) {
        throw new NullPointerException("类型ID:" + typeId + "，未注册");
      }
      return serializer.readObject(buf);
    } catch (Exception e) {
      buf.readerIndex(readerIndex);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void writeObject(ByteBuf buf, Object object) {
    Class<?> clazz = object == null ? NullSerializer.class : object.getClass();
    if (clazz.isArray()) {
      clazz = id2Clazz.get(ARRAY_ID);
    }

    int writeIdx = buf.writerIndex();
    try {
      Serializer<Object> serializer = findSerilaizer(clazz);
      if (serializer == null) {
        throw new RuntimeException("类型:" + clazz + "，未注册");
      }

      Integer typeId = clazz2Id.get(clazz);
      if (typeId == null) {
        throw new RuntimeException("类型:" + clazz + "，没有类型ID");
      }

      NettyByteBufUtil.writeInt32(buf, typeId);
      serializer.writeObject(buf, object);
    } catch (Exception e) {
      buf.writerIndex(writeIdx);
      throw new RuntimeException("类型:" + clazz + ",序列化错误", e);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> Serializer<T> findSerilaizer(Class<?> clzz) {
    Serializer<T> serializer = (Serializer<T>) serializers.get(clzz);
    //没有找到，尝试寻找父类(父类必须是接口，接口更加通用)
    if (serializer == null) {
      for (Class<?> key : serializers.keySet()) {
        if (key.isAssignableFrom(clzz) && key.isInterface()) {
          linkTo(clzz, key);
          break;
        }
      }

      serializer = (Serializer<T>) serializers.get(clzz);
    }

    return serializer;
  }
}
