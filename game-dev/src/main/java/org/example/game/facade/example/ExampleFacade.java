package org.example.game.facade.example;

import java.util.concurrent.CompletableFuture;
import org.example.net.anno.Req;
import org.example.common.net.annotation.RpcModule;
import org.springframework.stereotype.Component;

@RpcModule
@Component
public class ExampleFacade {


  @Req
  public boolean b(boolean a) {
    return false;
  }

  @Req
  public byte bytes(byte a) {
    return 1;
  }

  @Req(207)
  public short s(short a) {
    return 1;
  }

  @Req(208)
  public char c(char a) {
    return 1;
  }

  @Req(209)
  public int i(int a) {
    return 1;
  }

  @Req(210)
  public long l(long a) {
    return 1;
  }

  @Req(211)
  public float f(float a) {
    return 1;
  }

  @Req(212)
  public double d(double a) {
    return 1;
  }

  @Req
  public CompletableFuture<Void> notice() {
    return CompletableFuture.completedFuture(null);
  }

}
