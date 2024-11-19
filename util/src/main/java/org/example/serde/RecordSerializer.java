package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;


public class RecordSerializer implements Serializer<Object> {

  /**
   * 目标类型
   */
  private Class<?> clazz;
  /**
   * 默认无参构造
   */
  private MethodHandle constructor;

  private CommonSerializer serializer;
  /**
   * 字段信息
   */
  private FieldInfo[] fields;

  public RecordSerializer(Class<?> clazz, CommonSerializer serializer) {
    this.clazz = clazz;
    this.serializer = serializer;
    register(serializer, clazz);
  }

  @Override
  public Object readObject(ByteBuf buf) {
    Object[] args = null;
    if (0 < fields.length) {
      args = new Object[fields.length];
      for (int i = 0; i < fields.length; i++) {
        FieldInfo field = fields[i];
        try {
          Serializer<Object> ser = field.serializer != null ? field.serializer : serializer;
          Object value = ser.readObject(buf);
          args[i] = value;
        } catch (Throwable e) {
          throw new RuntimeException(
              String.format("反序列化:%s, 字段:%s 错误", clazz, field.name()), e);
        }
      }
    }
    Object o;
    try {
      if (args != null) {
        o = constructor.invokeWithArguments(args);
      } else {
        o = constructor.invoke();
      }
    } catch (Throwable e) {
      throw new RuntimeException("类型:" + clazz + ",创建失败", e);
    }
    return o;
  }

  @Override
  public void writeObject(ByteBuf buf, Object object) {
    for (FieldInfo field : fields) {
      try {
        Object value = field.getter().invoke(object);
        Serializer<Object> ser = field.serializer != null ? field.serializer : serializer;
        ser.writeObject(buf, value);
      } catch (Throwable e) {
        throw new RuntimeException(String.format("序列化:%s, 字段:%s 错误", clazz, field.name()),
            e);
      }
    }
  }

  public static void checkClass(Class<?> clazz) {
    if (!clazz.isRecord() || clazz.isInterface() || clazz.isAnnotation() || clazz.isPrimitive()
        || Modifier.isAbstract(clazz.getModifiers()) || clazz == Object.class) {
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

    RecordComponent[] components = clazz.getRecordComponents();
    Class<?>[] types = new Class[components.length];
    FieldInfo[] fieldInfos = new FieldInfo[components.length];
    for (int i = 0; i < components.length; i++) {
      RecordComponent component = components[i];
      types[i] = component.getType();

      try {
        Serializer<Object> ser = component.getType() == Object.class ?
            serializer :
            serializer.getSerializer(component.getType());
        fieldInfos[i] = new FieldInfo(ser,
            lookup.unreflect(component.getAccessor()), component.getName());
      } catch (Exception e) {
        throw new RuntimeException(
            String.format("类型:%s, 字段：%s %s, 创建getter和setter失败", clazz.getName(),
                component.getType(), component.getName()), e);
      }
    }
    try {
      Constructor<?> temp = clazz.getDeclaredConstructor(types);
      temp.setAccessible(true);
      constructor = lookup.unreflectConstructor(temp);
    } catch (Exception e) {
      throw new RuntimeException("类型:" + clazz + ",缺少无参构造方法");
    }
    this.fields = fieldInfos;
  }


  record FieldInfo(Serializer<Object> serializer, MethodHandle getter, String name) {

  }
}
