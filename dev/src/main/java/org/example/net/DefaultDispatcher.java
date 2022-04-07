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
    try {
      if (msg.isErr()) {
        logger.error("from:{}, proto:{}, 错误代码:{}", channel.remoteAddress(), msg.proto(),
            msg.status());
      }

      //TODO 这里需要增加, handler可以接受Connection和Message作为参数
      Object result = null;
      if (msg.packet() == null) {
        result = handler.invoke();
      } else if (msg.packet().getClass().isArray()) {
        result = handler.invoke((Object[]) msg.packet());
      } else {
        result = handler.invoke(msg.packet());
      }

      if (0 < msg.proto()) {
        Message response;
        result = extractResult(result);

        response = Message
            .of(Math.negateExact(msg.proto()))
            .msgId(msg.msgId())
            .status(MessageStatus.SUCCESS)
            .packet(result);

        if (result instanceof Message) {
          Message resMsg = (Message) result;
          if (resMsg.proto() != 0) {
            response.proto(Math.negateExact(msg.proto()));
          }

          if (resMsg.status() != MessageStatus.NONE.status()) {
            response.status(resMsg.status());
          }
        }
        channel.write(response);
      }

    } catch (Exception e) {
      if (0 < msg.proto()) {
        channel.write(Message.of(Math.negateExact(msg.proto())).msgId(msg.msgId())
            .status(MessageStatus.SERVER_EXCEPTION));
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
    if (obj instanceof ResultInvokeFuture) {
      res = ((ResultInvokeFuture<?>) obj).result();
    }
    return res;
  }


  @Override
  public void dispatcher(Channel channel, Message message) {
    doDispatcher(channel, message);
  }
}
