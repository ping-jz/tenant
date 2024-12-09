package org.example.net;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.example.exec.DefaultIdentity;
import org.example.exec.VirtualExecutor;
import org.example.exec.VirutalExecutors;
import org.example.util.Identity;

/**
 * 对{@link CompletableFuture},按照业务环境进行线程分发
 *
 * @author zhongjianping
 * @since 2024/12/8 14:22
 */
public class CompleteAbleFuture<T> {

  private final CompletableFuture<T> f;
  private Future<?> timeOutFuture;

  public CompleteAbleFuture(CompletableFuture<T> f) {
    this.f = f;
  }

  public static <U> CompleteAbleFuture<U> of(CompletableFuture<U> f) {
    return new CompleteAbleFuture<>(f);
  }

  /**
   * 如果当前在VirtualThreadExecutor环境中。 则此回调则在相同VirtualTHreadExecuotr中执行。
   * {@link CompletableFuture#whenCompleteAsync(BiConsumer, Executor)}}
   * <p>
   * 如果当前不在在VirtualThreadExecutor环境中，则参考
   * {@link CompleteAbleFuture#whenCompleteAsync(BiConsumer, VirtualExecutor)}
   * <p>
   *
   * @author zhongjianping
   * @since 2024/12/8 14:15
   */
  public CompleteAbleFuture<T> whenComplete(
      BiConsumer<? super T, ? super Throwable> consumer) {
    VirtualExecutor executor = VirtualExecutor.current();
    Objects.requireNonNull(executor,
        "在非VirtualThreadExecutor环境中进行了回调。修复方法："
            + "1.使用org.example.net.CompleteAbleFuture.future()此方法来获取真实的CompleteAbleFuture进行调用"
            + "2.使用org.example.net.CompleteAbleFuture#whenCompleteAsync(BiConsumer, VirtualExecutor)");
    Identity identity = executor.getIdentity();
    f.whenCompleteAsync(consumer, r -> {
      VirutalExecutors.commonPool().executeWith(identity, r);
    });
    return this;
  }

  /**
   * {@link CompletableFuture#whenCompleteAsync(BiConsumer, Executor)}}
   */
  public CompleteAbleFuture<T> whenCompleteAsync(
      BiConsumer<? super T, ? super Throwable> consumer, VirtualExecutor executor) {
    Objects.requireNonNull(executor, "executor不能为空");
    Identity identity = executor.getIdentity();
    f.whenCompleteAsync(consumer, r -> {
      VirutalExecutors.commonPool().executeWith(identity, r);
    });
    return this;
  }

  /**
   * {@link CompletableFuture#get(long, TimeUnit)}}
   */
  public T get() throws ExecutionException, InterruptedException, TimeoutException {
    return get(3, TimeUnit.SECONDS);
  }

  /**
   * {@link CompletableFuture#get(long, TimeUnit)}}
   */
  public T get(long timeout, TimeUnit unit)
      throws ExecutionException, InterruptedException, TimeoutException {
    return f.get(timeout, unit);
  }

  /**
   * @return {@link CompletableFuture}
   * @since 2024/12/8 16:20
   */
  public CompletableFuture<T> future() {
    return f;
  }

  /**
   * {@link CompletableFuture#cancel(boolean)}
   *
   * @since 2024/12/8 21:06
   */
  public boolean cancel(boolean mayInterruptIfRunning) {
    return f.cancel(mayInterruptIfRunning);
  }

  /**
   * {@link CompletableFuture#cancel(boolean)}
   *
   * @since 2024/12/8 21:06
   */
  public boolean completeExceptionally(Throwable ex) {
    return f.completeExceptionally(ex);
  }

  public Future<?> timeOut(long time, TimeUnit unit) {
    if (timeOutFuture != null) {
      timeOutFuture.cancel(false);
    }

    timeOutFuture = VirutalExecutors.commonPool().schedule(DefaultIdentity.DEFAULT, () -> {
      f.completeExceptionally(new TimeoutException("回调函数过期"));
    }, time, unit);
    return timeOutFuture;
  }

  Future<?> timeOutFuture() {
    return timeOutFuture;
  }

  CompleteAbleFuture<T> timeOutFuture(Future<?> future) {
    if (timeOutFuture != null) {
      timeOutFuture.cancel(false);
    }
    timeOutFuture = future;
    return this;
  }

  /**
   * {@link CompletableFuture#supplyAsync(Supplier, Executor)}
   *
   * @since 2024/12/8 17:31
   */
  public static <U> CompleteAbleFuture<U> supplyAsync(Supplier<U> supplier,
      Executor executor) {
    Objects.requireNonNull(executor, "executor不能为空");
    return new CompleteAbleFuture<>(CompletableFuture.supplyAsync(supplier, executor));
  }
}
