package org.example.common.net;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * IDGenerator is used for generating request id in integer form.
 */
public class MessageIdGenerator {

  private static final AtomicInteger id = new AtomicInteger(0);

  /**
   * generate the next id
   */
  public static int nextId() {
    return id.incrementAndGet();
  }
}
