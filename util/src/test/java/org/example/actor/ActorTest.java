package org.example.actor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * 当Actors数量少的时候，两种模式没有明显的区别
 *
 * @author ZJP
 * @since 2021年10月07日 16:53:57
 **/
@EnabledIfSystemProperty(named = "actorTest", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ActorTest {

  /** 线程数量 */
  private static int threads;
  /** 任务数量 */
  private static int tasks;
  /** actor数量 */
  private static int actorSize;
  /** actor数组下标求余 */
  private static int actorMark;

  @BeforeAll
  public static void beforeAll() {
    threads = Runtime.getRuntime().availableProcessors();
    //看机器情况调整，过多会瞬间创建大量对象导致内存不足
    tasks = 200000000;
    actorSize = nextPowerOfTwo(tasks / 10000);
    actorMark = actorSize - 1;
  }

  public Actor[] createActors(ExecutorService service, int size) {
    Actor[] actros = new DefaultActor[size];
    for (int i = 0; i < size; i++) {
      actros[i] = new DefaultActor(service);
    }

    return actros;
  }

  /**
   * 工作窃取
   *
   * @since 2021年10月07日 16:37:58
   */
  @RepeatedTest(5)
  @Order(1)
  public void forkJoinTest() throws InterruptedException {
    ExecutorService service = Executors.newWorkStealingPool(threads);
    Actor[] actors = createActors(service, actorSize);

    CountDownLatch latch = new CountDownLatch(tasks);
    AtomicInteger count = new AtomicInteger();
    Runnable run = () -> {
      latch.countDown();
      count.addAndGet(1);
    };
    IntStream.range(0, tasks).parallel().forEach(i -> actors[i & actorMark].offer(run));

    Assertions.assertTrue(latch.await(1, TimeUnit.MINUTES));
    Assertions.assertEquals(tasks, count.get());
    service.shutdown();
  }

  /**
   * n个线程共用一个总队列
   *
   * @since 2021年10月07日 16:36:41
   */
  @RepeatedTest(5)
  @Order(2)
  public void threadPoolTest() throws InterruptedException {
    ExecutorService service = Executors.newFixedThreadPool(threads);
    Actor[] actors = createActors(service, actorSize);

    CountDownLatch latch = new CountDownLatch(tasks);
    AtomicInteger count = new AtomicInteger();
    Runnable run = () -> {
      latch.countDown();
      count.addAndGet(1);
    };
    IntStream.range(0, tasks).parallel().forEach(i -> actors[i & actorMark].offer(run));

    Assertions.assertTrue(latch.await(1, TimeUnit.MINUTES));
    Assertions.assertEquals(tasks, count.get());
    service.shutdown();
  }


  private static int nextPowerOfTwo(int value) {
    int highestOneBit = Integer.highestOneBit(value);
    if (value == highestOneBit) {
      return value;
    }
    return highestOneBit << 1;
  }

}
