package org.example.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RejectedExecutionException;

public final class WorkQueue<T> {

  volatile int source;       // source queue id, or sentinel
  int id;                    // pool index, mode, tag
  int base;                  // index of next slot for poll
  int top;                   // index of next slot for push
  volatile int phase;        // versioned, negative: queued, 1: locked
  int stackPred;             // pool stack (ctl) predecessor link
  int nsteals;               // number of steals
  Object[] array;   // the queued tasks; power of 2 size

  WorkQueue() {
    // Place indices in the center of array (that is not yet allocated)
    id |= FIFO;
    base = top = INITIAL_QUEUE_CAPACITY >>> 1;
  }

  /**
   * Tries to lock shared queue by CASing phase field. use with lockPush
   */
  final boolean tryLockPhase() {
    return PHASE.compareAndSet(this, 0, 1);
  }

  /**
   * use with lockPush
   *
   * @since 2021年10月14日 12:28:21
   */
  final void releasePhaseLock() {
    PHASE.setRelease(this, 0);
  }

  /**
   * Returns an exportable index (used by ForkJoinWorkerThread).
   */
  final int getPoolIndex() {
    return (id & 0xffff) >>> 1; // ignore odd/even tag bit
  }

  /**
   * Returns the approximate number of tasks in the queue.
   */
  final int queueSize() {
    int n = (int) BASE.getAcquire(this) - top;
    return (n >= 0) ? 0 : -n; // ignore transient negative
  }

  /**
   * Provides a more accurate estimate of whether this queue has any tasks than does queueSize, by
   * checking whether a near-empty queue has at least one unclaimed task.
   */
  public final boolean isEmpty() {
    Object[] a;
    int n, cap, b;
    VarHandle.acquireFence(); // needed by external callers
    return (n = (b = base) - top) >= 0 || // possibly one task
        n == -1 && ((a = array) == null ||
            (cap = a.length) == 0 ||
            a[cap - 1 & b] == null);
  }

  /**
   * Pushes a task. Call only by owner in unshared queues.
   *
   * @param task the task. Caller must ensure non-null.
   * @throws RejectedExecutionException if array cannot be resized
   */
  final void push(T task) {
    Object[] a;
    int s = top, d, cap, m;
    if ((a = array) != null && (cap = a.length) > 0) {
      QA.setRelease(a, (m = cap - 1) & s, task);
      top = s + 1;
      if (((d = s - (int) BASE.getAcquire(this)) & ~1) == 0) {                 // size 0 or 1
        VarHandle.fullFence();
      } else if (d == m) {
        growArray(false);
      }
    }
  }

  /**
   * Version of push for shared queues. Call only with phase lock held.
   *
   * @return true if should signal work
   */
  final boolean lockedPush(T task) {
    Object[] a;
    boolean signal = false;
    int s = top, b = base, cap, d;
    if ((a = array) != null && (cap = a.length) > 0) {
      a[cap - 1 & s] = task;
      top = s + 1;
      if (b - s + cap - 1 == 0) {
        growArray(true);
      } else {
        phase = 0; // full volatile unlock
        if ((s - base & ~1) == 0) // size 0 or 1
        {
          signal = true;
        }
      }
    }
    return signal;
  }

  /**
   * Doubles the capacity of array. Call either by owner or with lock held -- it is OK for base, but
   * not top, to move while resizings are in progress.
   */
  final void growArray(boolean locked) {
    Object[] newA = null;
    try {
      Object[] oldA;
      int oldSize, newSize;
      if ((oldA = array) != null && (oldSize = oldA.length) > 0 &&
          (newSize = oldSize << 1) <= MAXIMUM_QUEUE_CAPACITY &&
          newSize > 0) {
        try {
          newA = new Object[newSize];
        } catch (OutOfMemoryError ex) {
        }
        if (newA != null) { // poll from old array, push to new
          int oldMask = oldSize - 1, newMask = newSize - 1;
          for (int s = top - 1, k = oldMask; k >= 0; --k) {
            Object x = (ForkJoinTask<?>)
                QA.getAndSet(oldA, s & oldMask, null);
            if (x != null) {
              newA[s-- & newMask] = x;
            } else {
              break;
            }
          }
          array = newA;
          VarHandle.releaseFence();
        }
      }
    } finally {
      if (locked) {
        phase = 0;
      }
    }
    if (newA == null) {
      throw new RejectedExecutionException("Queue capacity exceeded");
    }
  }

  final void shrink(boolean locked) {
    Object[] newA = null;
    if (!tryLockPhase()) {
      return;
    }
    try {
      int oldSize, newSize;
      Object[] oldA = array;
      if (array != null &&
          (oldSize = oldA.length) > 0 &&
          INITIAL_QUEUE_CAPACITY <= (newSize = oldSize >>> 1) &&
          queueSize() <= oldSize / 4
      ) {
        try {
          newA = new Object[newSize];
        } catch (OutOfMemoryError ex) {
        }
        // poll from old array, push to new
        if (newA != null) {
          int oldMask = oldSize - 1, newMask = newSize - 1;
          for (int s = top - 1, k = oldMask; k >= 0; --k) {
            ForkJoinTask<?> x = (ForkJoinTask<?>) QA.getAndSet(oldA, s & oldMask, null);
            if (x != null) {
              newA[s-- & newMask] = x;
            } else {
              break;
            }
            array = newA;
            VarHandle.releaseFence();
          }
        }
      }
    } finally {
      phase = 0;
    }
  }

  /**
   * Takes next task, if one exists, in FIFO order.
   */
  final T poll() {
    int b, k, cap;
    Object[] a;
    while ((a = array) != null && (cap = a.length) > 0 &&
        top - (b = base) > 0) {
      Object t = QA.getAcquire(a, k = cap - 1 & b);
      if (base == b++) {
        if (t == null) {
          Thread.yield(); // await index advance
        } else if (QA.compareAndSet(a, k, t, null)) {
          BASE.setOpaque(this, b);
          return (T) t;
        }
      }
    }
    return null;
  }

  /**
   * Takes next task, if one exists, in order specified by mode.
   */
  final T nextLocalTask() {
    Object t = null;
    int md = id, b, s, d, cap;
    Object[] a;
    if ((a = array) != null && (cap = a.length) > 0 &&
        (d = (s = top) - (b = base)) > 0) {
      if ((md & FIFO) == 0 || d == 1) {
        if ((t = (ForkJoinTask<?>)
            QA.getAndSet(a, (cap - 1) & --s, null)) != null) {
          TOP.setOpaque(this, s);
        }
      } else if ((t = (ForkJoinTask<?>)
          QA.getAndSet(a, (cap - 1) & b++, null)) != null) {
        BASE.setOpaque(this, b);
      } else // on contention in FIFO mode, use regular poll
      {
        t = poll();
      }
    }
    return (T) t;
  }

  /**
   * Removes and cancels all known tasks, ignoring any exceptions.
   */
  final void cancelAll() {
    for (Object t; (t = poll()) != null; ) {
    }
  }

  /**
   * Initial capacity of work-stealing queue array. Must be a power of two, at least 2.
   */
  static final int INITIAL_QUEUE_CAPACITY = 1 << 13;

  /**
   * Maximum capacity for queue arrays. Must be a power of two less than or equal to 1 << (31 -
   * width of array entry) to ensure lack of wraparound of index calculations, but defined to a
   * value a bit less than this to help users trap runaway programs before saturating systems.
   */
  static final int MAXIMUM_QUEUE_CAPACITY = 1 << 26; // 64M

  static final int FIFO = 1 << 16;       // fifo queue or access mode

  // VarHandle mechanics.
  static final VarHandle PHASE;
  static final VarHandle BASE;
  static final VarHandle TOP;
  static final VarHandle QA;

  static {
    try {
      MethodHandles.Lookup l = MethodHandles.lookup();
      PHASE = l.findVarHandle(WorkQueue.class, "phase", int.class);
      BASE = l.findVarHandle(WorkQueue.class, "base", int.class);
      TOP = l.findVarHandle(WorkQueue.class, "top", int.class);
      QA = MethodHandles.arrayElementVarHandle(Object[].class);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }
}
