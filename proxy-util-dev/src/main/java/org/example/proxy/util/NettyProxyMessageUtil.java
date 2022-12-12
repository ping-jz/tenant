package org.example.proxy.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.example.serde.NettyByteBufUtil;

/**
 * 消息中转工具类
 *
 * @author zhongjianping
 * @since 2022/12/12 18:22
 */
public final class NettyProxyMessageUtil {

  private NettyProxyMessageUtil() {
  }

  /**
   * 封装中转消息
   *
   * @param source 源服务ID
   * @param target 目标服ID
   * @param body   消息体(使用之后会降低引用)
   * @since 2022年12月12日“ 18:32
   */
  public static ByteBuf proxyMessage(int source, int target, ByteBuf body) {
    return proxyMessage(PooledByteBufAllocator.DEFAULT.buffer(), source, target, body);
  }

  /**
   * 封装中转消息
   *
   * @param out    目标buff
   * @param source 源服务ID
   * @param target 目标服ID
   * @param body   消息体(使用之后会降低引用)
   * @since 2022年12月12日“ 18:32
   */
  public static ByteBuf proxyMessage(ByteBuf out, int source, int target, ByteBuf body) {
    //预留长度字段
    int writeIdx = out.writerIndex();
    out.writerIndex(writeIdx + Integer.BYTES);

    //写入头部和消息体, 之后释放body
    NettyByteBufUtil.writeInt32(out, source);
    NettyByteBufUtil.writeInt32(out, target);
    out.writeBytes(body);
    ReferenceCountUtil.release(body);

    //写入长度
    int length = out.writerIndex() - writeIdx;
    out.setInt(writeIdx, length);
    return out;
  }

}
