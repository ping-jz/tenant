package org.example.actor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "actorTest", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExecutorTest {

  private static int powerOf2;
  private static int tasks;

  @BeforeAll
  public static void beforeAll() {
    powerOf2 = nextPowerOfTwo(Runtime.getRuntime().availableProcessors());
    //看机器情况调整，过多会瞬间创建大量对象导致内存不足
    tasks = 100000000;
  }

  /**
   * 工作窃取
   *
   * @since 2021年10月07日 16:37:58
   */
  @RepeatedTest(5)
  @Order(1)
  public void forkJoinTest() throws InterruptedException {
    ExecutorService service = Executors.newWorkStealingPool(powerOf2);

    CountDownLatch latch = new CountDownLatch(tasks);
    Runnable task = () -> latch.countDown();
    IntStream.range(0, tasks).parallel().forEach(i -> service.execute(task));

    Assertions.assertTrue(latch.await(5, TimeUnit.MINUTES));
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
    ExecutorService service = Executors.newFixedThreadPool(powerOf2);

    CountDownLatch latch = new CountDownLatch(tasks);
    Runnable task = () -> latch.countDown();
    IntStream.range(0, tasks).parallel().forEach(i -> service.execute(task));

    Assertions.assertTrue(latch.await(5, TimeUnit.MINUTES));
    service.shutdown();
  }


  /**
   * 线程隔离，轮询分发任务
   *
   * @since 2021年10月07日 16:36:41
   */
  @RepeatedTest(5)
  @Order(3)
  public void threadPoolsTest() throws InterruptedException {
    ExecutorService[] services = new ExecutorService[powerOf2];
    for (int i = 0; i < powerOf2; i++) {
      services[i] = Executors.newSingleThreadExecutor();
    }

    CountDownLatch latch = new CountDownLatch(tasks);
    Runnable task = () -> latch.countDown();
    final int MARK = powerOf2 - 1;
    IntStream.range(0, tasks).parallel().forEach(i -> {
      services[i & MARK].execute(task);
    });

    Assertions.assertTrue(latch.await(5, TimeUnit.MINUTES));
    for (ExecutorService service : services) {
      service.shutdown();
    }
  }


  private static int nextPowerOfTwo(int value) {
    int highestOneBit = Integer.highestOneBit(value);
    if (value == highestOneBit) {
      return value;
    }
    return highestOneBit << 1;
  }

}
