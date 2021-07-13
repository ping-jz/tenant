package org.ping.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;

/**
 * 子容器配置类
 *
 * @author ZJP
 * @since 2021年06月30日 18:09:27
 **/
@Component
@ComponentScan(basePackages = "org/ping/game")
@Import(LoggerConfig.class)
@ImportResource
public class GameConfig {

}
