package org.example;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * 不要用，实验代码
 *
 * @author ZJP
 * @since 2021年07月12日 18:22:18
 **/
@Deprecated
public class CustomClassLoader extends URLClassLoader {

  private static String[] prefix = {"org.example.game", "org.example.config"};
  private String name;


  public CustomClassLoader(String name, List<URL> urls, ClassLoader parent) {
    super(urls.toArray(new URL[0]), parent);
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
    List<URL> classPathUrls = toURLClassPath(classPath);
    return new CustomClassLoader(name, classPathUrls, parent);
  }

  private static List<URL> toURLClassPath(String cp) {
    ArrayList<URL> path = new ArrayList<>();
    if (cp != null) {
      // map each element of class path to a file URL
      int off = 0, next;
      do {
        next = cp.indexOf(File.pathSeparator, off);
        String element = (next == -1)
            ? cp.substring(off)
            : cp.substring(off, next);
        if (element.length() > 0) {
          URL url = toFileURL(element);
          if (url != null) {
            path.add(url);
          }
        }
        off = next + 1;
      } while (next != -1);
    }

    return path;
  }

  private static URL toFileURL(String s) {
    try {
      File f = new File(s).getCanonicalFile();
      return ParseUtil.fileToEncodedURL(f);
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public URL getResource(String name) {
    return super.getResource(name);
  }
}