package org.example.game;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.example.common.HelloWorld;
import org.example.game.log.LoggerService;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Sharable
public class HelloService extends SimpleChannelInboundHandler<HttpObject> {

  @Autowired
  private HelloWorld helloWorld;
  @Autowired
  private LoggerService loggerService;

  @Value("${game.id}")
  private String name;

  private Integer pid;

  private static AtomicInteger count;


  @PostConstruct
  public void postConstruct() {
    RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    pid = Integer.valueOf(runtimeMXBean.getName().split("@")[0]);

    count = new AtomicInteger();
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
    //直接从netty的示例代码复制过来的，稍微改了下
    if (msg instanceof HttpRequest) {
      HttpRequest req = (HttpRequest) msg;

      String content = helloWorld
          .hello(String.format("%s 进程:%s hello world 计数:%s", name, pid, count.incrementAndGet()));

      loggerService.log().error(content);

      boolean keepAlive = HttpUtil.isKeepAlive(req);
      FullHttpResponse response = new DefaultFullHttpResponse(req.protocolVersion(), OK,
          Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8)));
      response.headers()
          .set(CONTENT_TYPE, "text/plain; charset=utf-8")
          .setInt(CONTENT_LENGTH, response.content().readableBytes());

      if (keepAlive) {
        if (!req.protocolVersion().isKeepAliveDefault()) {
          response.headers().set(CONNECTION, KEEP_ALIVE);
        }
      } else {
        // Tell the client we're going to close the connection.
        response.headers().set(CONNECTION, CLOSE);
      }

      ChannelFuture f = ctx.write(response);

      if (!keepAlive) {
        f.addListener(ChannelFutureListener.CLOSE);
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
