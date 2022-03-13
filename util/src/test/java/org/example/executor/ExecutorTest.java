package org.example.executor;

/**
 * jvmArgs: -ea -Xms2g -Xmx2g
 *
 * @since 2021年10月08日 14:35:10
 */
public final class ExecutorTest {

  public static final int TASKS = 1000000;
  public static final int REPEAT = 20;

  private ExecutorTest() {
  }

  public static int nextPowerOfTwo(int value) {
    int highestOneBit = Integer.highestOneBit(value);
    if (value == highestOneBit) {
      return value;
    }
    return highestOneBit << 1;
  }

}
