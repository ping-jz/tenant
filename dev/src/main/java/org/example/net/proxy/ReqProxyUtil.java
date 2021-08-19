package org.example.net.proxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.example.net.ReqMethod;
import org.example.net.ReqModule;
import org.example.util.Pair;

public class ReqProxyUtil {

  public static int METHOD_LIMIT_PER_MODULE = 100;

  /**
   * 计算请求协议ID,具体规则{@link ReqMethod}, {@link ReqModule}
   *
   * @param inter rpc模块接口
   * @since 2021年08月18日 17:26:59
   */
  public static List<Pair<Integer, Method>> calcReqMethods(Class<?> inter) {
    Set<Integer> ids = new HashSet<>();
    List<Pair<Integer, Method>> infos = new ArrayList<>();

    ReqModule module = inter.getAnnotation(ReqModule.class);
    if (module == null) {
      throw new RuntimeException(String.format("类型:%s, 缺少@RpcModule标记", inter));
    }

    Method[] methods = inter.getDeclaredMethods();
    if (methods.length <= 0) {
      return Collections.emptyList();
    }

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

      ids.add(reqId);
      infos.add(Pair.of(reqId, m));
    }

    infos.sort(Comparator.comparingInt(Pair::first));
    return infos;
  }
}
