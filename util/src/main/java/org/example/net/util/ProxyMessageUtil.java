package org.example.net.util;

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
public final class ProxyMessageUtil {

  private ProxyMessageUtil() {
  }

  /**
   * 封装中转消息
   *
   * @param source 源服务ID
   * @param target 目标服ID
   * @param proto  协议ID
   * @param msg    消息ID
   * @param body   消息体(使用之后会降低引用)
   * @since 2022年12月12日“ 18:32
   */
  public static ByteBuf proxyMessage(int source, int target, int proto, int msg, ByteBuf body) {
    return proxyMessage(PooledByteBufAllocator.DEFAULT.directBuffer(), source, target, proto,
        msg, body);
  }

  /**
   * 封装中转消息
   *
   * @param out    目标buff
   * @param source 源服务ID
   * @param target 目标服ID
   * @param proto  协议ID
   * @param msg    消息ID
   * @param body   消息体(使用之后会降低引用)
   * @since 2022年12月12日“ 18:32
   */
  public static ByteBuf proxyMessage(ByteBuf out, int source, int target, int proto, int msg,
      ByteBuf body) {
    //预留长度字段
    int writeIdx = out.writerIndex();
    out.writerIndex(writeIdx + Integer.BYTES);

    //写入头部和消息体, 之后释放body
    NettyByteBufUtil.writeInt32(out, target);
    NettyByteBufUtil.writeInt32(out, source);
    NettyByteBufUtil.writeInt32(out, proto);
    NettyByteBufUtil.writeInt32(out, msg);
    try {
      out.writeBytes(body);
    } finally {
      ReferenceCountUtil.release(body);
    }

    //写入长度
    int length = out.writerIndex() - writeIdx;
    out.setInt(writeIdx, length);
    return out;
  }

}
