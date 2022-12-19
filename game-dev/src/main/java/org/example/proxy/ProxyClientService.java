package org.example.proxy;

import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import org.example.proxy.model.ServerRegister;
import org.example.proxy.util.NettyProxyMessageUtil;
import org.example.serde.CommonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中转服务客户端服务
 *
 * @author zhongjianping
 * @since 2022/12/14 23:06
 */
public class ProxyClientService {

  private Logger logger = LoggerFactory.getLogger(this.getClass());
  /** 代理客户端配置 */
  private ProxyClientConfig proxyClientConfig;
  /** 代理客户端 */
  private ProxyClient proxyClient;
  /** 序列化 */
  private CommonSerializer commonSerializer;


  public void register(ServerRegister register) {
    proxyClient.getChannel().writeAndFlush(
        NettyProxyMessageUtil.proxyMessage(proxyClientConfig.getId(), 0,
            commonSerializer.writeObject(register)));
  }

  public void connect(EventLoopGroup eventLoop, ChannelHandler channelHandler) {
    ProxyClient client = new ProxyClient();
    client.setAddress(proxyClientConfig.getAddress());
    client.setPort(proxyClientConfig.getPort());
    client.setHandler(channelHandler);

    boolean isSuc = client.connect(eventLoop).syncUninterruptibly()
        .isSuccess();
    if (isSuc) {
      proxyClient = client;
      logger.info("[Proxy Client]成功连接至【{}/{}】中转服", client.getAddress(), client.getPort());
    }
  }


  public boolean isActive() {
    return proxyClient != null && proxyClient.isActive();
  }

  public ProxyClientConfig getProxyClientConfig() {
    return proxyClientConfig;
  }

  public void setProxyClientConfig(ProxyClientConfig proxyClientConfig) {
    this.proxyClientConfig = proxyClientConfig;
  }

  public CommonSerializer getCommonSerializer() {
    return commonSerializer;
  }

  public void setCommonSerializer(CommonSerializer commonSerializer) {
    this.commonSerializer = commonSerializer;
  }
}
