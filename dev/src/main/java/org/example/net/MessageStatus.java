package org.example.net;

public enum MessageStatus {

  SUCCESS((short) 200),
  ERROR((short) 500),
  UNKNOWN((short) 501),
  TIMEOUT((short) 502),
  SEND_ERROR((short) 503),
  SERVER_EXCEPTION((short) 504),
  CLOSE((short) 504),
  ;

  private short status;

  MessageStatus(short status) {
    this.status = status;
  }

  public short status() {
    return status;
  }
}
