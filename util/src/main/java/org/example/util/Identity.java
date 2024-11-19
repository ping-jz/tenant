package org.example.util;

/**
 * ID标记接口
 *
 * @author ZJP
 * @since 2021年09月29日 16:31:02
 **/
@FunctionalInterface
public interface Identity<T> extends Comparable<T> {


  @Override
  boolean equals(Object o);

  @Override
  int hashCode();

}
