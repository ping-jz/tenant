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
public class InvokeFuture {

  /** 回调ID */
  private int id;
  /** 回调 */
  private InvokeCallback<Object> callback;
  /** 结果 */
  private Message result;
  /** 异常 */
  private Throwable cause;
  /** 超时任务 */
  private Future<?> timeout;
  /** 执行标记 */
  private final AtomicBoolean executeCallbackOnlyOnce = new AtomicBoolean(false);

  private final CountDownLatch latch = new CountDownLatch(1);

  public InvokeFuture(int invokeId) {
    id = invokeId;
  }

  public InvokeFuture(int invokeId, InvokeCallback<?> callback) {
    id = invokeId;
    this.callback = (InvokeCallback<Object>) callback;
  }

  public Message waitResponse(long timeoutMillis) throws InterruptedException {
    latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
    return result;
  }

  public Message waitResponse() throws InterruptedException {
    latch.await();
    return result;
  }

  public void putResult(Message response) {
    result = response;
    latch.countDown();
  }

  public void putCause(Throwable cause) {
    this.cause = cause;
    latch.countDown();
  }

  public void completeThrowAble() {
    if (callback != null && executeCallbackOnlyOnce.compareAndExchange(false, true)) {
      callback.onException(cause);
    }
  }

  public void completeNormally() {
    if (callback != null) {
      if (executeCallbackOnlyOnce.compareAndSet(false, true)) {
        callback.onResponse(result);
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

  public int id() {
    return id;
  }
}
