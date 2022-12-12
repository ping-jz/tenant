package org.example.net;

/**
 * 远程异步任务
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
