package org.example.common.model;

import org.example.util.Identity;

/**
 * 世界服链接信息
 *
 * @author zhongjianping
 * @since 2024/12/6 10:05
 */
public record ServerInfo(Identity id, int port) {

  public static ServerInfo serInfo(Identity id, int port) {
    return new ServerInfo(id, port);
  }

}
