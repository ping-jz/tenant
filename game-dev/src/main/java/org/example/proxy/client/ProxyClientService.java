package org.example.proxy.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.util.ReferenceCountUtil;
import org.example.net.Message;
import org.example.net.util.ProxyMessageUtil;
import org.example.proxy.message.ProxyProtoId;
import org.example.proxy.model.ServerRegister;
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
    ByteBuf msg = ProxyMessageUtil.proxyMessage(proxyClientConfig.getId(),
        proxyClientConfig.proxyId(),
        ProxyProtoId.REGISTER, 0, commonSerializer.writeObject(register));
    proxyClient.getChannel().writeAndFlush(msg);
  }

  public void send(int target, Message msg) {
    ByteBuf byteBuf = ProxyMessageUtil.proxyMessage(proxyClientConfig.getId(), target,
        msg.proto(), msg.msgId(), commonSerializer.writeObject(msg.packet()));
    proxyClient.getChannel().writeAndFlush(byteBuf);
  }

  public void send(Iterable<Integer> targets, Message msg) {
    ByteBuf buf = null;
    try {
      buf = commonSerializer.writeObject(msg.packet());
      int source = proxyClientConfig.getId();
      for (Integer target : targets) {
        ByteBuf byteBuf = ProxyMessageUtil.proxyMessage(source, target, msg.proto(),
            msg.msgId(), buf.retainedSlice());
        proxyClient.getChannel().write(byteBuf);
      }
      proxyClient.getChannel().flush();
    } finally {
      ReferenceCountUtil.release(buf);
    }
  }

  public void connect(EventLoopGroup eventLoop, ChannelHandler channelHandler) {
    ProxyClient client = new ProxyClient();
    client.setAddress(proxyClientConfig.getAddress());
    client.setPort(proxyClientConfig.getPort());
    client.setHandler(channelHandler);

    boolean isSuc = client.connect(eventLoop).syncUninterruptibly().isSuccess();
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
