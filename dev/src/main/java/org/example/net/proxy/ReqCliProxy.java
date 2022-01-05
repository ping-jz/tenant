package org.example.net.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.net.BaseRemoting;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.example.net.DefaultInvokeFuture;
import org.example.net.InvokeFuture;
import org.example.net.Message;
import org.example.net.MessageIdGenerator;
import org.example.net.ReqModule;
import org.example.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端RPC调用代理
 *
 * @author ZJP
 * @since 2021年07月25日 15:36:19
 **/
public class ReqCliProxy {

  private Logger logger = LoggerFactory.getLogger(getClass());

  /** 动态代理处理者 */
  private SendProxyInvoker methodProxy;
  /** 类型 -> [方法名, 方法信息] */
  private Map<Class<?>, Map<Method, ReqMetaMethodInfo>> rpcMethodInfos;
  /** 类型 -> Proxy对象 */
  private Map<Class<?>, Object> ivkCaches;
  /** 链接管理 (直接拿就行了，创建和管理链接，proxy不要管) */
  private ConnectionManager manager;
  /** 调用逻辑 */
  private BaseRemoting remoting;

  public ReqCliProxy(ConnectionManager manager) {
    ivkCaches = new ConcurrentHashMap<>();
    methodProxy = new SendProxyInvoker(this);
    rpcMethodInfos = new ConcurrentHashMap<>();
    remoting = new BaseRemoting();
    this.manager = manager;
  }

  /**
   * 构建远程方法调用信息
   *
   * @param inter rpc接口
   * @since 2021年07月25日 15:49:52
   */
  public static Map<Method, ReqMetaMethodInfo> registerRpcMethods(Class<?> inter) {
    ReqModule module = inter.getAnnotation(ReqModule.class);
    if (module == null) {
      throw new IllegalArgumentException(String.format("类型:%s, 缺少@RpcModule标记", inter));
    }

    if (!inter.isInterface()) {
      throw new IllegalArgumentException(String.format("类型:%s 不是接口", inter));
    }

    List<Pair<Integer, Method>> methods = ReqUtil.calcModuleMethods(inter);
    Map<Method, ReqMetaMethodInfo> infos = new HashMap<>();
    for (Pair<Integer, Method> pair : methods) {
      final int reqId = pair.first();
      final Method method = pair.second();
      ReqMetaMethodInfo info = new ReqMetaMethodInfo()
          .id(reqId).method(method);
      infos.put(method, info);

    }

    return infos;
  }


  /**
   * 根据类型和方法名，获取远程方法调用信息
   *
   * @param clazz 目标类型
   * @param method 方法
   * @since 2021年07月25日 15:49:52
   */
  public ReqMetaMethodInfo getRpcMetaMethodInfo(Class<?> clazz, Method method) {
    Map<Method, ReqMetaMethodInfo> infos = rpcMethodInfos
        .computeIfAbsent(clazz, ReqCliProxy::registerRpcMethods);
    return infos.get(method);
  }

  /**
   * 获取代理对象
   *
   * @param address ip地址
   * @param clz 代理接口
   * @since 2021年08月26日 16:29:36
   */
  @SuppressWarnings("unchecked")
  public <T> T getProxy(String address, Class<T> clz) {
    T proxy = (T) ivkCaches.get(clz);
    SendProxyInvoker invoker = resetProxy();
    if (proxy == null) {
      proxy = (T) Proxy.newProxyInstance(clz.getClassLoader(), new Class[]{clz}, invoker);
      ivkCaches.putIfAbsent(clz, proxy);
    }

    Connection connection = manager.connection(address);
    if (connection != null) {
      invoker.connections.set(Collections.singletonList(connection));
    } else {
      logger.error("{} 未链接", address);
    }

    return proxy;
  }

  private SendProxyInvoker resetProxy() {
    methodProxy.connections.set(Collections.emptyList());
    return methodProxy;
  }

  /**
   * 这个最后在测试吧
   *
   * @author ZJP
   * @since 2021年07月25日 14:23:21
   **/
  private static class SendProxyInvoker implements InvocationHandler {

    private ReqCliProxy rpcClientProxy;
    private ThreadLocal<List<Connection>> connections;

    public SendProxyInvoker(ReqCliProxy rpcClientProxy) {
      this.rpcClientProxy = rpcClientProxy;
      this.connections = ThreadLocal.withInitial(Collections::emptyList);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getDeclaringClass() == Object.class) {
        return method.invoke(this, args);
      } else {
        ReqMetaMethodInfo info = rpcClientProxy
            .getRpcMetaMethodInfo(method.getDeclaringClass(), method);
        if (info == null) {
          throw new RuntimeException(
              String.format("类型:%s 方法:%s，不是RPC方法", method.getDeclaringClass(),
                  method.getName()));
        }
        List<Connection> conns = connections.get();

        if (conns.isEmpty()) {
          rpcClientProxy.logger.error("[{}][{}],无链接", info.id(), method.getName());
          return defaultReturn(method);
        }

        boolean isCallBack = method.getReturnType() == InvokeFuture.class;
        if (isCallBack) {
          if (1 < conns.size()) {
            throw new RuntimeException(
                String.format("类型:%s 方法:%s，广播不能走回调", method.getDeclaringClass(),
                    method.getName()));
          }

          Message message = Message.of(info.id()).msgId(MessageIdGenerator.nextId()).packets(args);
          Connection connection = conns.get(0);

          DefaultInvokeFuture<?> res = new DefaultInvokeFuture<>(message.msgId());
          res.connection(connection);
          res.remoting(rpcClientProxy.remoting);
          res.reqMessage(message);
          return res;
        } else {
          Message message = Message.of(info.id()).packets(args);
          for (Connection connection : conns) {
            rpcClientProxy.remoting.invoke(connection, message);
          }
          return defaultReturn(method);
        }
      }
    }

    /**
     * 获取回调超时
     *
     * @param method 调用方法
     * @param info 方法信息
     * @since 2021年08月31日 12:12:31
     */
    private long getInvokeTimeOut(Method method, ReqMetaMethodInfo info) {
      //先写死
      return 3000L;
    }

    private Object defaultReturn(Method method) {
      return method.getReturnType().isPrimitive() ? 0 : null;
    }
  }

}
