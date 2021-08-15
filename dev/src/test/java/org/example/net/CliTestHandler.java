package org.example.net;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 请求处理器
 *
 * @author ZJP
 * @since 2021年08月14日 21:14:19
 **/
@Sharable
public class CliTestHandler extends SimpleChannelInboundHandler<Message> {

  private final Logger logger = LoggerFactory.getLogger("RpcRemoting");

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
    Connection connection = ctx.channel().attr(Connection.CONNECTION).get();
    if (connection == null) {
      return;
    }

    InvokeFuture future = connection.removeInvokeFuture(msg.msgId());
    if (future != null) {
      future.putResult(msg);
      future.cancelTimeout();
      try {
        future.completeNormally();
      } catch (Exception e) {
        logger.error("Exception caught when executing invoke callback, id={}",
            msg.msgId(), e);
      }
    } else {
      logger
          .warn("Cannot find InvokeFuture, maybe already timeout, id={}, from={} ",
              msg.msgId(),
              ctx.channel().remoteAddress());
    }

  }
}
