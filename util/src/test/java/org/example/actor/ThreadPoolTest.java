package org.example.actor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "actorTest", matches = "true")
public class ThreadPoolTest {


  protected static ExecutorService service;
  protected static int powerOf2;
  protected static int tasks;

  protected static int nextPowerOfTwo(int value) {
    int highestOneBit = Integer.highestOneBit(value);
    if (value == highestOneBit) {
      return value;
    }
    return highestOneBit << 1;
  }

  @BeforeAll
  public static void beforeAll() {
    powerOf2 = nextPowerOfTwo(Runtime.getRuntime().availableProcessors());
    //看机器情况调整，过多会瞬间创建大量对象导致内存不足
    tasks = ExecutorTest.TASKS;
    service = Executors.newFixedThreadPool(powerOf2);
  }

  @AfterAll
  public static void afterAll() {
    if (service != null) {
      service.shutdown();
    }
  }

  /**
   * 工作窃取
   *
   * @since 2021年10月07日 16:37:58
   */
  @RepeatedTest(ExecutorTest.REPEAT)
  public void test() throws InterruptedException {

    CountDownLatch latch = new CountDownLatch(tasks);
    Runnable task = () -> latch.countDown();
    IntStream.range(0, tasks).parallel().forEach(i -> service.execute(task));

    Assertions.assertTrue(latch.await(5, TimeUnit.MINUTES));
  }


}
