package org.example.net.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.net.anno.RpcModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * 代理适配管理
 *
 * @author ZJP
 * @since 2022年08月22日 16:39:21
 **/
@Component
public class ReqProxyManager implements InstantiationAwareBeanPostProcessor {

  private ReqCliProxy reqCliProxy;

  private Map<Class<?>, ReqProxy<?>> reqProxies = new ConcurrentHashMap<>();

  @Autowired
  public ReqProxyManager(ReqCliProxy reqCliProxy) {
    this.reqCliProxy = reqCliProxy;
  }

  @SuppressWarnings("unchecked")
  public  <T> ReqProxy<T> getReq(Class<T> clazz) {
    return (ReqProxy<T>) reqProxies.computeIfAbsent(clazz, k -> createReq(reqCliProxy, k));
  }

  private <T> ReqProxy<T> createReq(ReqCliProxy cliProxy, Class<T> clas) {
    return new ReqProxy<>(cliProxy, clas);
  }

  @Override
  public boolean postProcessAfterInstantiation(Object bean, String beanName) {
    ReflectionUtils.doWithFields(bean.getClass(), f -> {
      RpcModule rpcReq = f.getAnnotation(RpcModule.class);
      if (rpcReq == null) {
        return;
      }

      if (f.getType() != ReqProxy.class) {
        throw new RuntimeException(String.format("标记错误[%s].[%s]", beanName, f));
      }

      injectReq(bean, f);
    });
    return true;
  }

  private void injectReq(Object bean, Field field) {
    ReqProxy<?> req = createReq(reqCliProxy, getProxyType(field));

    field.setAccessible(true);
    try {
      field.set(bean, req);
    } catch (Exception e) {
      throw new RuntimeException(String.format("属性[%s]注入失败", field));
    }
  }


  private Class<?> getProxyType(Field field) {
    Type type = field.getGenericType();
    if (!(type instanceof ParameterizedType)) {
      throw new RuntimeException("类型不正确");
    }

    Type[] types = ((ParameterizedType) type).getActualTypeArguments();
    if (!(types[0] instanceof Class)) {
      throw new RuntimeException("类型不正确");
    }

    return (Class<?>) types[0];
  }


}
