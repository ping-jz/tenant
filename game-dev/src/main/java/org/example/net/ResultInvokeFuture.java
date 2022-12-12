package org.example.net;

/**
 * 单纯的包装类，不执行热更逻辑
 *
 * @author ZJP
 * @since 2021年08月14日 19:39:25
 **/
public class ResultInvokeFuture<T> implements InvokeFuture<T> {

  /**
   * 结果
   */
  private T result;

  public static <T> ResultInvokeFuture<T> withResult(T t) {
    ResultInvokeFuture<T> result = new ResultInvokeFuture<>();
    result.result = t;
    return result;
  }

  public T result() {
    return result;
  }

  @Override
  public InvokeFuture<T> onSuc(InvokeCallback<T> t) {
    throw new UnsupportedOperationException("此实现不支持回调");
  }

  @Override
  public InvokeFuture<T> onErr(InvokeCallback<Message> t) {
    throw new UnsupportedOperationException("此实现不支持回调");
  }

  @Override
  public void invoke() {
    throw new UnsupportedOperationException("此实现不支持回调");
  }
}
