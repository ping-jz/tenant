package org.example.common.config;

import java.util.ServiceLoader;
import org.example.serde.DefaultSerializersRegister;
import org.example.serde.SerdeRegister;
import org.example.serde.Serdes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 序列化整合配置
 *
 * @author zhongjianping
 * @since 2025/5/14 13:52
 */
@Configuration
public class SerdeConfig {

  @Bean
  public Serdes commonSerializer() {
    Serdes serializer = new Serdes();
    new DefaultSerializersRegister().register(serializer);
    for (SerdeRegister register : ServiceLoader.load(SerdeRegister.class)) {
      register.register(serializer);
    }
    return serializer;
  }
}
