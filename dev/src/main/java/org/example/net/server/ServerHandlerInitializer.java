package org.example.net.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
import org.example.net.ServerIdleHandler;
import org.example.net.codec.MessageCodec;

/**
 * Channel处理器
 *
 * @author ZJP
 * @since 2021年08月13日 21:41:18
 **/
public class ServerHandlerInitializer extends ChannelInitializer<SocketChannel> {

  private MessageCodec codec;
  private ServerIdleHandler idleHandler;

  private ChannelHandler handler;

  public ServerHandlerInitializer(ChannelHandler channelHandler) {
    idleHandler = new ServerIdleHandler();
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, ServerIdleHandler.IDLE_TIME,
        TimeUnit.MILLISECONDS));
    pipeline.addLast("serverIdleHandler", idleHandler);
    pipeline.addLast("codec", codec);
    pipeline.addLast("handler", handler);
  }
}
