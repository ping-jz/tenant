package org.example.net;

/**
 * 异常处理回调
 *
 * @author jiangping
 * @version $Id: InvokeCallback.java, v 0.1 2015-9-30 AM10:24:26 tao Exp $
 */
@FunctionalInterface
public interface ErrCallback<T> extends InvokeCallback<T> {


  /**
   * Response received.
   *
   * @param result 空消息，消息状态未错误的都会被InvokeFuture调用
   */
  void onMessage(T result);

  /**
   * 执行回调时发生异常，都会被InvokeFuture调用
   *
   * @param e the exception
   */
  default void onException(Throwable e) {
    throw new RuntimeException(e);
  }

  /**
   * 成功回调(如果需要处理失败的结果，直接使用{@link ErrCallback})
   *
   * @author ZJP
   * @since 2021年09月01日 17:53:08
   **/
  final class DefaultCallBack<T> implements
      ErrCallback<T> {

    private static final DefaultCallBack<?> DEFAULT = new DefaultCallBack<>();

    public static <T> DefaultCallBack<T> instance() {
      return (DefaultCallBack<T>) DEFAULT;
    }

    @Override
    public void onMessage(Object result) {
      //Do nothing
    }


    @Override
    public void onException(Throwable result) {
      //Just throw
      throw new RuntimeException(result);
    }


  }

}
