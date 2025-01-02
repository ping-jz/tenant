package org.example.common.generator;


import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.MethodSpec.Builder;
import com.palantir.javapoet.TypeSpec;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import javax.lang.model.element.Modifier;
import org.example.serde.CommonSerializer;
import org.example.serde.DefaultSerializersRegister;
import org.example.serde.Serde;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 协议注册配置生成
 *
 * @author zhongjianping
 * @since 2024/8/8 20:39
 */
public final class SerdeConfigGenerator {


  private SerdeConfigGenerator() {
  }

  public static void serdeConfig(Path outputDirs) {
    new SerdeConfigGenerator().run(outputDirs);
  }

  public void run(Path outputDirs) {
    final String projectPackage = "org.example.common";
    ScanResult classGraph = new ClassGraph()
        .enableAnnotationInfo()
        .enableClassInfo()
        .enableFieldInfo()
        .enableMethodInfo()
        .acceptPackages(projectPackage).scan();

    final String packaget = "org.example.common.config.generated";

    TypeSpec.Builder typeSpecBuilder = TypeSpec
        .classBuilder("SerdeConfig")
        .addJavadoc("协议注册配置，自动。不能手动更改\n")
        .addJavadoc("协议数量：$L\n", classGraph.getClassesWithAnnotation(Serde.class).size())
        .addJavadoc("@author zhongjianping\n")
        .addJavadoc("@since $S\n", LocalDateTime.now())
        .addAnnotation(Configuration.class);

    MethodSpec method = registerMethod(classGraph);

    TypeSpec typeSpec = typeSpecBuilder
        .addMethod(method)
        .build();
    JavaFile javaFile = JavaFile.builder(packaget, typeSpec).build();
    try {
      javaFile.writeTo(outputDirs);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private MethodSpec registerMethod(ScanResult classGraph) {
    Builder methodBuilder = MethodSpec
        .methodBuilder("commonSerializer")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Bean.class)
        .returns(CommonSerializer.class);
    methodBuilder.addStatement("$T serializer = new $T()", CommonSerializer.class,
        CommonSerializer.class);
    methodBuilder.addStatement("new $T().register(serializer)", DefaultSerializersRegister.class);
    for (ClassInfo info : classGraph.getClassesWithAnnotation(Serde.class)) {
      int protoId = Math.abs(info.getName().hashCode());
      ClassInfo serdeInfo = classGraph.getClassInfo(getSerdeName(info.getName()));
      if (serdeInfo == null) {
        throw new RuntimeException(
            String.format("[协议生成] 协议:%s,没有找到对应的编码和解码实现！！！！！！！！！！！！！！",
                info.getName()));
      }

      methodBuilder.addStatement("serializer.registerSerializer($L, $T.class, new $T())",
          protoId,
          ClassName.get(info.getPackageName(), info.getSimpleName()),
          ClassName.get(serdeInfo.getPackageName(), serdeInfo.getSimpleName()));

    }
    methodBuilder.addStatement("return serializer");
    return methodBuilder.build();
  }


  private String getSerdeName(String name) {
    return name + "Serde";
  }
}
