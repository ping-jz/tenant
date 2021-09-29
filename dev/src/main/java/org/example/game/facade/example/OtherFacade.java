package org.example.game.facade.example;

import org.example.game.facade.example.model.ProtoIds;
import org.example.net.Facade;
import org.example.net.ReqMethod;

@Facade
public class OtherFacade {

  public static final int MODULE_ONE = 300;

  @ReqMethod(MODULE_ONE)
  public void otherFunction() {

  }

  @ReqMethod(ProtoIds.MODULE_ONE)
  public void anotherFunction() {

  }
}
