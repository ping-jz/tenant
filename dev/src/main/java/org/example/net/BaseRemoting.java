package org.example.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseRemoting {

  private Logger logger = LoggerFactory.getLogger(getClass());

  public void oneway(final Connection conn, final Message request) {
    try {
      conn.channel().writeAndFlush(request).addListener(f -> {
        if (!f.isSuccess()) {
          logger.error("Invoke send failed. The address is {}", conn.channel().remoteAddress(),
              f.cause());
        }
      });
    } catch (Exception e) {
      if (null == conn) {
        logger.error("Conn is null");
      } else {
        logger.error("Exception caught when sending invocation. The address is {}",
            conn.channel().remoteAddress(), e);
      }
    }
  }
}
