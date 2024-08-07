package org.example.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import org.example.net.util.ProxyMessageUtil;
import org.example.serde.NettyByteBufUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MessageUtilTest {

  @Test
  public void readWriteTest() {
    int source = 1000;
    int target = -source;
    ByteBuf buf = Unpooled.buffer();
    buf.writeBytes("Hello, World".getBytes(StandardCharsets.UTF_8));
    ByteBuf out = ProxyMessageUtil.proxyMessage(Unpooled.buffer(), source, target, 1, 2, buf);

    //skip the length
    out.skipBytes(Integer.BYTES);
    Assertions.assertEquals(target, NettyByteBufUtil.readInt32(out));
    Assertions.assertEquals(source, NettyByteBufUtil.readInt32(out));
    Assertions.assertEquals(1, NettyByteBufUtil.readInt32(out));
    Assertions.assertEquals(2, NettyByteBufUtil.readInt32(out));

    byte[] bytes = new byte[out.readableBytes()];
    out.readBytes(bytes);
    Assertions.assertEquals("Hello, World", new String(bytes, StandardCharsets.UTF_8));

    Assertions.assertEquals(0, out.readableBytes());
  }
}
