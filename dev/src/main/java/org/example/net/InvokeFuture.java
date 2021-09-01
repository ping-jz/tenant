package org.example.net;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 回调任务
 *
 * @author ZJP
 * @since 2021年08月14日 19:39:25
 **/
public class InvokeFuture<T> {

  /** 回调ID */
  private int id;
  /** 回调 */
  private InvokeCallback<Message> callback;
  /** 返回消息 */
  private Message message;
  /** 结果 */
  private Object result;
  /** 异常 */
  private Throwable cause;
  /** 超时任务 */
  private Future<?> timeout;
  /** 执行标记 */
  private AtomicBoolean executeCallbackOnlyOnce;
  /** 同步组件 */
  private CountDownLatch latch;

  public static <T> InvokeFuture<T> withResult(T t) {
    InvokeFuture<T> result = new InvokeFuture<>();
    result.result = t;
    return result;
  }

  private InvokeFuture() {
    executeCallbackOnlyOnce = new AtomicBoolean(true);
    latch = new CountDownLatch(0);
  }

  public InvokeFuture(int invokeId) {
    this(invokeId, null);
  }

  public InvokeFuture(int invokeId, InvokeCallback<Message> callback) {
    id = invokeId;
    latch = new CountDownLatch(1);
    executeCallbackOnlyOnce = new AtomicBoolean(false);
    this.callback = callback;
  }

  public Message waitResponse(long timeoutMillis) throws InterruptedException {
    latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
    return message;
  }

  public Message waitResponse() throws InterruptedException {
    latch.await();
    return message;
  }

  public void putMessage(Message response) {
    message = response;
    result = response.packet();
    latch.countDown();
  }

  public void putCause(Throwable cause) {
    this.cause = cause;
    latch.countDown();
  }

  public void executeThrowAble() {
    if (callback != null && executeCallbackOnlyOnce.compareAndExchange(false, true)) {
      callback.onException(cause);
    }
  }

  public void executeCallBack() {
    if (callback != null) {
      if (executeCallbackOnlyOnce.compareAndSet(false, true)) {
        try {
          callback.onResponse(message);
        } catch (Exception e) {
          callback.onException(e);
        }
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

  public boolean isDone() {
    return latch.getCount() <= 0;
  }

  public T result() {
    return (T) result;
  }

  public int id() {
    return id;
  }
}
