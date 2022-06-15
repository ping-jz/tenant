package org.example.exec;

import io.netty.util.internal.shaded.org.jctools.queues.MpscUnboundedArrayQueue;
import java.io.Closeable;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认Actor实现
 * <p>此Actor共有4个状态，运行时会1至3中轮流切换。一旦关闭则不可在开启，关闭时不在接受新的任务，现在有任务会按顺序执行：</p>
 * <p>1:空闲(等待任务中)</p>
 * <p>2:等待(在线程任务队列等待调度)</p>
 * <p>3:执行中(获得控制权，正在执行任务)</p>
 *
 * @author ZJP
 * @since 2021年09月29日 23:40:00
 **/
public class DefaultExecutor implements Runnable, Executor, Closeable {

  public static final int IDLE = 1;
  public static final int WAITING = 2;
  public static final int RUNNING = 3;

  /**
   * Actor状态
   */
  private final AtomicInteger state;
  /**
   * 任务队列
   */
  private final Queue<Runnable> queue;
  /**
   * 线程池(推荐使用{@link java.util.concurrent.ForkJoinPool})
   */
  private final ExecutorService service;
  /**
   * 是否停止
   */
  private boolean stopping;

  public DefaultExecutor(ExecutorService service) {
    this(new MpscUnboundedArrayQueue<>(128), service);
  }

  public DefaultExecutor(Queue<Runnable> queue, ExecutorService service) {
    this.queue = queue;
    this.service = service;
    this.state = new AtomicInteger(IDLE);
  }

  @Override
  public void run() {
    boolean failed = !state.compareAndSet(WAITING, RUNNING);
    if (failed) {
      return;
    }

    int size = ThreadLocalRandom.current().nextInt(1, Short.MAX_VALUE);
    for (int i = 0; i < size; i++) {
      try {
        Runnable runnable = queue.poll();
        if (runnable == null) {
          break;
        }

        runnable.run();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    state.compareAndSet(RUNNING, IDLE);
    if (queue.peek() != null) {
      exec();
    }
  }

  /**
   * 提交任务
   *
   * @return 是否成功
   * @throws RuntimeException 如果队列停止收到新任务，报异常
   */
  public boolean offer(Runnable runnable) {
    if (stopping) {
      throw new RuntimeException("二级队列拒绝接收新任务");
    }

    boolean res = queue.add(runnable);
    exec();
    return res;
  }

  @Override
  public void close() {
    stopping = true;
  }

  /**
   * 把二级队列放到线程池等待执行
   */
  protected void exec() {
    try {
      if (state.compareAndSet(IDLE, WAITING)) {
        service.execute(this);
      }
    } catch (Exception e) {
      state.compareAndSet(WAITING, IDLE);
      e.printStackTrace();
    }
  }

  @Override
  public void execute(Runnable command) {
    if (stopping) {
      throw new RuntimeException("二级队列拒绝接收新任务");
    }

    queue.add(command);
    exec();
  }
}
