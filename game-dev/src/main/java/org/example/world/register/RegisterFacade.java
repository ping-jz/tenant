package org.example.world.register;

import org.example.common.supplier.ConnExecutorSupplier;
import org.example.net.Connection;
import org.example.net.anno.Req;
import org.example.net.anno.Rpc;
import org.example.util.Identity;

@Rpc
public class RegisterFacade implements ConnExecutorSupplier {

  private RegisterService registerService;

  public RegisterFacade(RegisterService registerService) {
    this.registerService = registerService;
  }

  @Req
  public boolean serverRegister(Connection connection, Identity identity) {
    return registerService.serverRegister(connection, identity);
  }

}
