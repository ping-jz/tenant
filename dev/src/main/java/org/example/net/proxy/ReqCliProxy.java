package org.example.net.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.example.net.Message;
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

  /** 每个某块可以注册的方法 */
  private static int METHOD_LIMIT = 100;

  /** 动态代理处理者 */
  private InvocationHandler methodProxy;
  /** 类型 -> [方法名, 方法信息] */
  private Map<Class<?>, Map<Method, ReqMetaMethodInfo>> rpcMethodInfos;
  /** 类型 -> Proxy对象 */
  private Map<Class<?>, Object> ivkCaches;
  /** 链接管理 */
  private ConnectionManager manager;

  public ReqCliProxy(ConnectionManager manager) {
    ivkCaches = new ConcurrentHashMap<>();
    methodProxy = new SendProxyInvoker(this);
    rpcMethodInfos = new ConcurrentHashMap<>();
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

  @SuppressWarnings("unchecked")
  public <T> T getProxy(Class<T> clz) {
    T proxy = (T) ivkCaches.get(clz);
    if (proxy != null) {
      return proxy;
    } else {
      T insta = (T) Proxy.newProxyInstance(clz.getClassLoader(), new Class[]{clz}, methodProxy);
      ivkCaches.putIfAbsent(clz, insta);
      return insta;
    }
  }


  /**
   * 这个最后在测试吧
   *
   * @author ZJP
   * @since 2021年07月25日 14:23:21
   **/
  private static class SendProxyInvoker implements InvocationHandler {

    private ReqCliProxy rpcClientProxy;
    private ThreadLocal<Iterable<Connection>> serverIds;

    public SendProxyInvoker(ReqCliProxy rpcClientProxy) {
      this.rpcClientProxy = rpcClientProxy;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getDeclaringClass() == Object.class) {
        return method.invoke(proxy, args);
      } else {
        ReqMetaMethodInfo info = rpcClientProxy
            .getRpcMetaMethodInfo(method.getDeclaringClass(), method);
        if (info == null) {
          throw new RuntimeException(
              String.format("类型:%s 方法:%s，不是RPC方法", method.getDeclaringClass(),
                  method.getName()));
        }

        Message message = Message.of(info.id()).packet(args);

        for (Connection connection : serverIds.get()) {
          connection.channel().writeAndFlush(message);
        }

        return defaultReturn(method);
      }
    }

    private Object defaultReturn(Method method) {
      return method.getReturnType().isPrimitive() ? 0 : null;
    }
  }

}
