package org.example.game;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;

/**
 * 子容器配置类
 *
 * @author ZJP
 * @since 2021年06月30日 18:09:27
 **/
@Component
@ComponentScan({"org.example.game", "org.example.common"})
@ImportResource
public class GameConfig {

}
