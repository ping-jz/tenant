package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用对象序列化实现
 *
 * <p>1.根据字段名字进行排序，不能随机更改字段名</p>
 * <p>2.字段类型也需要注册进{@link CommonSerializer}, 顺序无关</p>
 * <p>3.因为接口和抽象类的存在，无法确定具体类型，所以不提供自动注册</p>
 * <p>
 * 与{@link CommonSerializer} 组合使用,本体功能并不完整
 *
 * @since 2021年07月17日 16:16:14
 **/
public class ObjectSerializer implements Serializer<Object> {

  public static final FieldInfo[] EMPTY_FILE_INFO = new FieldInfo[0];
  /**
   * 目标类型
   */
  private Class<?> clazz;
  /**
   * 序列实现集合
   */
  private CommonSerializer serializer;
  /**
   * 默认无参构造
   */
  private Constructor<?> constructor;
  /**
   * 字段信息
   */
  private FieldInfo[] fields;

  public ObjectSerializer(Class<?> clazz, CommonSerializer serializer) {
    this.clazz = clazz;
    this.serializer = serializer;
    register(clazz);
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
        clazz.getModifiers()) || clazz == Object.class) {
      throw new RuntimeException("类型:" + clazz + ",无法序列化");
    }

    try {
      clazz.getDeclaredConstructor();
    } catch (Exception e) {
      throw new RuntimeException("类型:" + clazz + ",缺少无参构造方法");
    }
  }

  /**
   * 注册所有字段信息(排除静态，final和transient字段)
   *
   * @since 2021年07月18日 11:09:50
   */
  public void register(Class<?> clazz) {
    try {
      constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);
    } catch (Exception e) {
      throw new RuntimeException("类型:" + clazz + ",缺少无参构造方法");
    }

    //所有字段(Getter, Setter)
    List<FieldInfo> fields = new ArrayList<>();
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    for (Class<?> cls = clazz; cls != Object.class; cls = cls.getSuperclass()) {
      for (Field f : cls.getDeclaredFields()) {
        int modifier = f.getModifiers();
        if (Modifier.isStatic(modifier) || Modifier.isFinal(modifier) || Modifier.isTransient(
            modifier)) {
          continue;
        }

        if (fields == null) {
          fields = new ArrayList<>();
        }
        f.setAccessible(true);
        try {
          FieldInfo fieldInfo = new FieldInfo(lookup.unreflectSetter(f), lookup.unreflectGetter(f),
              f.getName());
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
    Object o;
    try {
      o = constructor.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("类型:" + clazz + ",创建失败", e);
    }

    for (FieldInfo field : fields) {
      try {
        Object value = serializer.readObject(buf);
        MethodHandle setter = field.getSetter();
        setter.invoke(o, value);
      } catch (Throwable e) {
        throw new RuntimeException(String.format("反序列化:%s, 字段:%s 错误", clazz, field.getName()), e);
      }
    }
    return o;
  }

  @Override
  public void writeObject(ByteBuf buf, Object object) {
    for (FieldInfo field : fields) {
      try {
        Object value = field.getGetter().invoke(object);
        serializer.writeObject(buf, value);
      } catch (Throwable e) {
        throw new RuntimeException(String.format("序列化:%s, 字段:%s 错误", clazz, field.getName()), e);
      }
    }
  }

  private static class FieldInfo {

    private MethodHandle setter;
    private MethodHandle getter;
    private String name;

    FieldInfo(MethodHandle setter, MethodHandle getter, String name) {
      this.setter = setter;
      this.getter = getter;
      this.name = name;
    }

    public MethodHandle getSetter() {
      return setter;
    }

    public MethodHandle getGetter() {
      return getter;
    }

    public String getName() {
      return name;
    }
  }
}
