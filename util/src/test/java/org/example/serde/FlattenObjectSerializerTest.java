package org.example.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FlattenObjectSerializerTest {

  private CommonSerializer serializer;
  private ByteBuf buf;

  @BeforeEach
  public void before() {
    serializer = new CommonSerializer();
    buf = Unpooled.buffer();
  }

  @Test
  public void checkTest() {
    //primitive check
    assertThrows(RuntimeException.class, () -> FlattenObjectSerializer.checkClass(Integer.TYPE));

    //abstract class check
    assertThrows(RuntimeException.class, () -> FlattenObjectSerializer.checkClass(List.class));

    //annotation class check
    assertThrows(RuntimeException.class, () -> FlattenObjectSerializer.checkClass(Override.class));

    //annotation class check
    assertThrows(RuntimeException.class, () -> FlattenObjectSerializer.checkClass(Integer[].class));

    //abstract class check
    assertThrows(RuntimeException.class,
        () -> FlattenObjectSerializer.checkClass(AbstractList.class));
  }

  @Test
  public void primitiveTest() {
    PrimitiveObj obj = new PrimitiveObj();
    obj.d = Double.MIN_VALUE;

    serializer.registerFlattenObject(10, PrimitiveObj.class);
    serializer.writeObject(buf, obj);

    Object[] res = serializer.read(buf);

    assertEquals(res[0], obj.d);
    assertEquals(res[1], obj.b);
    assertEquals(res[2], obj.s);
    assertEquals(res[3], obj.i);
    assertEquals(res[4], obj.l);
    assertEquals(res[5], obj.f);
    assertEquals(res[6], obj.c);
    assertEquals(res.length, 7);
  }

  @Test
  public void wrapperTest() {
    WrapperObj obj = new WrapperObj();
    obj.d = Double.MIN_VALUE;
    obj.str = "Hello World!";
    obj.b = 123;

    serializer.registerFlattenObject(10, WrapperObj.class);
    serializer.writeObject(buf, obj);

    Object[] res = serializer.read(buf);
    assertEquals(res[0], obj.d);
    assertEquals(res[1], obj.str);
    assertEquals(res[2], obj.b);
    assertEquals(res[3], obj.s);
    assertEquals(res[4], obj.i);
    assertEquals(res[5], obj.l);
    assertEquals(res[6], obj.f);
    assertEquals(res[7], obj.c);
    assertEquals(res.length, 8);
  }

  @Test
  public void composeTest() {
    serializer.registerObject(10, PrimitiveObj.class);
    serializer.registerObject(11, WrapperObj.class);
    serializer.registerFlattenObject(12, ComposeObj.class);
    serializer.registerRecord(13, AAA.class);

    PrimitiveObj pri = new PrimitiveObj();
    pri.l = Long.MIN_VALUE;

    WrapperObj wrap = new WrapperObj();
    wrap.str = "Hello World!";
    wrap.l = ThreadLocalRandom.current().nextLong();

    ComposeObj composeObj = new ComposeObj();
    composeObj.pri = pri;
    composeObj.wrap = wrap;
    composeObj.aaa = new AAA(1, 2, 3);

    serializer.writeObject(buf, composeObj);

    Object[] res = serializer.read(buf);
    assertEquals(res[0], composeObj.pri);
    assertEquals(res[1], composeObj.wrap);
    assertEquals(res[2], composeObj.aaa);
    assertEquals(res.length, 3);
  }

  @Test
  public void inheritanceTest() {
    serializer.registerObject(11, PrimitiveObj.class);
    serializer.registerObject(12, WrapperObj.class);
    serializer.registerFlattenObject(13, Child.class);

    PrimitiveObj pri = new PrimitiveObj();
    pri.l = Long.MIN_VALUE;

    WrapperObj wrap = new WrapperObj();
    wrap.str = "Hello World!";
    wrap.l = ThreadLocalRandom.current().nextLong();

    Child child = new Child();
    child.pri = pri;
    child.wrap = wrap;
    child.a = ThreadLocalRandom.current().nextInt();
    child.A = ThreadLocalRandom.current().nextInt();
    child.ignore = ThreadLocalRandom.current().nextInt() + 1;

    serializer.writeObject(buf, child);

    Object[] res = serializer.read(buf);
    assertEquals(res[0], child.A);
    assertEquals(res[1], child.a);
    assertEquals(res[2], child.pri);
    assertEquals(res[3], child.wrap);
    assertEquals(res[4], null);
    assertEquals(res.length, 5);
  }

  /**
   * 基础类型测试类
   *
   * @author ZJP
   * @since 2021年07月18日 13:06:39
   **/
  private static class PrimitiveObj {

    public double d = Double.MAX_VALUE;
    private byte b = Byte.MAX_VALUE;
    private short s = Short.MAX_VALUE;
    private int i = Integer.MAX_VALUE;
    private long l = Long.MAX_VALUE;
    private float f = Float.MAX_VALUE;
    private char c = '中';

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PrimitiveObj that = (PrimitiveObj) o;
      return b == that.b && s == that.s && i == that.i && l == that.l
          && Float.compare(that.f, f) == 0 && Double.compare(that.d, d) == 0
          && c == that.c;
    }

    @Override
    public int hashCode() {
      return Objects.hash(b, s, i, l, f, d, c);
    }
  }


  /**
   * 包装类型测试类
   *
   * @author ZJP
   * @since 2021年07月18日 13:06:39
   **/
  private static class WrapperObj {

    public Double d = Double.MAX_VALUE;
    public String str = "hi";
    private Byte b = Byte.MAX_VALUE;
    private Short s = Short.MAX_VALUE;
    private Integer i = Integer.MAX_VALUE;
    private Long l = Long.MAX_VALUE;
    private Float f = Float.MAX_VALUE;
    private Character c = '中';

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      WrapperObj that = (WrapperObj) o;
      return Objects.equals(d, that.d) && Objects.equals(str, that.str)
          && Objects.equals(b, that.b) && Objects.equals(s, that.s)
          && Objects.equals(i, that.i) && Objects.equals(l, that.l)
          && Objects.equals(f, that.f) && Objects.equals(c, that.c);
    }

    @Override
    public int hashCode() {
      return Objects.hash(d, str, b, s, i, l, f, c);
    }
  }

  /**
   * 组合测试
   *
   * @author ZJP
   * @since 2021年07月18日 13:45:35
   **/
  private static class ComposeObj {

    public PrimitiveObj pri;
    public WrapperObj wrap;
    public AAA aaa;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ComposeObj that = (ComposeObj) o;
      return Objects.equals(pri, that.pri) && Objects.equals(wrap, that.wrap)
          && Objects.equals(aaa, that.aaa);
    }

    @Override
    public int hashCode() {
      return Objects.hash(pri, wrap, aaa);
    }
  }

  /**
   * 继承测试
   *
   * @author ZJP
   * @since 2021年07月18日 13:45:35
   **/
  private final static class Child extends ComposeObj {

    public int A;
    public Integer a;
    public transient int ignore;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      Child child = (Child) o;
      return A == child.A &&
          Objects.equals(a, child.a);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), A, a);
    }
  }

  record AAA(int a, int b, int c) {

  }


}
