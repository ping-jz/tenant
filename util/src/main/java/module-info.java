module org.example.util {
  exports org.example.util;
  exports org.example.serde;
  exports org.example.actor;
  requires io.netty.all;
  requires com.github.benmanes.caffeine;
  requires spring.data.mongodb;
}