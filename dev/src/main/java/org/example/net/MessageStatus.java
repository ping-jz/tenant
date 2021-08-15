package org.example.net;

public enum MessageStatus {

  ERROR((short) 0x0000),
  SUCCESS((short) 0x0001),
  UNKOWN((short) 0x0003),
  TIMEOUT((short) 0x0004),
  SEND_ERROR((short) 0x0005),
  SERVER_EXCEPTION((short) 0x0006),
  ;

  private short status;

  MessageStatus(short status) {
    this.status = status;
  }

  public short status() {
    return status;
  }
}
