package org.example.proxy.register;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import org.example.proxy.service.ProxyService;
import org.example.serde.NettyByteBufUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 服务器注册拦截器
 *
 * @author zhongjianping
 * @since 2022/12/13 15:10
 */
public class RegisterCodec extends SimpleChannelInboundHandler<ByteBuf> {

  private Logger logger  = LoggerFactory.getLogger(this.getClass());

  private ProxyService proxyService;

  public RegisterCodec(ProxyService proxyService) {
    this.proxyService = proxyService;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
    int length = msg.getInt(msg.readerIndex());
    if(length <= 0) {
      logger.error("[{}],非法消息长度:{}", ctx.channel().remoteAddress(), length);
      ctx.close();
      return;
    }

    proxyService.register(ctx.channel(), msg);
  }
}
