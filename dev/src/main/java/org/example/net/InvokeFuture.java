package org.example.net;

/**
 * RPC异步回调
 */
public interface InvokeFuture<T> {

    /**
     * 执行成功
     */
    InvokeFuture<T> onSuc(InvokeCallback<T> t);

    /**
     * 执行失败
     */
    InvokeFuture<T> onErr(InvokeCallback<Message> t);

    void invoke();
}
