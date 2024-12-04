package org.example.common.model;

import org.example.serde.Serde;
import org.example.util.Identity;

@Serde
public record AvatarId(long id) implements Identity {


}
