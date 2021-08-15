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
  void onResponse(T result);

  /**
   * Exception caught.
   *
   * @param e the exception
   */
  default void onException(Throwable e) {
  }

}
