package org.example.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * 集合序列化测试
 *
 * @author ZJP
 * @since 2021年07月18日 14:17:04
 **/
public class SetSerializerTest {

  /** 业务入口 */
  private CommonSerializer serializer;
  /** 临时buff */
  private ByteBuf buf;

  @BeforeEach
  void beforeEach() {
    serializer = new CommonSerializer();
    buf = Unpooled.buffer();

    CollectionSerializer collectSer = new CollectionSerializer(serializer, HashSet::new);

    serializer.registerSerializer(10, Set.class, collectSer);
    serializer.linkTo(HashSet.class, Set.class);
  }

  @Test
  void intCollectionTest() {
    Random random = ThreadLocalRandom.current();
    Set<Integer> col = new HashSet<>();
    int size = random.nextInt(Short.MAX_VALUE);
    for (int i = 0; i < size; i++) {
      col.add(random.nextInt());
    }

    serializer.writeObject(buf, col);
    Set<Integer> res = serializer.read(buf);
    assertEquals(col, res);
  }

  @Test
  void doubleCollectionTest() {
    Random random = ThreadLocalRandom.current();
    Set<Double> col = new HashSet<>();
    int size = random.nextInt(Short.MAX_VALUE);
    for (int i = 0; i < size; i++) {
      col.add(random.nextDouble());
    }

    serializer.writeObject(buf, col);
    Set<Double> res = serializer.read(buf);
    assertEquals(col, res);
  }

  @Test
  void strCollectionTest() {
    Random random = ThreadLocalRandom.current();
    Set<String> col = new HashSet<>();
    int size = random.nextInt(Short.MAX_VALUE);
    for (int i = 0; i < size; i++) {
      col.add(Integer.toString(random.nextInt()));
    }

    serializer.writeObject(buf, col);
    Set<String> res = serializer.read(buf);
    assertEquals(col, res);
  }

  @Test
  void colCollectionTest() {
    Random random = ThreadLocalRandom.current();
    Set<Set<String>> col = new HashSet<>();
    int size = Byte.MAX_VALUE;
    for (int i = 0; i < size; i++) {
      int subSize = random.nextInt(size);
      Set<String> strs = new HashSet<>(subSize);
      for (int j = 0; j < subSize; j++) {
        strs.add(Integer.toString(random.nextInt()));
      }
      col.add(strs);
    }

    serializer.writeObject(buf, col);
    Set<Set<String>> res = serializer.read(buf);
    assertEquals(col, res);
  }


}
