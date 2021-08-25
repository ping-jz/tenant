package org.example.util;

/**
 * 键值对
 *
 * @author ZJP
 * @since 2021年08月18日 17:17:58
 **/
public class Pair<F, S> {

  private F first;
  private S second;

  public Pair(F first, S second) {
    this.first = first;
    this.second = second;
  }

  public static <F, S> Pair<F, S> of(F first, S second) {
    return new Pair<>(first, second);
  }

  public F first() {
    return first;
  }

  public S second() {
    return second;
  }
}
