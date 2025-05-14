package org.example.common;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.example.common.generator.rpc.GameRpcConfigGenerator;

public final class SourceCodeGenerator {

  /**
   * @return 所有代码生成器，因为会并发执行所以写逻辑请不要相互依赖
   * @since 2024/8/8 20:42
   */
  private static List<Consumer<Path>> codeGenerators() {
    return List.of(GameRpcConfigGenerator::rpcConfig);
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: SourceCodeGenerator <output-directory>");
      return;
    }

    Path outputDir = Path.of(args[0]);
    for (Consumer<Path> consumer : codeGenerators()) {
      consumer.accept(outputDir);
    }
  }


}