package org.example;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.lang.model.element.Modifier;

public class SourceCodeGenerator {

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: SourceCodeGenerator <output-directory>");
      return;
    }

    String outputDir = args[0];
    generateJavaSourceFile(outputDir);
  }

  private static void generateJavaSourceFile(String outputDir) {
    try {
      File sourceDirectory = new File(outputDir);
      sourceDirectory.mkdirs();
      TypeSpec typeSpec = TypeSpec.classBuilder("MyNewClass")
          .addMethod(MethodSpec.methodBuilder("main")
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC).returns(Void.TYPE)
              .addParameter(String[].class, "args")
              .addStatement("System.out.println(\"Hello, World!\")").build()).build();

      JavaFile.builder("org.example", typeSpec).build().writeToFile(sourceDirectory);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}