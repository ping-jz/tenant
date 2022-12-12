package org.example.proxy.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.example.serde.NettyByteBufUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class NettyProxyMessageUtilTest {

    @Test
    public void readWriteTest() {
        int source = 1000;
        int target = -source;
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes("Hello, World".getBytes(StandardCharsets.UTF_8));
        ByteBuf out = NettyProxyMessageUtil.proxyMessage(Unpooled.buffer(), source, target, buf);

        //skip the length
        out.skipBytes(Integer.BYTES);
        Assertions.assertEquals(source, NettyByteBufUtil.readInt32(out));
        Assertions.assertEquals(target, NettyByteBufUtil.readInt32(out));

        byte[] bytes = new byte[out.readableBytes()];
        out.readBytes(bytes);
        Assertions.assertEquals("Hello, World", new String(bytes, StandardCharsets.UTF_8));

        Assertions.assertEquals(0, out.readableBytes());
    }
}
