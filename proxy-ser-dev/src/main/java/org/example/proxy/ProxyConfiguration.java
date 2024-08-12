package org.example.proxy;

import org.example.net.DefaultDispatcher;
import org.example.proxy.model.ServerRegister;
import org.example.serde.CommonSerializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan
@PropertySource("classpath:/proxy.properties")
public class ProxyConfiguration {

  @Bean
  public CommonSerializer initSerializer() {
    CommonSerializer serializer = new CommonSerializer();
    serializer.registerObject(ServerRegister.class);
    return serializer;
  }

  @Bean
  public DefaultDispatcher dispatcher(AnnotationConfigApplicationContext context) {
//    DefaultDispatcher defaultDispatcher = new DefaultDispatcher(registry)
//    Collection<Object> objects = context.getBeansWithAnnotation(RpcModule.class).values();
//    objects.forEach(defaultDispatcher::registerHandlers);
//    return defaultDispatcher;

    throw new UnsupportedOperationException();
  }


}
