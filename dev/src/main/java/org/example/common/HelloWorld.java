package org.example.common;

import org.springframework.stereotype.Component;

/**
 * helloWorld服务，无状态。演示用
 *
 * @author ZJP
 * @since 2021年06月30日 18:08:21
 **/
@Component
public class HelloWorld {

  public String hello(String hi) {
    return "我是公共服务，有人要我说:[" + hi + "]";
  }
}
