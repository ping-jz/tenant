package org.example.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import java.io.IOException;
import org.example.common.ThreadCommonResource;
import org.example.net.client.RpcClient;
import org.example.net.server.RpcServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicUsageTest {

  private Logger logger = LoggerFactory.getLogger(BasicUsageTest.class);

  private static ThreadCommonResource resource;

  private RpcServer rpcServer;
  private RpcClient rpcClient;
  private String address;


  @BeforeAll
  static void beforeAll() {
    resource = new ThreadCommonResource();
  }

  @AfterAll
  static void afterAll() {
    if (resource != null) {
      resource.close();
    }
  }

  @BeforeEach
  void start() throws Exception {
    rpcServer = new RpcServer();
    rpcServer.handler(new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel ch) throws Exception {
        logger.info("client:{}, connected", ch.remoteAddress());
      }
    });
    rpcServer.start(resource);

    address = rpcServer.ip() + ":" + rpcServer.port();

    rpcClient = new RpcClient();
    rpcClient.handler(new ChannelInitializer<>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {
        //DO nothing
      }
    });
    rpcClient.init(resource.getBoss());
  }

  @AfterEach
  void close() {
    if (rpcServer != null) {
      rpcServer.close();
    }

    if (rpcClient != null) {
      rpcClient.close();
    }
  }

  @Test
  public void testOneway() throws IOException {
    Connection connection = rpcClient.getConnection(address);
  }

  @Test
  public void testSync() {
    throw new UnsupportedOperationException("Make me passed");
  }

  @Test
  public void testFuture() {
    throw new UnsupportedOperationException("Make me passed");
  }

  @Test
  public void testCallback() {
    throw new UnsupportedOperationException("Make me passed");
  }


}
