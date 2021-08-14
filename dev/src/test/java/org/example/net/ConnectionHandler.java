package org.example.net;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 请求处理器
 *
 * @author ZJP
 * @since 2021年08月14日 21:14:19
 **/
@Sharable
public class ConnectionHandler extends SimpleChannelInboundHandler<Message> {

  private AtomicInteger invokeTimes;

  private AtomicInteger connectionCount;

  public ConnectionHandler() {
    invokeTimes = new AtomicInteger();
    connectionCount = new AtomicInteger();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    connectionCount.incrementAndGet();
  }


  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
    invokeTimes.incrementAndGet();
  }

  public int invokeTimes() {
    return invokeTimes.get();
  }

  public int connectionCount() {
    return connectionCount.get();
  }
}
