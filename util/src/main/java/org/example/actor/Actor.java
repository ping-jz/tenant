package org.example.actor;

/**
 * Actor接口(二级队列),配合ForkJoinPool来实现线程间的负载均衡
 *
 * @author ZJP
 * @since 2021年09月29日 23:32:25
 **/
public interface Actor extends Runnable {

  /**
   * 执行任务
   *
   * @since 2021年09月29日 23:33:08
   */
  @Override
  void run();


  /**
   * 调度Actor来执行此任务。此任务可能在一条新的线程，某个线程池，或者调用者线程执行
   *
   * @param runnable 任务
   * @since 2021年09月29日 23:35:19
   */
  boolean offer(Runnable runnable);

}
