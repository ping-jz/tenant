package org.example.serde.processor;

import com.google.auto.service.AutoService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
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
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import org.apache.commons.lang3.StringUtils;

@SupportedAnnotationTypes("org.example.serde.Serde")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class SerdeProcessor extends AbstractProcessor {


  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      Set<? extends Element> annotationElements = roundEnv.getElementsAnnotatedWith(annotation);
      if (annotationElements.isEmpty()) {
        continue;
      }

      for (Element clazz : annotationElements) {
        if (clazz.getKind() != ElementKind.CLASS) {
          processingEnv.getMessager()
              .printMessage(Kind.ERROR, "@BuilderProperty must be applied to a Class", clazz);
        }
        String className = clazz.toString();
        String packageName = null;
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
          packageName = className.substring(0, lastDot);
        }
        String simpleClassName = className.substring(lastDot + 1);
        String builderClassName = className + "Serde";
        String builderSimpleClassName = builderClassName.substring(lastDot + 1);

        try {
          JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(builderClassName);

          try (PrintWriter writer = new PrintWriter(builderFile.openWriter())) {
            if (packageName != null) {
              writer.println("package %s;".formatted(packageName));
            }
            writer.println("""
                  import io.netty.buffer.ByteBuf;
                  import org.example.serde.CommonSerializer;
                 \s
                  import org.example.serde.NettyByteBufUtil;
                  import org.example.serde.Serializer;
                 \s
                  public class %s implements Serializer<%s> {
                     private CommonSerializer serializer;
                                                \s
                     public %s(CommonSerializer serializer) {
                       this.serializer = serializer;
                     }
                \s""".formatted(builderSimpleClassName, simpleClassName, builderSimpleClassName));

            deSerializerCode(simpleClassName, clazz, writer);
            serializerCode(simpleClassName, clazz, writer);

            writer.println('}');
          }

        } catch (IOException e) {
          throw new RuntimeException(e);
        }

      }


    }
    return true;
  }

  private static void deSerializerCode(String simpleClassName, Element clazz, PrintWriter writer)
      throws IOException {
    String code = """
        @Override
        public %s readObject(ByteBuf buf) {
            //deSerializer code
         %s
        }
        """;
    StringBuilder builder = new StringBuilder();
    builder.append("%s object = new %s();\n".formatted(simpleClassName, simpleClassName));

    List<Element> fieldElements = clazz.getEnclosedElements().stream()
        .filter(e -> e.getKind() == ElementKind.FIELD).filter(
            e -> !(e.getModifiers().contains(Modifier.FINAL) || e.getModifiers()
                .contains(Modifier.STATIC))).collect(Collectors.toUnmodifiableList());

    fieldElements.forEach(e -> {
      String fieldName = StringUtils.capitalize(e.getSimpleName().toString());
      switch (e.asType().getKind()) {
        case BOOLEAN: {
          builder.append("  object.set%s(buf.readBoolean());\n".formatted(fieldName));
          break;
        }
        case BYTE: {
          builder.append("  object.set%s(buf.readByte());\n".formatted(fieldName));
          break;
        }

        case SHORT: {
          builder.append("  object.set%s(buf.readShort());\n".formatted(fieldName));
          break;
        }

        case INT: {
          builder.append("  object.set%s(NettyByteBufUtil.readInt32(buf));\n".formatted(fieldName));
          break;
        }
        case LONG: {
          builder.append("  object.set%s(NettyByteBufUtil.readInt64(buf));\n".formatted(fieldName));
          break;
        }

        case CHAR: {
          builder.append("  object.set%s(buf.readChar());\n".formatted(fieldName));
          break;
        }

        case FLOAT: {
          builder.append("  object.set%s(buf.readFloat());\n".formatted(fieldName));
          break;
        }
        case DOUBLE: {
          builder.append("  object.set%s(buf.readDouble());\n".formatted(fieldName));
          break;
        }
        default: {
          builder.append("  object.set%s(serializer.read(buf));\n".formatted(fieldName));
          break;
        }
      }
    });
    builder.append("  return object;");
    writer.println(code.formatted(simpleClassName, builder.toString()));

  }

  private static void serializerCode(String simpleClassName, Element clazz, PrintWriter writer)
      throws IOException {

    List<Element> fieldElements = clazz.getEnclosedElements().stream()
        .filter(e -> e.getKind() == ElementKind.FIELD).filter(
            e -> !(e.getModifiers().contains(Modifier.FINAL) || e.getModifiers()
                .contains(Modifier.STATIC))).collect(Collectors.toUnmodifiableList());

    StringBuilder builder = new StringBuilder();
    fieldElements.forEach(e -> {
      String fieldName = StringUtils.capitalize(e.getSimpleName().toString());
      switch (e.asType().getKind()) {
        case BOOLEAN: {
          builder.append("  buf.writeBoolean(object.is%s());\n".formatted(fieldName));
          break;
        }
        case BYTE: {
          builder.append("  buf.writeByte(object.get%s();\n".formatted(fieldName));
          break;
        }

        case SHORT: {
          builder.append("  buf.writeShort(object.get%s();\n".formatted(fieldName));
          break;
        }

        case INT: {
          builder.append("  NettyByteBufUtil.writeInt32(buf,object.get%s());\n".formatted(fieldName));
          break;
        }
        case LONG: {
          builder.append("  NettyByteBufUtil.writeInt64(buf,object.get%s());\n".formatted(fieldName));
          break;
        }

        case CHAR: {
          builder.append("  buf.writeChar(object.get%s();\n".formatted(fieldName));
          break;
        }

        case FLOAT: {
          builder.append("  buf.writeFloat(object.get%s();\n".formatted(fieldName));
          break;
        }
        case DOUBLE: {
          builder.append("  buf.writeDouble(object.get%s();\n".formatted(fieldName));
          break;
        }
        default: {
          builder.append("  serializer.writeObject(buf, object.get%s());\n".formatted(fieldName));
          break;
        }
      }
    });

    String code = """
             @Override
             public void writeObject(ByteBuf buf, %s object) {
                %s
             }
        """;
    writer.println(code.formatted(simpleClassName, builder.toString()));
  }


}