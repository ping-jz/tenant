package org.example.common.generator.rpc;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import javax.lang.model.element.Modifier;
import org.example.net.DefaultDispatcher;
import org.example.net.handler.Handler;
import org.example.serde.CommonSerializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class GameRpcConfigGenerator {

  private static final String HANDLER_SIMPLE_NAME = "Handler";

  private GameRpcConfigGenerator() {
  }

  public static void rpcConfig(Path outputDirs) {
    new GameRpcConfigGenerator().run(outputDirs);
  }

  public void run(Path outputDirs) {

    //For Game
    generateCOnfig(outputDirs, "org.example.game.config.generated", "GameRpcConfig",
        new ClassGraph()
            .enableAnnotationInfo()
            .enableClassInfo()
            .enableFieldInfo()
            .enableMethodInfo()
            .acceptPackages("org.example.game").scan());

    //For World
    generateCOnfig(outputDirs, "org.example.world.config.generated", "WorldRpcConfig",
        new ClassGraph()
            .enableAnnotationInfo()
            .enableClassInfo()
            .enableFieldInfo()
            .enableMethodInfo()
            .acceptPackages("org.example.world").scan());
  }

  private static void generateCOnfig(Path outputDirs, String outPutPackage, String simpleName,
      ScanResult classGraph) {

    TypeSpec.Builder typeSpecBuilder = TypeSpec
        .classBuilder(simpleName)
        .addJavadoc("RPC注册配置\n")
        .addJavadoc("@author zhongjianping\n")
        .addJavadoc("@since $S\n", LocalDateTime.now())
        .addAnnotation(Configuration.class);

    String contextVarName = "c";
    String dispatcherVarName = "d";
    String serializerVarName = "s";
    String handlerRegisterMethodName = "handlerRegister";

    MethodSpec registerBean = MethodSpec.methodBuilder("defaultDispatcher")
        .addAnnotation(Bean.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(
            ParameterSpec.builder(AnnotationConfigApplicationContext.class, contextVarName).build())
        .returns(DefaultDispatcher.class)
        .addStatement("$T $L = new $T()", DefaultDispatcher.class, dispatcherVarName,
            DefaultDispatcher.class)
        .addStatement("$L($L, $L)", handlerRegisterMethodName, dispatcherVarName, contextVarName)
        .addStatement("return $L", dispatcherVarName)
        .build();

    MethodSpec.Builder handlerRegister = MethodSpec.methodBuilder(handlerRegisterMethodName)
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .addParameter(ParameterSpec.builder(DefaultDispatcher.class, dispatcherVarName).build())
        .addParameter(
            ParameterSpec.builder(AnnotationConfigApplicationContext.class, contextVarName).build())
        .addStatement("$T $L = $L.getBean($T.class)", CommonSerializer.class, serializerVarName,
            contextVarName, CommonSerializer.class);

    for (ClassInfo info : classGraph.getClassesImplementing(Handler.class)) {
      if (!info.getName().endsWith(HANDLER_SIMPLE_NAME)) {
        continue;
      }
      String facdeVarName = "f";
      String handlerVarName = "h";
      int handlerSubFix = info.getSimpleName().lastIndexOf(HANDLER_SIMPLE_NAME);
      ClassName handler = ClassName.get(info.getPackageName(), info.getSimpleName());
      ClassName facade = ClassName.get(info.getPackageName(),
          info.getSimpleName().substring(0, handlerSubFix));

      handlerRegister
          .beginControlFlow("")
          .addStatement("$T $L = $L.getBean($T.class)", facade, facdeVarName, contextVarName,
              facade)
          .addStatement("$T $L = new $T($L, $L)", handler, handlerVarName, handler, facdeVarName,
              serializerVarName)
          .beginControlFlow("for (int id : $T.protos)", handler)
          .addStatement("$L.registeHandler(id, $L)", dispatcherVarName, handlerVarName)
          .endControlFlow()
          .endControlFlow();
    }

    TypeSpec typeSpec = typeSpecBuilder
        .addMethod(registerBean)
        .addMethod(handlerRegister.build())
        .build();
    JavaFile javaFile = JavaFile.builder(outPutPackage, typeSpec).build();
    try {
      javaFile.writeTo(outputDirs);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
