package org.example.game.remote;

import org.example.common.event.ServerStartEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RemoteFacade {

  private RemoteService remoteService;

  public RemoteFacade(RemoteService remoteService) {
    this.remoteService = remoteService;
  }

  @EventListener
  public void serverStart(ServerStartEvent event) {
    remoteService.serverStart();
  }

}
