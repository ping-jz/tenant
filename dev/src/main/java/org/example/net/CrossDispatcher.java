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
public class CrossDispatcher implements Dispatcher {

  /** 消息处理器集合 */
  private HandlerRegistry handlerRegistry;
  /** 日志 */
  private Logger logger;

  public CrossDispatcher(HandlerRegistry handlerRegistry) {
    this.handlerRegistry = handlerRegistry;
    logger = LoggerFactory.getLogger(getClass());
  }

  /**
   * 因为Logger隔离有点困难，但为了以后方便，Logger还是先有外部传进来
   *
   * @since 2021年07月24日 15:53:52
   */
  public CrossDispatcher(Logger logger, HandlerRegistry handlerRegistry) {
    this.logger = logger;
    this.handlerRegistry = handlerRegistry;
  }

  /**
   * 根据{@link Message#proto()}进行消息分发
   *
   * @param channel 通信channel
   * @param req 请求消息
   * @since 2021年07月24日 15:58:39
   */
  public void doDispatcher(Channel channel, Message req) {
    Handler handler = handlerRegistry.getHandler(req.proto());
    if (handler == null && req.msgId() == 0) {
      logger.error("地址:{}, 协议号:{}, 消息ID:{} 无对应处理器", channel.remoteAddress(), req.proto(),
          req.msgId());
      return;
    }

    if (handler == null) {
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
              channel.remoteAddress());
    }
  }

  /**
   * 执行处理器
   *
   * @param msg 请求消息
   * @param handler 注册的处理器
   * @since 2021年08月15日 20:22:04
   */
  private void invokeHandler(Channel channel, Message msg, Handler handler) {
    try {
      if (msg.isSuc()) {
        Object result = null;
        if (msg.packet() == null) {
          result = handler.invoke();
        } else if (msg.packet().getClass().isArray()) {
          result = handler.invoke((Object[]) msg.packet());
        } else {
          result = handler.invoke(msg.packet());
        }
        //
        if (result != null && 0 < msg.proto()) {
          Message response = Message
              .of(Math.negateExact(msg.proto()))
              .msgId(msg.msgId())
              .status(MessageStatus.SUCCESS)
              .packet(extractResult(result));
          channel.write(response);
        }
      } else {
        logger.error("from:{}, proto:{}, 错误代码:{}", channel.remoteAddress(), msg.proto(),
            msg.status());
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
      logger.error("from:{}, proto:{}, handler error", channel.remoteAddress(), msg.proto(), e);
    }
  }

  /**
   * 处理特殊返回值，如{@link InvokeFuture}
   *
   * @param obj 调用返回值
   * @since 2021年08月31日 15:49:56
   */
  private Object extractResult(Object obj) {
    Object res = obj;
    if (obj instanceof InvokeFuture) {
      res = ((InvokeFuture<?>) obj).result();
    }
    return res;
  }


  @Override
  public void dispatcher(Channel channel, Message message) {
    doDispatcher(channel, message);
  }
}
