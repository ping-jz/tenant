package org.example.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
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
   * @param timeout 超时时间
   * @since 2021年08月15日 15:45:03
   */
  public <T> CompletableFuture<T> invoke(final Connection conn, final Message message,
      final long timeout, TimeUnit timeUnit) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    return invokeWithFuture(conn, message, future, timeout, timeUnit);
  }

  /**
   * Rpc invocation with future returned.<br>
   *
   * @param conn    目标链接
   * @param message 请求消息
   * @param timeout 超时时间
   * @since 2021年08月15日 15:45:03
   */
  public <T> CompletableFuture<T> invokeWithFuture(final Connection conn, final Message message,
      CompletableFuture<T> future, final long timeout, TimeUnit timeUnit) {
    Objects.requireNonNull(future);

    final int msgId = message.msgId();
    conn.addInvokeFuture(message.msgId(), future);
    Future<?> timeoutFuture = conn.channel().eventLoop().schedule(() -> {
      conn.removeInvokeFuture(msgId, future);
    }, timeout, timeUnit);
    try {
      conn.channel().writeAndFlush(message).addListener(cf -> {
        if (!cf.isSuccess()) {
          conn.removeInvokeFuture(msgId, future);
          timeoutFuture.cancel(false);
          logger.error("Invoke send failed. The address is {}", conn.channel().remoteAddress(),
              cf.cause());
        }
      });
    } catch (Exception e) {
      assert ReferenceCountUtil.release(message);
      conn.removeInvokeFuture(msgId, future);
      timeoutFuture.cancel(false);
      logger.error("Exception caught when sending invocation. The address is {}",
          conn.channel().remoteAddress(), e);
    }

    return future;
  }
}
