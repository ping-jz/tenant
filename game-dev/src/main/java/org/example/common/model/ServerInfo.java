package org.example.common.model;

import java.net.InetSocketAddress;
import org.example.util.Identity;

/**
 * 世界服链接信息
 *
 * @author zhongjianping
 * @since 2024/12/6 10:05
 */
public record ServerInfo(Identity id, InetSocketAddress addr) implements Identity {

  public static ServerInfo serInfo(Identity id, InetSocketAddress addr) {
    return new ServerInfo(id, addr);
  }

}
