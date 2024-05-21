package org.example.serde;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArraySerializerTest {

  private CommonSerializer serializer;
  private ByteBuf buf;

  @BeforeEach
  public void beforeEach() {
    serializer = new CommonSerializer();
    serializer.registerSerializer( Object.class, new MarkSerializer());
    serializer.registerObject(ArrayWrapper.class);
    buf = Unpooled.buffer();
  }

  @Test
  public void oneDimensionsCheckTest() {
    int[] array = {};
    assertArrayEquals(new int[]{0}, ArraySerializer.getDimensions(array));
    int[] arrayTwo = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    assertArrayEquals(new int[]{10}, ArraySerializer.getDimensions(arrayTwo));
  }

  @Test
  public void fourDimensionsCheckTest() {
    int[][][][] array = new int[3][2][4][6];
    assertArrayEquals(new int[]{3, 2, 4, 6}, ArraySerializer.getDimensions(array));
  }

  @Test
  public void diffLengthDimensionCheckTest() {
    int[][][][] array = new int[2][2][4][6];
    array[0] = new int[10][11][12];
    assertThrows(RuntimeException.class, () -> ArraySerializer.getDimensions(array));
  }

  @Test
  public void emptyArrayTest() {
    int[] test = {};
    serializer.writeObject(buf, test);

    int[] res = serializer.read(buf);
    assertArrayEquals(test, res);
  }

  @Test
  public void simpleArrayTest() {
    int[] test = new int[1000];
    for(int i = 0; i < test.length; i++) {
      test[i] = ThreadLocalRandom.current().nextInt();
    }
    serializer.writeObject(buf, test);

    int[] res = serializer.read(buf);
    assertArrayEquals(test, res);
  }

  @Test
  public void twoDimensionIntArrayTest() {
    int[][] test = {{1, 10}, {10, 1}};
    serializer.writeObject(buf, test);
    int[][] res = serializer.read(buf);
    assertArrayEquals(test, res);
  }

  @Test
  public void threeDimensionDoubleArrayTest() {
    Random random = ThreadLocalRandom.current();
    double[][][] test = new double[2][3][4];
    for (int o = 0; o < 2; o++) {
      for (int t = 0; t < 3; t++) {
        for (int th = 0; th < 4; th++) {
          test[o][t][th] = random.nextDouble();
        }
      }
    }
    serializer.writeObject(buf, test);
    double[][][] res = serializer.read(buf);
    assertArrayEquals(test, res);
  }

  @Test
  public void fourDimensionStringArrayTest() {
    Random random = ThreadLocalRandom.current();
    String[][][][] test = new String[1][2][3][4];
    for (int f = 0; f < 1; f++) {
      for (int o = 0; o < 2; o++) {
        for (int t = 0; t < 3; t++) {
          //故意不设值
          if (t == 1) {
            continue;
          }
          for (int th = 0; th < 4; th++) {
            test[f][o][t][th] = "a" + random.nextInt();
          }
        }
      }
    }

    serializer.writeObject(buf, test);
    String[][][][] res = serializer.read(buf);
    assertArrayEquals(test, res);
  }

  @Test
  public void fourDimensionObjectArrayTest() {
    Random random = ThreadLocalRandom.current();
    int one = 5, two = 2, three = 3, four = 4;
    ArrayWrapper[][][][] test = new ArrayWrapper[one][two][three][four];
    int special = 2;
    for (int f = 0; f < one; f++) {
      for (int o = 0; o < two; o++) {
        for (int t = 0; t < three; t++) {
          for (int th = 0; th < four; th++) {
            ArrayWrapper wrap = new ArrayWrapper();
            if (th != special) {
              wrap.str = "a" + random.nextInt();
            } else {
              int length = random.nextInt(100);
              wrap.test = new int[length];
              for (int i = 0; i < length; i++) {
                wrap.test[i] = random.nextInt();
              }
            }
            test[f][o][t][th] = wrap;
          }
        }
      }
    }

    serializer.writeObject(buf, test);
    ArrayWrapper[][][][] res = serializer.read(buf);
    assertArrayEquals(test, res);
  }

  @Test
  public void objectArraySerializerTest() {
    Object[] objects = {1, 2L, "asdfasdf", new ArrayWrapper(), null, 'a'};

    serializer.writeObject(buf, objects);
    Object[] res = serializer.read(buf);
    assertArrayEquals(objects, res);
  }

  private static class ArrayWrapper {

    private String str;
    private int[] test;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ArrayWrapper that = (ArrayWrapper) o;
      return Objects.equals(str, that.str) &&
          Arrays.equals(test, that.test);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(str);
      result = 31 * result + Arrays.hashCode(test);
      return result;
    }

    @Override
    public String toString() {
      return "StringWrapper{" +
          "str='" + str + '\'' +
          '}';
    }
  }

}
