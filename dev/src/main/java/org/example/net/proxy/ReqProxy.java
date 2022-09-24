package org.example.net.proxy;

/**
 * 代理类型包装类型，尽量做到像一个原生字段声明一样，如下图
 *
 * public class Example {
 *
 *   private Req<Service> serverReq;
 *
 *   public void test() {
 *       serverReq.helloWorld();
 *   }
 * }
 *
 * @author ZJP
 * @since 2022年08月22日 11:18:47
 **/
public class ReqProxy<T> {

  /** 请求代理类 */
  private ReqCliProxy cliProxy;
  /** 代理类型 */
  private Class<T> type;

  public ReqProxy(ReqCliProxy cliProxy, Class<T> type) {
    this.cliProxy = cliProxy;
    this.type = type;
  }

  public ReqCliProxy cliProxy() {
    return cliProxy;
  }

  public Class<T> type() {
    return type;
  }

  public T to(Integer serverId) {
    return cliProxy.getProxy(serverId, type);
  }

}
