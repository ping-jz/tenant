package org.example.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 父容器配置
 *
 * @author ZJP
 * @since 2021年06月30日 18:08:38
 **/
@Configuration
@ComponentScan(basePackages = "org/example/common")
public class CommonConfig {

}
