package org.example;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import org.example.common.generator.SerdeConfigGenerator;

public final class SourceCodeGenerator {

  /**
   * @return 所有代码生成器，因为会并发执行所以写逻辑请不要相互依赖
   * @since 2024/8/8 20:42
   */
  private static List<BiConsumer<Path, ScanResult>> codeGenerators() {
    return List.of(SerdeConfigGenerator::serdeConfig);
  }

  /**
   * @param outputDir  所有生成的代码，请放在这个目录
   * @param classGraph 本项目所有类的信息
   * @since 2024/8/8 20:37
   */
  private static void generateJavaSourceFile(Path outputDir, ScanResult classGraph) {
    codeGenerators().parallelStream().forEach(c -> c.accept(outputDir, classGraph));
  }


  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: SourceCodeGenerator <output-directory>");
      return;
    }

    String outputDir = args[0];
    final String projectPackage = "org.example";
    ScanResult classGraph = new ClassGraph()
        .enableAnnotationInfo()
        .enableClassInfo()
        .enableFieldInfo()
        .enableMethodInfo()
        .acceptPackages(projectPackage).scan();

    generateJavaSourceFile(Path.of(outputDir), classGraph);
  }


}