//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.example.hotswap;

import java.io.DataInputStream;
import java.io.File;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 热更实现类
 *
 * */
public final class Agent {
  private static Instrumentation instance;

  private static Map<String, Long> lastModifyTime = new HashMap<>();
  public Agent() {
  }

  public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
    System.out.println("Agent Main called");
    System.out.println("agentArgs : " + agentArgs);
    instance = inst;

    DynamicClassLoader myLoader = new DynamicClassLoader();
    try {
      List<ClassDefinition> list = new ArrayList<>();
      //使用分号切割不同文件
      String[] howSwapFiles = agentArgs.split(";");

      Map<String, Long> updateFile = new HashMap<>();
      //列出所有文件，如果是文件则递归遍历
      for(String path : howSwapFiles) {
         listFiles(new File(path), updateFile);
      }
      List<String> updateFiles = new ArrayList<>();

      for(Entry<String, Long> e : updateFile.entrySet()) {
        //如果修改时间发生变化则进行热更
        long last = lastModifyTime.getOrDefault(e.getKey(), 0L);
        if(e.getValue() != last) {
          updateFiles.add(e.getKey());
          lastModifyTime.put(e.getKey(), e.getValue());
        }
      }

      //获取需要热更的文件数据
      for (String fileUrl : updateFiles) {
        byte[] targetClassFile = getUrlFileData(fileUrl);
        Class<?> targetClazz = myLoader.findClass(targetClassFile);
        ClassDefinition reporterDef = new ClassDefinition(Class.forName(targetClazz.getName()),
            targetClassFile);
        list.add(reporterDef);
      }

      //进行热更
      inst.redefineClasses(list.toArray(new ClassDefinition[0]));
      for(String file : updateFiles) {
        System.out.println("热更:" + file + "成功");
      }
    } catch (Exception var11) {
      System.out.format("Agent fail! %s", var11);
    }
  }


  /**
   * 找出所有class文件的最后修改时间
   * */
  private static void listFiles(File file, Map<String, Long> lastModifyTimes) {
    if (file.isFile() && file.getName().endsWith(".class")) {
      //加上文件协议
      lastModifyTimes.put("file:///" + file.getAbsolutePath(), file.lastModified());
    } else if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (File f : files) {
          listFiles(f, lastModifyTimes);
        }
      }
    }
  }

  public static byte[] getUrlFileData(String fileUrl) throws Exception {
    URL url = new URL(fileUrl);
    URLConnection conn = url.openConnection();
    byte[] reporterClassFile = new byte[conn.getContentLength()];
    DataInputStream in = new DataInputStream(conn.getInputStream());
    in.readFully(reporterClassFile);
    in.close();
    return reporterClassFile;
  }

  public static void premain(String agentArgs, Instrumentation inst) {
    System.out.println("premain Main called");
    System.out.println("agentArgs : " + agentArgs);
    instance = inst;
  }

  public static Instrumentation getInstrumentation() {
    return instance;
  }
}
