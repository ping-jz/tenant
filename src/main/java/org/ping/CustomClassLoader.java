package org.ping;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 不要用，实验代码
 *
 * @author ZJP
 * @since  2021年07月12日 18:22:18
 **/
@Deprecated
public class CustomClassLoader extends URLClassLoader {

  private static String[] prefix = {"org.ping.game", "org.ping.config"};
  private String name;


  public CustomClassLoader(String name, URL[] urls, ClassLoader parent) {
    super(urls, parent);
    this.name = name;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    for (String pre : prefix) {
      if (name.startsWith(pre)) {
        Class<?> clz = findLoadedClass(name);
        if (clz != null) {
          return clz;
        }

        return findClass(name);
      }
    }

    return super.loadClass(name);
  }

  public static ClassLoader of(final String name, final ClassLoader parent) throws Exception {
    final String classPath = System.getProperty("java.class.path");
    final File[] classPaths = classPath == null ? new File[0] : getClassPath(classPath);
    URL[] classPathUrls = classPath == null ? new URL[0] : pathToURLs(classPaths);
    return new CustomClassLoader(name, classPathUrls, parent);
  }

  private static File[] getClassPath(String var0) {
    File[] var1;
    if (var0 != null) {
      int var2 = 0;
      int var3 = 1;
      boolean var4 = false;

      int var5;
      int var7;
      for (var5 = 0; (var7 = var0.indexOf(File.pathSeparator, var5)) != -1; var5 = var7 + 1) {
        ++var3;
      }

      var1 = new File[var3];
      var4 = false;

      for (var5 = 0; (var7 = var0.indexOf(File.pathSeparator, var5)) != -1; var5 = var7 + 1) {
        if (var7 - var5 > 0) {
          var1[var2++] = new File(var0.substring(var5, var7));
        } else {
          var1[var2++] = new File(".");
        }
      }

      if (var5 < var0.length()) {
        var1[var2++] = new File(var0.substring(var5));
      } else {
        var1[var2++] = new File(".");
      }

      if (var2 != var3) {
        File[] var6 = new File[var2];
        System.arraycopy(var1, 0, var6, 0, var2);
        var1 = var6;
      }
    } else {
      var1 = new File[0];
    }

    return var1;
  }

  private static URL[] pathToURLs(File[] files) throws Exception {
    URL[] urls = new URL[files.length];

    for (int i = 0; i < files.length; ++i) {
      urls[i] = files[i].toURI().toURL();
    }

    return urls;
  }


}