package org.example.net;

import io.netty.channel.Channel;
import org.example.handler.Handler;
import org.example.handler.HandlerRegistry;
import org.slf4j.Logger;

/**
 * 消息分发调度者(分发客户端的消息)
 *
 * @author ZJP
 * @since 2021年07月24日 14:41:27
 **/
public class Dispatcher {

  /** 消息处理器集合 */
  private HandlerRegistry handlerRegistry;
  /** 日志 */
  private Logger logger;

  /**
   * 因为Logger隔离有点困难，但为了以后方便，Logger还是先有外部传进来
   *
   * @since 2021年07月24日 15:53:52
   */
  public Dispatcher(Logger logger, HandlerRegistry handlerRegistry) {
    this.logger = logger;
    this.handlerRegistry = handlerRegistry;
  }

  /**
   * 根据{@link Message#packet()}进行消息分发
   *
   * @param channel 通信channel
   * @param req 请求消息
   * @since 2021年07月24日 15:58:39
   */
  public void doDispatcher(Channel channel, Message req) {
    Handler handler = handlerRegistry.getHandler(req.proto());
    if (handler == null) {
      logger.error("地址:{}, 协议号:{}, 无对应处理器", channel.remoteAddress(), req.proto());
      return;
    }

    try {
      Object result = handler.invoke(req.packet());
      //
      if (result != null && 0 < req.proto()) {
        Message response = new Message();
        response.proto(Math.negateExact(req.proto()));
        response.msgId(req.msgId());
        response.packet(result);
        channel.write(response);
      }
    } catch (Exception e) {
      //TODO 处理进行错误处理或者错误通知，交由具体业务决定
      logger.error("地址:{}, 协议号:{}, 处理错误", channel.remoteAddress(), req.proto(), e);
    }
  }

}
