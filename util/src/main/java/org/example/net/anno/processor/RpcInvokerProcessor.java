package org.example.net.anno.processor;

import static org.example.net.Util.BASE_REMOTING;
import static org.example.net.Util.BYTEBUF_UTIL;
import static org.example.net.Util.BYTE_BUF;
import static org.example.net.Util.COMMON_SERIALIZER;
import static org.example.net.Util.CONNECTION_CLASS_NAME;
import static org.example.net.Util.CONNECTION_GETTER;
import static org.example.net.Util.LOGGER;
import static org.example.net.Util.LOGGER_FACTOR;
import static org.example.net.Util.MESSAGE_CLASS_NAME;
import static org.example.net.Util.MSG_ID_VAR_NAME;
import static org.example.net.Util.POOLED_UTIL;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.netty.util.ReferenceCountUtil;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import org.example.net.Util;

/**
 * 负责RPC方法的调用类和代理类
 * <p>
 *
 * @author zhongjianping
 * @since 2024/8/9 11:19
 */
@SupportedAnnotationTypes("org.example.net.anno.Rpc")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class RpcInvokerProcessor extends AbstractProcessor {

  private static final String INVOKER_PACKAGE = "org.example.common.net.generated.invoker";

  private static final String INNER_SIMPLE_NAME = "Invoker";

  private static final String CONNECTION_FIELD_NAME = "c";
  private static final String BUF_VAR_NAME = "buf";
  private static final String MANAGER_VAR_NAME = "manager";


  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    for (TypeElement annotation : annotations) {
      Set<? extends Element> annotationElements = roundEnv.getElementsAnnotatedWith(annotation);
      if (annotationElements.isEmpty()) {
        continue;
      }
      for (Element clazz : annotationElements) {
        TypeElement typeElement = (TypeElement) clazz;
        try {
          List<ExecutableElement> elements = Util.getReqMethod(processingEnv, typeElement);
          generateOuter(typeElement, elements);
        } catch (Exception e) {
          processingEnv.getMessager()
              .printError(
                  "[%s] %s build invoker error, %s".formatted(getClass(),
                      typeElement.getQualifiedName(),
                      Arrays.stream(
                              e.getStackTrace()).map(Objects::toString)
                          .collect(Collectors.joining("\n"))), typeElement);
        }
      }
    }

    return false;
  }

  public void generateOuter(TypeElement typeElement, List<ExecutableElement> elements)
      throws Exception {
    TypeSpec inner = generateInner(typeElement, elements);

    String simpleName = typeElement.getSimpleName() + "Invoker";

    TypeSpec.Builder outerBuilder = TypeSpec.classBuilder(simpleName)
        .addJavadoc("{@link $T}\n", typeElement)
        .addJavadoc("@since $S", LocalDateTime.now())
        .addAnnotation(Util.COMPONENT_ANNOTATION)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addField(FieldSpec
            .builder(LOGGER, "logger")
            .addModifiers(Modifier.FINAL, Modifier.STATIC)
            .initializer("$T.getLogger($L.class)", LOGGER_FACTOR, simpleName)
            .build())
        .addField(FieldSpec
            .builder(CONNECTION_GETTER, MANAGER_VAR_NAME)
            .addModifiers(Modifier.PRIVATE)
            .build())
        .addField(FieldSpec
            .builder(BASE_REMOTING, "remoting")
            .addModifiers(Modifier.PRIVATE)
            .build())
        .addField(Util.COMMON_SERIALIZER_FIELD_SPEC);

    MethodSpec constructor = MethodSpec
        .constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(CONNECTION_GETTER, "manager")
        .addParameter(COMMON_SERIALIZER, "serializer")
        .addStatement("this.manager = manager")
        .addStatement("this.serializer = serializer")
        .addStatement("remoting = new $T()", BASE_REMOTING)
        .build();

    ClassName inner_type = ClassName.get(String.format("%s.%s", INVOKER_PACKAGE, simpleName),
        INNER_SIMPLE_NAME);

    MethodSpec ofId = MethodSpec.methodBuilder("of")
        .addModifiers(Modifier.PUBLIC)
        .returns(inner_type)
        .addParameter(Util.IDENTITY_CLASS_NAME, "id")
        .addStatement("$T connection = manager.connection(id)", CONNECTION_CLASS_NAME)
        .addStatement("return of(connection)").build();

    MethodSpec ofConnection = MethodSpec.methodBuilder("of")
        .addModifiers(Modifier.PUBLIC)
        .returns(inner_type)
        .addParameter(CONNECTION_CLASS_NAME, "c")
        .addStatement("$T.requireNonNull(c)", Objects.class)
        .beginControlFlow("if (!c.isActive())")
        .addStatement("logger.error(\"[RPC] $L, 因为链接【{}】失效：无法处理\", c.id());", simpleName)
        .endControlFlow()
        .addStatement("return new $L(c)", INNER_SIMPLE_NAME)
        .build();

    outerBuilder.addMethod(constructor).addMethod(ofId).addMethod(ofConnection);

    TypeSpec outer = outerBuilder.addType(inner).build();
    JavaFile javaFile = JavaFile.builder(INVOKER_PACKAGE, outer).build();

    String qualifiedName = "%s.%s".formatted(INVOKER_PACKAGE, outer.name());
    JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
    try (PrintWriter writer = new PrintWriter(file.openWriter())) {
      javaFile.writeTo(writer);
    }
  }

  public TypeSpec generateInner(TypeElement typeElement, List<ExecutableElement> methods) {
    final String protoIdVarName = "id_";

    TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(INNER_SIMPLE_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    typeBuilder.addField(FieldSpec
            .builder(CONNECTION_CLASS_NAME, CONNECTION_FIELD_NAME)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build())
        .addMethod(MethodSpec.constructorBuilder()
            .addParameter(CONNECTION_CLASS_NAME, CONNECTION_FIELD_NAME)
            .addStatement("this.$L = $L", CONNECTION_FIELD_NAME, CONNECTION_FIELD_NAME)
            .build());

    for (ExecutableElement method : methods) {
      Name methodName = method.getSimpleName();
      TypeMirror typeMirror = method.getReturnType();
      boolean callback = typeMirror.getKind() != TypeKind.VOID;

      MethodSpec.Builder methodBuilder = MethodSpec
          .methodBuilder(methodName.toString())
          .addModifiers(Modifier.PUBLIC);

      //Handle ID
      int id = Util.calcProtoId(typeElement, method);
      methodBuilder
          .addJavadoc("{@link $T#$L}", typeElement, methodName)
          .addStatement("final int $L = $L", protoIdVarName, id)
      ;

      methodBuilder
          .addCode("\n");
      List<? extends VariableElement> pparameters = method.getParameters();
      boolean noParam = pparameters.isEmpty() && !callback;
      if (noParam) {
        methodBuilder
            .addStatement("$T $L = $T.EMPTY_BUFFER", BYTE_BUF, BUF_VAR_NAME, Util.UNNPOOLED_UTIL);
      } else {

        methodBuilder
            .addStatement("$T $L = $T.DEFAULT.buffer()", BYTE_BUF, BUF_VAR_NAME, POOLED_UTIL);
        if (callback) {
          methodBuilder.addStatement("int $L = $L.nextCallBackMsgId()", MSG_ID_VAR_NAME,
              MANAGER_VAR_NAME);
        }

        methodBuilder.beginControlFlow("try");

        //Handle param
        if (callback) {
          methodBuilder.addStatement("$T.writeInt32($L, $L)", BYTEBUF_UTIL, BUF_VAR_NAME,
              MSG_ID_VAR_NAME);
        }
        for (VariableElement variableElement : method.getParameters()) {
          Name name = variableElement.getSimpleName();
          TypeMirror paramType = variableElement.asType();
          TypeName paramTypeName = TypeName.get(paramType);

          // Connection和Message不用生成
          if (paramTypeName.equals(CONNECTION_CLASS_NAME) || paramTypeName.equals(
              MESSAGE_CLASS_NAME)) {
            continue;
          }

          methodBuilder.addParameter(paramTypeName, name.toString());
          switch (paramType.getKind()) {
            case BOOLEAN -> methodBuilder.addStatement("$L.writeBoolean($L)", BUF_VAR_NAME, name);
            case BYTE -> methodBuilder.addStatement("$L.writeByte($L)", BUF_VAR_NAME, name);
            case SHORT -> methodBuilder.addStatement("$L.writeShort($L)", BUF_VAR_NAME, name);
            case CHAR -> methodBuilder.addStatement("$L.writeChar($L)", BUF_VAR_NAME, name);
            case FLOAT -> methodBuilder.addStatement("$L.writeFloat($L)", BUF_VAR_NAME, name);
            case DOUBLE -> methodBuilder.addStatement("$L.writeDouble($L)", BUF_VAR_NAME, name);
            case INT ->
                methodBuilder.addStatement("$T.writeInt32($L, $L)", BYTEBUF_UTIL, BUF_VAR_NAME,
                    name);
            case LONG ->
                methodBuilder.addStatement("$T.writeInt64($L, $L)", BYTEBUF_UTIL, BUF_VAR_NAME,
                    name);
            default -> methodBuilder.addStatement("serializer.writeObject(buf, $L)", name);
          }
        }

        methodBuilder
            .endControlFlow()
            .beginControlFlow("catch(Throwable t)")
            .addStatement("$T.release($L)", ReferenceCountUtil.class, BUF_VAR_NAME)
            .addStatement("throw t")
            .endControlFlow()
            .addCode("\n")
        ;
      }

      if (callback) {
        methodBuilder
            .returns(ParameterizedTypeName.get(Util.COMPLETE_ABLE_FUTURE_CLASS_NAME,
                TypeName.get(typeMirror).box()))
            .addStatement(
                "return remoting.invoke($L, $L, $T.of($L, $L), $L)",
                MANAGER_VAR_NAME,
                CONNECTION_FIELD_NAME,
                MESSAGE_CLASS_NAME,
                protoIdVarName,
                BUF_VAR_NAME,
                MSG_ID_VAR_NAME);

      } else {
        methodBuilder.addStatement(
            "remoting.invoke($L, $T.of($L, $L))",
            CONNECTION_FIELD_NAME,
            MESSAGE_CLASS_NAME,
            protoIdVarName,
            BUF_VAR_NAME);
      }

      typeBuilder.addMethod(methodBuilder.build());
    }

    return typeBuilder.build();
  }


}
