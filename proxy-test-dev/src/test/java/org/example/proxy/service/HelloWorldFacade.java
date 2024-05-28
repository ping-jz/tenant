package org.example.proxy.service;

import java.util.concurrent.atomic.AtomicInteger;
import org.example.net.anno.Req;
import org.example.net.anno.Resp;
import org.example.net.anno.RpcModule;

@RpcModule
public class HelloWorldFacade {

  public AtomicInteger integer = new AtomicInteger();

  public static final int echo = 1;

  @Req(echo)
  public Object echo(Object o) {
    integer.incrementAndGet();
    return o;
  }

  @Resp(echo)
  public void echoRes(Object o) {
    integer.incrementAndGet();
  }
}
