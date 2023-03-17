package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 对象扁平化序列化实现 Object { A a B b C c } 转换为[a, b, c]
 *
 * <p>2.字段类型也需要注册进{@link CommonSerializer}, 顺序无关</p>
 * <p>3.因为接口和抽象类的存在，无法确定具体类型，所以不提供自动注册</p>
 * <p>
 * 与{@link CommonSerializer} 组合使用,本体功能并不完整
 *
 * @since 2021年07月17日 16:16:14
 **/
public class FlattenObjectSerializer implements Serializer<Object> {

  public static final FieldInfo[] EMPTY_FILE_INFO = new FieldInfo[0];
  /** 目标类型 */
  private Class<?> clazz;
  /** 序列实现集合 */
  private CommonSerializer serializer;
  /** 字段信息 */
  private FieldInfo[] fields;

  public FlattenObjectSerializer(Class<?> clazz, CommonSerializer serializer) {
    this.clazz = clazz;
    this.serializer = serializer;
    register(serializer, clazz);
  }

  /**
   * 接口，抽象类，标记，无法进行注册
   * <p>
   * 必须提供无参构造方法
   *
   * @param clazz 注册类型
   * @since 2021年07月18日 11:02:39
   */
  public static void checkClass(Class<?> clazz) {
    if (clazz.isInterface() || clazz.isAnnotation() || clazz.isPrimitive() || Modifier.isAbstract(
        clazz.getModifiers()) || clazz.isArray() || clazz == Object.class) {
      throw new RuntimeException("类型:" + clazz + ",无法序列化");
    }
  }

  /**
   * 注册所有字段信息(排除静态，final和transient字段)
   *
   * @since 2021年07月18日 11:09:50
   */
  public void register(CommonSerializer serializer, Class<?> clazz) {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    //所有字段(Getter)
    List<FieldInfo> fields = new ArrayList<>();

    for (Class<?> cls = clazz; cls != Object.class; cls = cls.getSuperclass()) {
      for (Field f : cls.getDeclaredFields()) {
        int modifier = f.getModifiers();
        if (Modifier.isStatic(modifier) || Modifier.isFinal(modifier) || Modifier.isTransient(
            modifier)) {
          continue;
        }

        f.setAccessible(true);
        try {
          FieldInfo fieldInfo = new FieldInfo(serializer.getSerializer(f.getType()),
              lookup.unreflectGetter(f), f.getName());
          fields.add(fieldInfo);
        } catch (Exception e) {
          throw new RuntimeException(
              String.format("类型:%s.%s, 创建getter和setter失败", clazz.getName(), f.getName()));
        }
      }
    }

    this.fields = fields.toArray(EMPTY_FILE_INFO);
  }

  @Override
  public Object readObject(ByteBuf buf) {
    Object[] o = new Object[fields.length];
    for (int i = 0; i < fields.length; i++) {
      FieldInfo field = fields[i];
      try {
        Serializer<Object> serializer =
            field.serializer != null ? field.serializer : this.serializer;
        Object value = serializer.readObject(buf);
        o[i] = value;
      } catch (Throwable e) {
        throw new RuntimeException(String.format("反序列化:%s, 字段:%s 错误", clazz, field.name()),
            e);
      }
    }
    return o;
  }

  @Override
  public void writeObject(ByteBuf buf, Object object) {
    for (FieldInfo field : fields) {
      try {
        Object value = field.getter().invoke(object);
        Serializer<Object> serializer =
            field.serializer != null ? field.serializer : this.serializer;
        serializer.writeObject(buf, value);
      } catch (Throwable e) {
        throw new RuntimeException(String.format("序列化:%s, 字段:%s 错误", clazz, field.name()),
            e);
      }
    }
  }

  record FieldInfo(Serializer<Object> serializer, MethodHandle getter, String name) {

  }
}
