package org.example.net;

import io.netty.channel.Channel;
import org.example.net.handler.Handler;
import org.example.net.handler.HandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 消息分发调度者(服务器之间)
 *
 * @author ZJP
 * @since 2021年07月24日 14:41:27
 **/
public class DefaultDispatcher implements Dispatcher {

  /**
   * 消息处理器集合
   */
  private HandlerRegistry handlerRegistry;
  /**
   * 日志
   */
  private Logger logger;

  public DefaultDispatcher(HandlerRegistry handlerRegistry) {
    this.handlerRegistry = handlerRegistry;
    logger = LoggerFactory.getLogger(getClass());
  }

  /**
   * 因为Logger隔离有点困难，但为了以后方便，Logger还是先有外部传进来
   *
   * @since 2021年07月24日 15:53:52
   */
  public DefaultDispatcher(Logger logger, HandlerRegistry handlerRegistry) {
    this.logger = logger;
    this.handlerRegistry = handlerRegistry;
  }

  /**
   * 根据{@link Message#proto()}进行消息分发
   *
   * @param channel 通信channel
   * @param req     请求消息
   * @since 2021年07月24日 15:58:39
   */
  public void doDispatcher(Channel channel, Message req) {
    Handler handler = handlerRegistry.getHandler(req.proto());
    if (handler == null && req.msgId() == 0) {
      logger.error("地址:{}, 协议号:{}, 消息ID:{} 无对应处理器", channel.remoteAddress(),
          req.proto(),
          req.msgId());
      return;
    }

    if (0 < req.msgId() && req.proto() < 0) {
      invokeFuture(channel, req);
    } else {
      invokeHandler(channel, req, handler);
    }
  }

  /**
   * 执行回调
   *
   * @param msg 请求消息
   * @since 2021年08月15日 20:22:04
   */
  private void invokeFuture(Channel channel, Message msg) {
    Connection connection = channel.attr(Connection.CONNECTION).get();
    if (connection == null) {
      return;
    }

    DefaultInvokeFuture<?> future = connection.removeInvokeFuture(msg.msgId());
    if (future != null) {
      future.cancelTimeout();
      try {
        future.executeCallBack(msg);
      } catch (Exception e) {
        logger.error("Exception caught when executing invoke callback, id={}", msg.msgId(), e);
      }
    } else {
      logger.warn("Cannot find InvokeFuture, maybe already timeout, id={}, from={} ", msg.msgId(),
          channel.remoteAddress());
    }
  }

  /**
   * 执行处理器
   *
   * @param msg     请求消息
   * @param handler 注册的处理器
   * @since 2021年08月15日 20:22:04
   */
  private void invokeHandler(Channel channel, Message msg, Handler handler) {
    Connection connection = channel.attr(Connection.CONNECTION).get();
    if (connection == null) {
      logger.error("channel:{}, 没有与绑定Connection。无法处理消息:{}", channel.remoteAddress(), msg.proto());
      return;
    }

    try {
      byte[] result = handler.invoke(connection, msg);

      if (0 < msg.proto() && result != null) {
        Message response = Message.of(Math.negateExact(msg.proto()))
            //.target(msg.source())
            .msgId(msg.msgId())
            .packet(result);

        channel.write(response);
      }

    } catch (Throwable e) {
      logger.error("from:{}, proto:{}, handler error", channel.remoteAddress(), msg.proto(), e);
    }
  }

  @Override
  public void dispatcher(Channel channel, Message message) {
    doDispatcher(channel, message);
  }
}
