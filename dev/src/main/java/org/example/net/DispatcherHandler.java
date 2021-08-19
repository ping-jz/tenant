package org.example.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 消息分发适配器
 *
 * @author ZJP
 * @since 2021年08月19日 12:16:19
 **/
public class DispatcherHandler extends SimpleChannelInboundHandler<Message> {

  /** 分发器实现 */
  private Dispatcher dispatcher;

  public DispatcherHandler(Dispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
    dispatcher.dispatcher(ctx.channel(), msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
    ctx.fireChannelReadComplete();
  }

}
