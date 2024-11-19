package org.example.serde.processor;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeSpec.Builder;
import io.netty.buffer.ByteBuf;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import org.apache.commons.lang3.StringUtils;
import org.example.serde.CommonSerializer;
import org.example.serde.NettyByteBufUtil;
import org.example.serde.Serializer;

@SupportedAnnotationTypes("org.example.serde.Serde")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class SerdeProcessor extends AbstractProcessor {

  private static final String BUF_VAR_NAME = "buf";
  private static final String SERIALIZER_VAR_NAME = "serializer";
  private static final String OBJECT_VAR_NAME = "object";

  public SerdeProcessor() {
  }


  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      Set<? extends Element> annotationElements = roundEnv.getElementsAnnotatedWith(annotation);
      if (annotationElements.isEmpty()) {
        continue;
      }

      for (Element clz : annotationElements) {
        if (clz.getKind() != ElementKind.CLASS && clz.getKind() != ElementKind.RECORD) {
          processingEnv.getMessager()
              .printMessage(Kind.ERROR, "@Serde must be applied to a Class", clz);
          return false;
        }

        TypeElement clazz = (TypeElement) clz;

        try {
          ClassName typename = ClassName.get(clazz);
          ClassName serderTypeName = ClassName.get(typename.packageName(),
              typename.simpleName() + "Serde");

          TypeName genericInterface = ParameterizedTypeName.get(ClassName.get(Serializer.class),
              typename);
          Builder builder = TypeSpec.classBuilder(serderTypeName.simpleName())
              .addSuperinterface(genericInterface)
              .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

          switch (clazz.getKind()) {
            case CLASS -> {
              List<Element> fieldElements = BeanSerde.getAllFieldElements(this,
                  clazz);
              MethodSpec deSer = BeanSerde.deSerializerCode(typename, fieldElements);
              MethodSpec serde = BeanSerde.serializerCode(typename, fieldElements);
              TypeSpec typeSpec = consturctorAndFields(builder)
                  .addMethod(deSer)
                  .addMethod(serde)
                  .build();

              JavaFileObject builderFile = processingEnv.getFiler()
                  .createSourceFile(serderTypeName.canonicalName());
              try (PrintWriter writer = new PrintWriter(builderFile.openWriter())) {
                JavaFile.builder(typename.packageName(), typeSpec).build().writeTo(writer);
              }
            }
            case RECORD -> {
              List<Element> fieldElements = RecordSerde.getAllFieldElements((TypeElement) clazz);
              MethodSpec deSer = RecordSerde.deSerializerCode(typename, fieldElements);
              MethodSpec serde = RecordSerde.serializerCode(typename, fieldElements);
              TypeSpec typeSpec = consturctorAndFields(builder)
                  .addMethod(deSer)
                  .addMethod(serde)
                  .build();

              JavaFileObject builderFile = processingEnv.getFiler()
                  .createSourceFile(serderTypeName.canonicalName());
              try (PrintWriter writer = new PrintWriter(builderFile.openWriter())) {
                JavaFile.builder(typename.packageName(), typeSpec).build().writeTo(writer);
              }
            }
            default -> {
              processingEnv.getMessager()
                  .printMessage(Kind.ERROR, "@Serde must be applied to a Class", clazz);
              return false;
            }
          }


        } catch (Throwable e) {
          processingEnv.getMessager()
              .printError(
                  "[%s] %s build Serde error, %s".formatted(getClass(),
                      clazz,
                      Arrays.stream(
                              e.getStackTrace()).map(Objects::toString)
                          .collect(Collectors.joining("\n"))), clazz);
        }
      }
    }
    return false;
  }

  public static TypeSpec.Builder consturctorAndFields(TypeSpec.Builder builder) {
    MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(CommonSerializer.class, SERIALIZER_VAR_NAME)
        .addStatement("this.$N = $N", SERIALIZER_VAR_NAME, SERIALIZER_VAR_NAME)
        .build();

    FieldSpec fieldSpec = FieldSpec
        .builder(CommonSerializer.class, SERIALIZER_VAR_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build();

    return builder.addMethod(constructor).addField(fieldSpec);
  }


  /**
   * class代码生成
   *
   * @author zhongjianping
   * @since 2024/11/19 15:08
   */
  private static final class RecordSerde {

    public static List<Element> getAllFieldElements(TypeElement element) {
      return element.getEnclosedElements().stream()
          .filter(e -> e.getKind() == ElementKind.RECORD_COMPONENT)
          .collect(Collectors.toUnmodifiableList());
    }

    public static MethodSpec deSerializerCode(TypeName typeName, List<Element> fieldElements) {
      MethodSpec.Builder builder = MethodSpec.methodBuilder("readObject")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .returns(typeName)
          .addParameter(ByteBuf.class, BUF_VAR_NAME);

      builder.addCode("return new $T(\n", typeName);
      Iterator<Element> elementIterator = fieldElements.iterator();
      while (elementIterator.hasNext()) {
        Element e = elementIterator.next();
        switch (e.asType().getKind()) {
          case BOOLEAN -> builder.addCode("$L.readBoolean()", BUF_VAR_NAME);
          case BYTE -> builder.addCode("$L.readByte()", BUF_VAR_NAME);
          case SHORT -> builder.addCode("$L.readShort()", BUF_VAR_NAME);
          case CHAR -> builder.addCode("$L.readChar()", BUF_VAR_NAME);
          case FLOAT -> builder.addCode("$L.readFloat()", BUF_VAR_NAME);
          case DOUBLE -> builder.addCode("$T $L = $L.readDouble()", BUF_VAR_NAME);
          case INT -> builder.addCode("$T.readInt32($L)",
              NettyByteBufUtil.class,
              BUF_VAR_NAME);
          case LONG -> builder.addCode("$T.readInt64($L)",
              NettyByteBufUtil.class,
              BUF_VAR_NAME);
          default -> builder.addCode("$L.read($L)",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME
          );
        }

        if (elementIterator.hasNext()) {
          builder.addCode(",");
        }
        builder.addCode("\n");
      }
      builder.addCode(");");
      return builder.build();
    }

    public static MethodSpec serializerCode(TypeName typeName, List<Element> fieldElements) {
      MethodSpec.Builder builder = MethodSpec.methodBuilder("writeObject")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .addParameter(ByteBuf.class, BUF_VAR_NAME, Modifier.FINAL)
          .addParameter(typeName, OBJECT_VAR_NAME, Modifier.FINAL)
          .returns(TypeName.VOID);

      fieldElements.forEach(e -> {
        String fieldName = e.getSimpleName().toString();
        switch (e.asType().getKind()) {
          case BOOLEAN -> builder.addStatement("$L.writeBoolean($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case BYTE -> builder.addStatement("$L.writeByte($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case SHORT -> builder.addStatement("$L.writeShort($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case CHAR -> builder.addStatement("$L.writeChar($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case FLOAT -> builder.addStatement("$L.writeFloat($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case DOUBLE -> builder.addStatement("$L.writeDouble($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case INT -> builder.addStatement("$T.writeInt32($L, $L.$L())",
              NettyByteBufUtil.class,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case LONG -> builder.addStatement("$T.writeInt64($L, $L.$L())",
              NettyByteBufUtil.class,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          default -> builder.addStatement("$L.writeObject($L, $L.$L())",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
        }
      });

      return builder.build();
    }

  }


  /**
   * class代码生成
   *
   * @author zhongjianping
   * @since 2024/11/19 15:08
   */
  private static final class BeanSerde {

    private static TypeElement getSuperclass(SerdeProcessor processor, TypeElement type) {
      if (type.getSuperclass().getKind() == TypeKind.DECLARED) {
        TypeElement superclass = (TypeElement) processor.processingEnv.getTypeUtils()
            .asElement(type.getSuperclass());
        String name = superclass.getQualifiedName().toString();
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("android.")) {
          // Skip system classes, this just degrades performance
          return null;
        } else {
          return superclass;
        }
      } else {
        return null;
      }
    }


    public static List<Element> getAllFieldElements(SerdeProcessor processor, TypeElement element) {
      List<TypeElement> clazzs = new ArrayList<>();
      clazzs.add(element);

      TypeElement parent = element;
      while (true) {
        parent = getSuperclass(processor, parent);
        if (parent == null) {
          break;
        }

        clazzs.addFirst(parent);
      }

      List<Element> fields = new ArrayList<>();
      for (TypeElement typeElement : clazzs) {
        List<Element> fieldElements = typeElement.getEnclosedElements().stream()
            .filter(e -> e.getKind() == ElementKind.FIELD).filter(
                e -> !(e.getModifiers().contains(Modifier.FINAL) || e.getModifiers()
                    .contains(Modifier.STATIC) || e.getModifiers().contains(Modifier.TRANSIENT)))
            .collect(Collectors.toUnmodifiableList());

        fields.addAll(fieldElements);
      }

      return fields;
    }

    public static MethodSpec deSerializerCode(TypeName typeName, List<Element> fieldElements) {
      MethodSpec.Builder builder = MethodSpec.methodBuilder("readObject")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .addParameter(ByteBuf.class, BUF_VAR_NAME, Modifier.FINAL)
          .addStatement("$T $L = new $T()", typeName, OBJECT_VAR_NAME, typeName).returns(typeName);

      fieldElements.forEach(e -> {
        String fieldName = StringUtils.capitalize(e.getSimpleName().toString());
        switch (e.asType().getKind()) {
          case BOOLEAN -> builder.addStatement("$L.set$L($L.readBoolean())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case BYTE -> builder.addStatement("$L.set$L($L.readByte())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case SHORT -> builder.addStatement("$L.set$L($L.readShort())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case CHAR -> builder.addStatement("$L.set$L($L.readChar())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case FLOAT -> builder.addStatement("$L.set$L($L.readFloat())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case DOUBLE -> builder.addStatement("$L.set$L($L.readDouble())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case INT -> builder.addStatement("$L.set$L($T.readInt32($L))",
              OBJECT_VAR_NAME,
              fieldName,
              NettyByteBufUtil.class,
              BUF_VAR_NAME);
          case LONG -> builder.addStatement("$L.set$L($T.readInt64($L))",
              OBJECT_VAR_NAME,
              fieldName,
              NettyByteBufUtil.class,
              BUF_VAR_NAME);
          default -> builder.addStatement("$L.set$L($L.read(buf))",
              OBJECT_VAR_NAME,
              fieldName,
              SERIALIZER_VAR_NAME);
        }
      });

      builder.addStatement("return object");
      return builder.build();
    }

    public static MethodSpec serializerCode(TypeName typeName, List<Element> fieldElements) {
      MethodSpec.Builder builder = MethodSpec.methodBuilder("writeObject")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .addParameter(ByteBuf.class, BUF_VAR_NAME, Modifier.FINAL)
          .addParameter(typeName, OBJECT_VAR_NAME, Modifier.FINAL)
          .returns(TypeName.VOID);

      fieldElements.forEach(e -> {
        String fieldName = StringUtils.capitalize(e.getSimpleName().toString());
        switch (e.asType().getKind()) {
          case BOOLEAN -> builder.addStatement("$L.writeBoolean($L.is$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case BYTE -> builder.addStatement("$L.writeByte($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case SHORT -> builder.addStatement("$L.writeShort($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case CHAR -> builder.addStatement("$L.writeChar($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case FLOAT -> builder.addStatement("$L.writeFloat($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case DOUBLE -> builder.addStatement("$L.writeDouble($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case INT -> builder.addStatement("$T.writeInt32($L, $L.get$L())",
              NettyByteBufUtil.class,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case LONG -> builder.addStatement("$T.writeInt64($L, $L.get$L())",
              NettyByteBufUtil.class,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          default -> builder.addStatement("$L.writeObject($L, $L.get$L())",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
        }
      });

      return builder.build();
    }

  }


}