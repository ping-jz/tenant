package org.example.game.facade.example;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.example.common.model.ReqMove;
import org.example.common.model.ResMove;
import org.example.net.anno.Req;
import org.example.net.anno.RpcModule;

/**
 * 游戏门面(文档生产插件测试)
 *
 * @author ZJP
 * @since 2021年09月27日 15:54:54
 **/
@RpcModule
public class GameFacade {


  /**
   * 回声
   *
   * @author ZJP
   * @since 2021年09月27日 16:01:08
   **/
  @Req(200)
  public String echo(String str) {
    return str;
  }

  @Req(201)
  public void nothing() {
  }

  /**
   * 请求我
   *
   * @author ZJP
   * @since 2021年09月28日 12:16:00
   **/
  @Req(202)
  public String ok() {
    return "ok";
  }

  @Req
  public boolean b(boolean a) {
    return false;
  }

  @Req
  public byte b(byte a) {
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
  public int all(boolean boolean1, byte[] byte1, short short1, char char1, int int1, long long1,
      float float1, double double1, ReqMove reqMove, ResMove resMove) {
    return Objects.hash(boolean1, Arrays.hashCode(byte1), short1, char1, int1, long1, float1,
        double1, reqMove, resMove);
  }

  @Req
  public CompletableFuture<Integer> callback(boolean boolean1, byte[] byte1, short short1,
      char char1, int int1, long long1,
      float float1, double double1, ReqMove reqMove, ResMove resMove) {
    return CompletableFuture.completedFuture(
        Objects.hash(boolean1, Arrays.hashCode(byte1), short1, char1, int1, long1, float1,
            double1, reqMove, resMove));
  }
}
