package org.example.common.net;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.example.net.Connection;
import org.example.net.DefaultInvokeFuture;
import org.example.net.InvokeCallback;
import org.example.net.InvokeFuture;
import org.example.net.Message;
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
   * Rpc invocation return future.<br>
   *
   * @param conn    目标链接
   * @param message 请求消息
   * @param timeout 超时时间
   * @since 2021年08月15日 15:45:03
   */
  public <T> InvokeFuture<T> invoke(final Connection conn, final Message message,
      final long timeout) {
    final DefaultInvokeFuture<T> future = new DefaultInvokeFuture<>(message.msgId());
    return invokeWithFuture(conn, message, future, timeout);
  }


  /**
   * callBack Rpc invocation .<br>
   *
   * @param conn    目标链接
   * @param message 请求消息
   * @param timeout 超时时间
   * @since 2021年08月15日 15:45:03
   */
  public <T> void invokeWithCallBack(final Connection conn, final Message message,
      InvokeCallback<T> callback, final long timeout) {
    final DefaultInvokeFuture<T> future = new DefaultInvokeFuture<>(message.msgId(), callback);
    invokeWithFuture(conn, message, future, timeout);
  }

  /**
   * Rpc invocation with future returned.<br>
   *
   * @param conn    目标链接
   * @param message 请求消息
   * @param timeout 超时时间
   * @since 2021年08月15日 15:45:03
   */
  public <T> InvokeFuture<T> invokeWithFuture(final Connection conn, final Message message,
      DefaultInvokeFuture<T> future, final long timeout) {
    final int msgId = message.msgId();
    conn.addInvokeFuture(future);
    try {
      Future<?> timeoutFuture = conn.channel().eventLoop().schedule(() -> {
        DefaultInvokeFuture<T> f = conn.removeInvokeFuture(msgId);
      }, timeout, TimeUnit.MILLISECONDS);
      future.addTimeout(timeoutFuture);

      conn.channel().writeAndFlush(message).addListener(cf -> {
        if (!cf.isSuccess()) {
          DefaultInvokeFuture<T> f = conn.removeInvokeFuture(msgId);
          if (f != null) {
            f.cancelTimeout();
          }
          logger.error("Invoke send failed. The address is {}", conn.channel().remoteAddress(),
              cf.cause());
        }
      });
    } catch (Exception e) {
      DefaultInvokeFuture<T> f = conn.removeInvokeFuture(msgId);
      if (f != null) {
        f.cancelTimeout();
      }
      logger.error("Exception caught when sending invocation. The address is {}",
          conn.channel().remoteAddress(), e);
    }

    return future;
  }
}
