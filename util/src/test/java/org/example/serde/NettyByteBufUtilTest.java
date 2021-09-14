package org.example.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

/**
 * 编码，反编码工具类测试
 *
 * @author ZJP
 * @since 2021年07月17日 10:01:08
 **/
public class NettyByteBufUtilTest {

  @Test
  public void varaint64Test() {
    ByteBuf buf = Unpooled.buffer();
    for (long i = -10000000L; i <= 10000000L; i++) {
      buf.clear();
      NettyByteBufUtil.writeRawVaraint64(buf, i);
      assertEquals(i, NettyByteBufUtil.readRawVarint64(buf));
    }
  }

  @Test
  public void varaint64SlowTest() {
    ByteBuf buf = Unpooled.buffer();
    for (long i = -10000000L; i <= 10000000L; i++) {
      buf.clear();
      NettyByteBufUtil.writeRawVaraint64(buf, i);
      assertEquals(i, NettyByteBufUtil.readRawVarint64SlowPath(buf));
    }
  }

  @Test
  public void zigZag64Test() {
    for (long i = -10000000L; i <= 10000000L; i++) {
      long encoded = NettyByteBufUtil.encodeZigZag64(i);
      assertEquals(i, NettyByteBufUtil.decodeZigZag64(encoded));
    }
  }

  @Test
  public void int64Test() {
    ByteBuf buf = Unpooled.buffer();
    for (long i = -10000000L; i <= 10000000L; i++) {
      buf.clear();
      NettyByteBufUtil.writeInt64(buf, i);
      assertEquals(i, NettyByteBufUtil.readInt64(buf));
    }

    buf.clear();
    NettyByteBufUtil.writeInt64(buf, Long.MIN_VALUE);
    assertEquals(Long.MIN_VALUE, NettyByteBufUtil.readInt64(buf));

    buf.clear();
    NettyByteBufUtil.writeInt64(buf, Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, NettyByteBufUtil.readInt64(buf));
  }

  @Test
  public void zigZag32Test() {
    for (int i = -10000000; i <= 10000000; i++) {
      int encoded = NettyByteBufUtil.encodeZigZag32(i);
      assertEquals(i, NettyByteBufUtil.decodeZigZag32(encoded));
    }
  }

  @Test
  public void varaint32Test() {
    ByteBuf buf = Unpooled.buffer();
    for (int i = -10000000; i <= 10000000; i++) {
      buf.clear();
      NettyByteBufUtil.writeRawVarint32(buf, i);
      assertEquals(i, NettyByteBufUtil.readRawVarint32(buf));
    }
  }

  @Test
  public void int32Test() {
    ByteBuf buf = Unpooled.buffer();
    for (int i = -10000000; i <= 10000000; i++) {
      buf.clear();
      NettyByteBufUtil.writeInt32(buf, i);
      assertEquals(i, NettyByteBufUtil.readInt32(buf));
    }

    buf.clear();
    NettyByteBufUtil.writeInt32(buf, Integer.MIN_VALUE);
    assertEquals(Integer.MIN_VALUE, NettyByteBufUtil.readInt32(buf));

    buf.clear();
    NettyByteBufUtil.writeInt32(buf, Integer.MAX_VALUE);
    assertEquals(Integer.MAX_VALUE, NettyByteBufUtil.readInt32(buf));
  }

}
