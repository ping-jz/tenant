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
public class ThreadPoolsTest {


  protected static ExecutorService[] services;
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
    services = new ExecutorService[powerOf2];
    for (int i = 0; i < powerOf2; i++) {
      services[i] = Executors.newSingleThreadExecutor();
    }
  }

  @AfterAll
  public static void afterAll() {
    for (ExecutorService service : services) {
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
    final int MARK = powerOf2 - 1;
    IntStream.range(0, tasks).parallel().forEach(i -> {
      services[i & MARK].execute(task);
    });

    Assertions.assertTrue(latch.await(5, TimeUnit.MINUTES));
  }


}
