//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.example.hotswap;

import com.sun.tools.attach.VirtualMachine;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Command {

  public Command() {
  }

  public static void main(String[] args) throws Exception {
    new Command().run(args[0], args[1]);
  }


  /**
   * 执行热更
   *
   * @param args 热更文件或者目录 /example 或者 /example/a.class
   * */
  public void run(String args) {
    String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    run(pid, args);
  }

  /**
   * 执行热更
   * @param pid 对应的jvm进程
   * @param args 热更文件或者目录 /example 或者 /example/a.class
   * */
  public void run(String pid, String args) throws RuntimeException {
    String jarPath = getJarPath();
    System.out.println("jarPath = " + jarPath);

    VirtualMachine virtualMachine = null;
    try {
      virtualMachine = VirtualMachine.attach(pid);
      virtualMachine.loadAgent(jarPath, args);
      System.out.println("ok");
    } catch (Exception var5) {
      throw new RuntimeException(var5);
    } finally {
      if (virtualMachine != null) {
        try {
          virtualMachine.detach();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public String getJarPath() {
    URL url = Agent.class.getProtectionDomain().getCodeSource().getLocation();
    String filePath = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
    File file = new File(filePath);
    filePath = file.getAbsolutePath();
    return filePath;
  }
}
