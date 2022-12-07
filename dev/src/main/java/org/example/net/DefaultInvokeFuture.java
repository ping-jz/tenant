package org.example.net;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 回调任务
 *
 * @author ZJP
 * @since 2021年08月14日 19:39:25
 **/
public class DefaultInvokeFuture<T> implements InvokeFuture<T> {

  /**
   * 回调ID
   */
  private int id;
  /**
   * 处理成功回调
   */
  private volatile InvokeCallback<T> sucCallBack;
  /**
   * 处理错误回调
   */
  private volatile InvokeCallback<Message> errCallBack;
  /**
   * 超时任务
   */
  private Future<?> timeout;
  /**
   * 执行标记
   */
  private AtomicBoolean executeCallbackOnlyOnce;
  /**
   * rpc实现
   */
  private BaseRemoting remoting;
  /**
   * 链接
   */
  private Connection connection;
  /**
   * 请求消息
   */
  private Message reqMessage;

  private DefaultInvokeFuture() {
    executeCallbackOnlyOnce = new AtomicBoolean(true);
  }

  public DefaultInvokeFuture(int invokeId) {
    this(invokeId, null);
  }

  public DefaultInvokeFuture(int invokeId, InvokeCallback<T> callback) {
    id = invokeId;
    executeCallbackOnlyOnce = new AtomicBoolean(false);
    sucCallBack = callback;
  }

  public void executeCallBack(Message message) {
    if (executeCallbackOnlyOnce.compareAndSet(false, true)) {
      try {
        if (message != null && message.isSuc()) {
          sucCallBack.onMessage((T) message.packet());
        } else if (errCallBack != null) {
          errCallBack.onMessage(message);
        }
      } catch (Exception e) {
        //TODO Logger me
      }
    }
  }

  public void addTimeout(Future<?> future) {
    timeout = future;
  }

  public void cancelTimeout() {
    if (timeout != null) {
      timeout.cancel(false);
      timeout = null;
    }
  }

  public int id() {
    return id;
  }

  @Override
  public InvokeFuture<T> onSuc(InvokeCallback<T> t) {
    sucCallBack = t;
    return this;
  }

  @Override
  public InvokeFuture<T> onErr(InvokeCallback<Message> t) {
    errCallBack = t;
    return this;
  }

  @Override
  public void invoke() {
    if (connection == null || remoting == null) {
      return;
    }

    remoting.invokeWithFuture(connection, reqMessage, this, 3000L);
  }

  public void remoting(BaseRemoting remoting) {
    this.remoting = remoting;
  }

  public Connection connection() {
    return connection;
  }

  public void connection(Connection connection) {
    this.connection = connection;
  }


  public void reqMessage(Message reqMessage) {
    this.reqMessage = reqMessage;
  }
}
