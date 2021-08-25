package org.example.net;

public enum MessageStatus {

  SUCCESS((short) 0x0200),
  ERROR((short) 0x0201),
  UNKNOWN((short) 0x0203),
  TIMEOUT((short) 0x0204),
  SEND_ERROR((short) 0x0205),
  SERVER_EXCEPTION((short) 0x0206),
  CLOSE((short) 0x0207),
  ;

  private short status;

  MessageStatus(short status) {
    this.status = status;
  }

  public short status() {
    return status;
  }
}
