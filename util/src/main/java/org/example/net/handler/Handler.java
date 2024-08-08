package org.example.net.handler;

import java.lang.invoke.MethodHandle;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.example.net.Connection;
import org.example.net.Message;

/**
 * 业务请求处理
 *
 * @author zhongjianping
 * @since 2024/8/8 14:44
 */
public interface Handler {

  byte[] invoke(Connection connection, Message message);
}
