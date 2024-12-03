package org.example.net;

import io.netty.util.ReferenceCountUtil;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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
    } catch (Throwable e) {
      assert ReferenceCountUtil.release(request);
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
   * @since 2021年08月15日 15:45:03
   */
  public <T> CompletableFuture<T> invoke(ConnectionManager manager, final Connection conn,
      final Message message, int msgId) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    return invokeWithFuture(manager, conn, message, msgId, future, 3, TimeUnit.SECONDS);
  }

  /**
   * Rpc invocation with future returned.<br>
   *
   * @param conn    目标链接
   * @param message 请求消息
   * @param timeout 超时时间
   * @since 2021年08月15日 15:45:03
   */
  public <T> CompletableFuture<T> invokeWithFuture(ConnectionManager manager, Connection conn,
      Message message,
      int msgId, CompletableFuture<T> future, final long timeout, TimeUnit timeUnit) {
    Objects.requireNonNull(future);

    manager.addInvokeFuture(conn, msgId, future);
    Future<?> timeoutFuture = conn.channel().eventLoop().schedule(() -> {
      manager.removeInvokeFuture(msgId, future);
    }, timeout, timeUnit);
    try {
      conn.channel().writeAndFlush(message).addListener(cf -> {
        if (!cf.isSuccess()) {
          manager.removeInvokeFuture(msgId, future);
          timeoutFuture.cancel(false);
          logger.error("Invoke send failed. The address is {}", conn.channel().remoteAddress(),
              cf.cause());
        }
      });
    } catch (Exception e) {
      assert ReferenceCountUtil.release(message);
      manager.removeInvokeFuture(msgId, future);
      timeoutFuture.cancel(false);
      logger.error("Exception caught when sending invocation. The address is {}",
          conn.channel().remoteAddress(), e);
    }

    return future;
  }
}
