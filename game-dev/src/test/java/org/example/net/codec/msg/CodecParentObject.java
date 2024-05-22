package org.example.net.codec.msg;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.example.serde.Serde;

public class CodecParentObject {

  private int type;

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }
}
