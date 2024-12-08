package org.example.world.register;

import org.example.net.Connection;
import org.example.net.anno.Req;
import org.example.net.anno.Rpc;
import org.example.net.handler.FirstArgExecutorSupplier;
import org.example.util.Identity;

@Rpc
public class RegisterFacade implements FirstArgExecutorSupplier<Identity> {

  private RegisterService registerService;

  public RegisterFacade(RegisterService registerService) {
    this.registerService = registerService;
  }

  @Req
  public boolean serverRegister(Identity identity, Connection connection) {
    return registerService.serverRegister(identity, connection);
  }

}
