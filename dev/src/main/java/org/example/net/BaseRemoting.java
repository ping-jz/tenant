package org.example.net;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseRemoting {

  private Logger logger = LoggerFactory.getLogger(getClass());

  public void invoke(final Connection conn, final Message request) {
    try {
      conn.channel().writeAndFlush(request).addListener(f -> {
        if (!f.isSuccess()) {
          logger.error("Invoke send failed. The address is {}", conn.channel().remoteAddress(),
              f.cause());
        }
      });
    } catch (Exception e) {
      if (null == conn) {
        logger.error("Conn is null");
      } else {
        logger.error("Exception caught when sending invocation. The address is {}",
            conn.channel().remoteAddress(), e);
      }
    }
  }

  /**
   * Rpc invocation with future returned.<br>
   *
   * @param conn 目标链接
   * @param message 请求消息
   * @param timeout 超时时间
   * @since 2021年08月15日 15:45:03
   */
  public InvokeFuture invokeWithFuture(final Connection conn, final Message message,
      final long timeout) {
    final InvokeFuture future = new InvokeFuture(message.msgId());
    final int msgId = message.msgId();
    conn.addInvokeFuture(future);
    try {
      Future<?> timeoutFuture = conn.channel().eventLoop().schedule(() -> {
        InvokeFuture f = conn.removeInvokeFuture(msgId);
        if (f != null) {
          f.putResult(Message.of().status(MessageStatus.TIMEOUT));
        }
      }, timeout, TimeUnit.MILLISECONDS);
      future.addTimeout(timeoutFuture);

      conn.channel().writeAndFlush(message).addListener(cf -> {
        if (!cf.isSuccess()) {
          InvokeFuture f = conn.removeInvokeFuture(msgId);
          if (f != null) {
            f.cancelTimeout();
            f.putResult(Message.of().status(MessageStatus.SEND_ERROR));
          }
          logger.error("Invoke send failed. The address is {}",
              conn.channel().remoteAddress(), cf.cause());
        }
      });
    } catch (Exception e) {
      InvokeFuture f = conn.removeInvokeFuture(msgId);
      if (f != null) {
        f.cancelTimeout();
        f.putCause(e);
        f.executeThrowAble();
      }
      logger.error("Exception caught when sending invocation. The address is {}",
          conn.channel().remoteAddress(), e);
    }

    return future;
  }

  /**
   * Rpc invocation with future returned.<br>
   *
   * @param conn 目标链接
   * @param message 请求消息
   * @param timeout 超时时间
   * @since 2021年08月15日 15:45:03
   */
  public void invokeWithCallBack(final Connection conn, final Message message,
      InvokeCallback<?> callback,
      final long timeout) {
    final InvokeFuture future = new InvokeFuture(message.msgId(), callback);
    final int msgId = message.msgId();
    conn.addInvokeFuture(future);
    try {
      Future<?> timeoutFuture = conn.channel().eventLoop().schedule(() -> {
        InvokeFuture f = conn.removeInvokeFuture(msgId);
        if (f != null) {
          f.putResult(Message.of().status(MessageStatus.TIMEOUT));
          f.executeCallBack();
        }
      }, timeout, TimeUnit.MILLISECONDS);
      future.addTimeout(timeoutFuture);

      conn.channel().writeAndFlush(message).addListener(cf -> {
        if (!cf.isSuccess()) {
          InvokeFuture f = conn.removeInvokeFuture(msgId);
          if (f != null) {
            f.cancelTimeout();
            f.putResult(Message.of().status(MessageStatus.SEND_ERROR));
            f.executeCallBack();
          }
          logger.error("Invoke send failed. The address is {}",
              conn.channel().remoteAddress(), cf.cause());
        }
      });
    } catch (Exception e) {
      InvokeFuture f = conn.removeInvokeFuture(msgId);
      if (f != null) {
        f.cancelTimeout();
        f.putResult(Message.of().status(MessageStatus.SEND_ERROR));
        f.putCause(e);
        f.executeThrowAble();
      }
      logger.error("Exception caught when sending invocation. The address is {}",
          conn.channel().remoteAddress(), e);
    }
  }
}
