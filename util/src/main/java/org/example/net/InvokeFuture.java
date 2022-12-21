package org.example.net;

import org.example.util.Identity;

/**
 * 远程异步任务
 */
public interface InvokeFuture<T> extends Identity<Integer> {

    /**
     * 执行成功
     */
    InvokeFuture<T> onSuc(InvokeCallback<T> t);

    void invoke();
}
