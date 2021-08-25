package org.example.net;

import io.netty.channel.Channel;
import org.example.net.handler.Handler;
import org.example.net.handler.HandlerRegistry;
import org.slf4j.Logger;

/**
 * 消息分发调度者(分发客户端的消息)
 *
 * @author ZJP
 * @since 2021年07月24日 14:41:27
 **/
public class GameDispatcher implements Dispatcher {

  /** 消息处理器集合 */
  private HandlerRegistry handlerRegistry;
  /** 日志 */
  private Logger logger;

  /**
   * 因为Logger隔离有点困难，但为了以后方便，Logger还是先有外部传进来
   *
   * @since 2021年07月24日 15:53:52
   */
  public GameDispatcher(Logger logger, HandlerRegistry handlerRegistry) {
    this.logger = logger;
    this.handlerRegistry = handlerRegistry;
  }

  /**
   * 根据{@link Message#packet()}进行消息分发
   *
   * @param channel 通信channel
   * @param msg 请求消息
   * @since 2021年07月24日 15:58:39
   */
  public void doDispatcher(Channel channel, Message msg) {
    Handler handler = handlerRegistry.getHandler(msg.proto());
    if (handler == null) {
      logger.error("地址:{}, 协议号:{}, 无对应处理器", channel.remoteAddress(), msg.proto());
      return;
    }

    try {
      Object result = null;
      if (msg.packet() == null) {
        result = handler.invoke();
      } else {
        result = handler.invoke(msg.packet());
      }
      //
      if (result != null && 0 < msg.proto()) {
        Message response = new Message();
        response.proto(Math.negateExact(msg.proto()));
        response.msgId(msg.msgId());
        response.packet(result);
        channel.write(response);
      }
    } catch (Exception e) {
      if (0 < msg.proto()) {
        channel.write(
            Message
                .of(Math.negateExact(msg.proto()))
                .msgId(msg.msgId())
                .status(MessageStatus.SERVER_EXCEPTION)
        );
      }
      logger.error("地址:{}, 协议号:{}, 处理错误", channel.remoteAddress(), msg.proto(), e);
    }
  }

  @Override
  public void dispatcher(Channel channel, Message message) {
    doDispatcher(channel, message);
  }
}
