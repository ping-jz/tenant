package org.example.net;

/**
 * 回调方法
 */
@FunctionalInterface
public interface InvokeCallback<T> {

  /**
   * Response received.
   *
   * @param result the result,
   */
  void onMessage(T result);
}
