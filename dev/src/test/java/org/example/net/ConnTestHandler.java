package org.example.net;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 请求处理器
 *
 * @author ZJP
 * @since 2021年08月14日 21:14:19
 **/
@Sharable
public class ConnTestHandler extends SimpleChannelInboundHandler<Message> {

  private Logger logger;

  private AtomicInteger invokeTimes;

  private AtomicInteger connectionCount;

  public ConnTestHandler(String name) {
    invokeTimes = new AtomicInteger();
    connectionCount = new AtomicInteger();
    logger = LoggerFactory.getLogger(ConnTestHandler.class.getSimpleName() + "_" + name);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    connectionCount.incrementAndGet();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
    Connection connection = ctx.channel().attr(Connection.CONNECTION).get();
    if (connection == null) {
      return;
    }

    invokeTimes.incrementAndGet();
    if (msg.proto() < 0 && 0 != msg.msgId()) {
      InvokeFuture<?> future = connection.removeInvokeFuture(msg.msgId());
      if (future != null) {
        future.putMessage(msg);
        future.cancelTimeout();
        try {
          future.executeCallBack();
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
    } else if (0 < msg.proto()) {
      ctx.write(Message
          .of(-msg.proto())
          .msgId(msg.msgId())
          .status(MessageStatus.SUCCESS)
          .packet(msg.packet()));
    } else {
      logger.error("no handler found for message, proto:{}, msgId:{}, status:{}", msg.proto(),
          msg.msgId(),
          msg.status());
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  public int invokeTimes() {
    return invokeTimes.get();
  }

  public int connectionCount() {
    return connectionCount.get();
  }
}
