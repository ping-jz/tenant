package org.example.net;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.example.net.InvokeCallback.DefaultSucCallBack;

/**
 * 回调任务
 *
 * @author ZJP
 * @since 2021年08月14日 19:39:25
 **/
public class InvokeFuture<T> {

  /** 回调ID */
  private int id;
  /** 返回消息 */
  private volatile Message message;
  /** 结果 */
  private Object result;
  /** 处理成功回调 */
  private volatile InvokeCallback<Message> callback;
  /** 处理错误回调 */
  private volatile ErrCallback<Message> errCallBack;
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
    this.errCallBack = ErrCallback.DefaultCallBack.instance();
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


  /**
   * 只处理成功的结果，成功的判断请看{@link Message#isSuc()},如果需要完整的流程，使用{@link InvokeFuture#onSucMsg(InvokeCallback)}
   *
   * @param callback 回调方法
   * @since 2021年09月01日 18:05:17
   */
  public void onSuc(DefaultSucCallBack<T> callback) {
    onSucMsg(callback);
  }

  /**
   * 当回调完成时,如果此Future已完成。则在调用者线程执行
   *
   * @param callback 回调方法
   * @since 2021年09月01日 18:05:17
   */
  public void onSucMsg(InvokeCallback<Message> callback) {
    this.callback = callback;
  }

  /**
   * 回调失败接口
   *
   * @param errCallBack 回调方法
   * @since 2021年09月01日 18:05:17
   */
  public void onErr(ErrCallback<Message> errCallBack) {
    this.errCallBack = errCallBack;
  }

  public void putCause(Throwable cause) {
    this.cause = cause;
    latch.countDown();
  }

  public void executeThrowAble() {
    if (errCallBack != null && executeCallbackOnlyOnce.compareAndExchange(false, true)) {
      errCallBack.onException(cause);
    }
  }

  public void executeCallBack() {
    if (executeCallbackOnlyOnce.compareAndSet(false, true)) {
      try {
        if (callback != null && message != null && message.isSuc()) {
          callback.onMessage(message);
        } else if (errCallBack != null) {
          errCallBack.onMessage(message);
        }
      } catch (Exception e) {
        if (errCallBack != null) {
          errCallBack.onException(e);
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
