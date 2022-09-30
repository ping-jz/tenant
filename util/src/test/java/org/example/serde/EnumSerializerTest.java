package org.example.serde;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EnumSerializerTest {

  @Test
  public void simpleTest() {
    EnumSerializer<EnumOne> serializer = new EnumSerializer<>(EnumOne.class);
    ByteBuf byteBuf = Unpooled.buffer();

    for (EnumOne o : EnumOne.values()) {
      serializer.writeObject(byteBuf, o);
    }

    for (EnumOne o : EnumOne.values()) {
      Assertions.assertEquals(o, serializer.readObject(byteBuf));
    }

    serializer.writeObject(byteBuf, null);
    Assertions.assertNull(serializer.readObject(byteBuf));
  }

  @Test
  public void errorTest() {
    @SuppressWarnings("all") EnumSerializer<Object> serializer = new EnumSerializer(EnumOne.class);
    ByteBuf byteBuf = Unpooled.buffer();

    Assertions.assertThrows(IllegalArgumentException.class,
        () -> serializer.writeObject(byteBuf, EnumTwo.One));
  }

  @Test
  public void intergateTest() {
    CommonSerializer serializer = new CommonSerializer();
    serializer.registerSerializer(10, EnumOne.class, new EnumSerializer<>(EnumOne.class));
    serializer.registerSerializer(11, EnumTwo.class, new EnumSerializer<>(EnumTwo.class));
    serializer.registerObject(12, TestCaseOne.class);
    serializer.registerSerializer(13, List.class, new CollectionSerializer(serializer));
    ByteBuf buf = Unpooled.buffer();

    serializer.writeObject(buf, EnumOne.values());
    Assertions.assertArrayEquals(EnumOne.values(), serializer.read(buf));
    Assertions.assertFalse(buf.isReadable());

    {
      TestCaseOne caseOne = new TestCaseOne();
      caseOne.name = "caseOne";
      caseOne.two = EnumTwo.One;
      caseOne.values = EnumOne.values();

      serializer.writeObject(buf, caseOne);
      Assertions.assertEquals(caseOne, serializer.read(buf));
      Assertions.assertFalse(buf.isReadable());
    }

    {
      TestCaseOne caseOne = new TestCaseOne();
      caseOne.name = "casetwo";
      caseOne.two = EnumTwo.One;
      caseOne.list = Arrays.asList(EnumOne.values());

      serializer.writeObject(buf, caseOne);
      Assertions.assertEquals(caseOne, serializer.read(buf));
      Assertions.assertFalse(buf.isReadable());
    }
  }

  public enum EnumOne {
    One(), Two(), Three(), Four(), Five(),
  }

  public enum EnumTwo {
    One()
  }

  public static class TestCaseOne {

    public String name;
    public EnumTwo nullNum;
    public EnumTwo two;
    public EnumOne[] values;
    public List<EnumOne> list;


    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestCaseOne one = (TestCaseOne) o;
      return Objects.equals(name, one.name) && nullNum == one.nullNum && two == one.two
          && Arrays.equals(values, one.values) && Objects.equals(list, one.list);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(name, nullNum, two, list);
      result = 31 * result + Arrays.hashCode(values);
      return result;
    }
  }

}
