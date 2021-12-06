package org.example.actor;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认Actor实现
 * <p>此Actor共有3个状态，运行时会1至3中轮流切换。一旦关闭则不可在开启，关闭时不在接受新的任务，现在有任务会按顺序执行：</p>
 * <p>1:空闲(等待任务中)</p>
 * <p>2:等待(在线程任务队列等待调度)</p>
 * <p>3:执行中(获得控制权，正在执行任务)</p>
 *
 * @author ZJP
 * @since 2021年09月29日 23:40:00
 **/
public class DefaultActor implements Actor {

  public static final int IDLE = 1;
  public static final int WAITING = 2;
  public static final int RUNNING = 3;

  /** Actor状态 */
  private final AtomicInteger state;
  /** 任务队列 */
  private final Queue<Runnable> queue;
  /** 线程池(推荐使用{@link java.util.concurrent.ForkJoinPool}) */
  private final ExecutorService service;
  /** 是否停止 */
  private boolean stopped;

  public DefaultActor(ExecutorService service) {
    state = new AtomicInteger(IDLE);
    queue = new ConcurrentLinkedQueue<>();
    this.service = service;
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
        //TODO log me
      }
    }
    state.compareAndSet(RUNNING, IDLE);
    if (queue.peek() != null) {
      exec();
    }
  }

  @Override
  public boolean offer(Runnable runnable) {
    if (isStopped()) {
      return false;
    }

    boolean res = queue.add(runnable);
    exec();
    return res;
  }

  public boolean isStopped() {
    return stopped;
  }

  public void stopped() {
    stopped = true;
  }

  protected void exec() {
    try {
      if (state.compareAndSet(IDLE, WAITING)) {
        service.execute(this);
      }
    } catch (Exception e) {
      state.compareAndSet(WAITING, IDLE);
      //TODO Log me
    }
  }
}
