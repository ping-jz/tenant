package org.example.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * logger环境配置，每个容器独有 还是通过配置文件来吧，全部依赖log4j2的代码。少自己创建新概念新流程
 *
 * @author ZJP
 * @since 2021年06月30日 18:09:39
 **/
@Deprecated
public class LoggerConfig {


  @Bean(value = "game_log_context", destroyMethod = "close")
  public LoggerContext configDataSource(Environment env) {
    return createLoggerContext(env);
  }

  /**
   * 通过代码的方式来配置logger,具体语法规则而看log4j2官网
   *
   * TODO 是否可以通过配置文件，然后程序注入变量来实现初始化
   *
   * @param env 环境变量
   * @link https://logging.apache.org/log4j/2.x/manual/customconfig.html
   * @since 2021年07月13日 16:53:40
   */
  private static LoggerContext createLoggerContext(Environment env) {
    ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory
        .newConfigurationBuilder();

    String gameId = env.getRequiredProperty("game.id");

    builder.setStatusLevel(Level.INFO);
    builder.setConfigurationName(gameId);

    // 控制台输出
    Level baseLevel = Level.getLevel(env.getProperty("org.example.game.console.log.level", "INFO"));
    LayoutComponentBuilder fileLayout = builder.newLayout("PatternLayout")
        .addAttribute("pattern", new ParameterizedMessage("%d [%-p] [{}] [%t] %c{1} - %m%n", gameId)
            .getFormattedMessage());
    AppenderComponentBuilder consoleAppender =
        builder.newAppender("console", "CONSOLE")
            .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)
            .add(fileLayout)
            .addComponent(createThresholdFilter(builder, baseLevel));
    builder.add(consoleAppender);

    // 创建输出者
    final String errorAppender = "errorAppender";
    final String infoAppender = "infoAppender";
    createRollingFileAppender(builder, errorAppender, gameId, "error", Level.ERROR);
    createRollingFileAppender(builder, infoAppender, gameId, "info", Level.INFO);

    //创建默认根logger或者默认logger
    builder.add(builder.newRootLogger(baseLevel)
        .add(builder.newAppenderRef(errorAppender))
        .add(builder.newAppenderRef(infoAppender))
        .add(builder.newAppenderRef("console")));

    LoggerContext context = new LoggerContext(gameId, null);
    context.start(builder.build());
    return context;
  }

  private static void createRollingFileAppender(ConfigurationBuilder<BuiltConfiguration> builder,
      String appenderName, String gameId, String fileName, Level level) {

    //消息模板
    LayoutComponentBuilder fileLayout = builder.newLayout("PatternLayout")
        .addAttribute("pattern", "%d [%-p] [%t] %c{1} - %m%n");

    //滚动模式
    ComponentBuilder<?> triggeringPolicy = builder.newComponent("Policies")
        .addComponent(
            builder.newComponent("TimeBasedTriggeringPolicy").addAttribute("interval", "1"))
        .addComponent(
            builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "100M"));

    // 输出者
    AppenderComponentBuilder rollingFile = builder.newAppender(appenderName, "RollingFile")
        .addAttribute("fileName", new ParameterizedMessage("./logs/{}/{}.log", gameId, fileName)
            .getFormattedMessage())
        .addAttribute("filePattern",
            new ParameterizedMessage("./logs/{}/%d{yyyy-MM-dd}/{}_%d{yyyy-MM-dd}.log.gz", gameId,
                fileName)
                .getFormattedMessage())
        .add(fileLayout)
        .addComponent(triggeringPolicy)
        .addComponent(createThresholdFilter(builder, level));

    builder.add(rollingFile);
  }

  // 创建过滤器
  private static ComponentBuilder<?> createThresholdFilter(
      ConfigurationBuilder<BuiltConfiguration> builder, Level level) {
    return builder
        .newFilter("ThresholdFilter", Filter.Result.ACCEPT, Result.DENY)
        .addAttribute("level", level);
  }

}
