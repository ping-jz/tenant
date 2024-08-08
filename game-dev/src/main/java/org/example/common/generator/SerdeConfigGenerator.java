package org.example.common.generator;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import javax.lang.model.element.Modifier;
import org.example.serde.CommonSerializer;
import org.example.serde.Serde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 协议注册配置生成
 *
 * @author zhongjianping
 * @since 2024/8/8 20:39
 */
public final class SerdeConfigGenerator {

  private Logger logger = LoggerFactory.getLogger(SerdeConfigGenerator.class);


  private SerdeConfigGenerator() {
  }

  public static void serdeConfig(Path outputDirs, ScanResult classGraph) {
    new SerdeConfigGenerator().run(outputDirs, classGraph);
  }

  public void run(Path outputDirs, ScanResult classGraph) {
    final String packaget = "org.example.common.config";

    TypeSpec.Builder typeSpecBuilder = TypeSpec
        .classBuilder("SerdeConfig")
        .addJavadoc("协议注册配置，自动。不能手动更改\n")
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
    methodBuilder.addStatement("$T serializer = new $T();", CommonSerializer.class, CommonSerializer.class);
    ClassInfoList list = classGraph.getClassesWithAnnotation(Serde.class);
    logger.info("协议数量：{}", list.size());
    for (ClassInfo info : classGraph.getClassesWithAnnotation(Serde.class)) {
      int protoId = Math.abs(info.getName().hashCode());
      ClassInfo serdeInfo = classGraph.getClassInfo(getSerdeName(info.getName()));
      if (serdeInfo == null) {
        throw new RuntimeException(
            String.format("[协议生成] 协议:%s,没有找到对应的编码和解码实现！！！！！！！！！！！！！！",
                info.getName()));
      }

      methodBuilder.addStatement("serializer.registerSerializer($L, $T.class, new $T(serializer))",
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
