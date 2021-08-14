package org.example.net;

import java.util.concurrent.CountDownLatch;
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
  private int invokeId;
  /** 回调 */
  private InvokeCallback<InvokeFuture> callback;
  /** 结果 */
  private Message result;
  /** 异常 */
  private Throwable cause;
  /** 执行标记 */
  private final AtomicBoolean executeCallBackOnlyOnce = new AtomicBoolean(false);

  private final CountDownLatch latch = new CountDownLatch(1);

  public InvokeFuture(int invokeId, InvokeCallback<InvokeFuture> callback) {
    this.invokeId = invokeId;
    this.callback = callback;
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
    if (callback != null && executeCallBackOnlyOnce.compareAndExchange(false, true)) {
      callback.onResponse(this);
    }
  }

  public void putCause(Throwable cause) {
    this.cause = cause;
    latch.countDown();
  }

  public void completeNormally() {
    if (callback != null && executeCallBackOnlyOnce.compareAndExchange(false, true)) {
      callback.onResponse(this);
    }
  }

  public void completeThrowAble() {
    if (callback != null && executeCallBackOnlyOnce.compareAndExchange(false, true)) {
      callback.onException(cause);
    }
  }


  public boolean isDone() {
    return latch.getCount() <= 0;
  }


}
