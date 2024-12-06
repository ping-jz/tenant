package org.example.game.avatar;

import org.example.util.Identity;

public record IntId(int id) implements Identity {

  public static IntId intId(int id) {
    return new IntId(id);
  }
}
