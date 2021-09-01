package org.example.net;

/**
 * Invoke callback.
 *
 * @author jiangping
 * @version $Id: InvokeCallback.java, v 0.1 2015-9-30 AM10:24:26 tao Exp $
 */
@FunctionalInterface
public interface InvokeCallback<T> {

  /**
   * Response received.
   *
   * @param result the result
   */
  void onMessage(T result);

  /**
   * Exception caught, default rethrow again.
   *
   * @param e the exception
   */
  default void onException(Throwable e) {
    throw new RuntimeException(e);
  }

  /**
   * 成功回调(如果需要处理失败的结果，直接使用{@link InvokeCallback})
   *
   * @author ZJP
   * @since 2021年09月01日 17:53:08
   **/
  @FunctionalInterface
  interface DefaultCallBack<T> extends InvokeCallback<Message> {

    /**
     * 只处理
     *
     * @param result the result
     */
    default void onMessage(Message result) {
        onSuc((T) result.packet());
    }


    void onSuc(T t);
  }

}
