package org.example.net.proxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.example.net.Facade;
import org.example.net.ReqMethod;
import org.example.net.ReqModule;
import org.example.util.Pair;

public class ReqUtil {

  public static int METHOD_LIMIT_PER_MODULE = 100;

  /**
   * 计算请求协议ID,具体规则{@link ReqMethod}, {@link ReqModule}
   *
   * @param inter rpc模块接口
   * @since 2021年08月18日 17:26:59
   */
  public static List<Pair<Integer, Method>> calcModuleMethods(Class<?> inter) {
    ReqModule module = inter.getAnnotation(ReqModule.class);
    if (module == null) {
      return Collections.emptyList();
    }

    Method[] methods = inter.getDeclaredMethods();
    if (methods.length <= 0) {
      return Collections.emptyList();
    }

    Set<Integer> ids = new HashSet<>(methods.length);
    List<Pair<Integer, Method>> infos = new ArrayList<>(methods.length);

    int start = module.value();
    int end = start + METHOD_LIMIT_PER_MODULE;
    Arrays.sort(methods, Comparator.comparing(Method::getName));
    for (int i = 0; i < methods.length; i++) {
      Method m = methods[i];
      ReqMethod method = m.getAnnotation(ReqMethod.class);
      if (method == null) {
        continue;
      }
      methods[i] = null;

      final int reqId = method.value();
      int absId = Math.abs(reqId);
      if (ids.contains(reqId)
          || absId < start
          || end <= absId) {
        throw new RuntimeException(
            String.format("类型:%s 方法:%s, 协议号:%s, 协议错误", inter.getName(), m.getName(), reqId));
      }

      m.setAccessible(true);
      ids.add(reqId);
      infos.add(Pair.of(reqId, m));
    }

    //没标记的都按方法名，排序分配
    for (int i = 0, mi = 0; mi < methods.length && i < METHOD_LIMIT_PER_MODULE; i++) {
      if (ids.contains(i)) {
        continue;
      }

      Method m = methods[mi++];
      if (m == null) {
        continue;
      }

      final int reqId = module.value() + i;
      int absId = Math.abs(reqId);
      if (absId < start || end <= absId) {
        throw new RuntimeException(
            String.format("类型:%s 方法:%s, 协议号:%s, 协议错误", inter.getName(), m.getName(), reqId));
      }

      m.setAccessible(true);
      ids.add(reqId);
      infos.add(Pair.of(reqId, m));
    }

    infos.sort(Comparator.comparingInt(Pair::first));
    return infos;
  }

  public static List<Pair<Integer, Method>> calcFacadeMethods(Class<?> clazz) {
    Facade facade = clazz.getAnnotation(Facade.class);
    if (facade == null) {
      return Collections.emptyList();
    }

    Method[] methods = clazz.getDeclaredMethods();
    if (methods.length <= 0) {
      return Collections.emptyList();
    }

    Set<Integer> ids = new HashSet<>(methods.length);
    List<Pair<Integer, Method>> infos = new ArrayList<>(methods.length);

    Arrays.sort(methods, Comparator.comparing(Method::getName));
    for (Method m : methods) {
      ReqMethod method = m.getAnnotation(ReqMethod.class);
      if (method == null) {
        continue;
      }

      final int reqId = method.value();

      if (ids.add(reqId)) {
        m.setAccessible(true);
        infos.add(Pair.of(reqId, m));
      } else {
        throw new IllegalArgumentException(
            String.format("facade:%s has duplicated reqId:%s", clazz, reqId));
      }
    }

    infos.sort(Comparator.comparingInt(Pair::first));
    return infos;
  }
}
