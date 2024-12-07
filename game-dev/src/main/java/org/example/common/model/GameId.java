package org.example.common.model;

import org.example.serde.Serde;
import org.example.util.Identity;

@Serde
public record GameId(String id) implements Identity, Comparable<GameId> {

  public static GameId gameId(String id) {
    return new GameId(id);
  }

  @Override
  public int compareTo(GameId o) {
    return id.compareTo(o.id);
  }
}
