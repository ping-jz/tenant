package org.example.world.register;

import java.util.Objects;
import org.example.model.AnonymousId;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.example.util.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RegisterService {

  private static final Logger logger = LoggerFactory.getLogger(RegisterService.class);

  private ConnectionManager connectionManager;

  public RegisterService(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  public boolean serverRegister(Connection connection, Identity identity) {
    if (Objects.requireNonNull(connection.id()) instanceof AnonymousId) {
      connectionManager.reBindConnection(identity, connection.channel());
      logger.error("服务器：【{}】， 注册成功", identity);
      return true;
    } else {
      logger.error("服务器：【{}】， 重复注册", connection.id());
      return false;
    }
  }
}
