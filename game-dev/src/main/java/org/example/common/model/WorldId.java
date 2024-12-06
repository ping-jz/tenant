package org.example.common.model;

import org.example.serde.Serde;
import org.example.util.Identity;

@Serde
public record WorldId(String id) implements Identity, Comparable<WorldId> {

  public static WorldId worldId(String id) {
    return new WorldId(id);
  }

  @Override
  public int compareTo(WorldId o) {
    return id.compareTo(o.id);
  }

  @Override
  public String toString() {
    return "WorldId[" + id + ']';
  }
}
