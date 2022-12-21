package org.example.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;
import org.example.net.Message;
import org.example.serde.NettyByteBufUtil;
import org.example.serde.Serializer;

/**
 * {@link Message} 序列化和反序列
 *
 * @author ZJP
 * @since 2021年07月24日 16:28:07
 **/
public class MessageCodec extends ByteToMessageCodec<Message> {


  /** 序列化实现 */
  private Serializer<Object> serializer;

  public MessageCodec(Serializer<?> serializer) {
    this.serializer = (Serializer<Object>) serializer;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
    int lengthFieldLength = Integer.BYTES;
    int start = out.writerIndex();
    //serializing
    out.writerIndex(start + lengthFieldLength);

    NettyByteBufUtil.writeInt32(out, msg.target());
    NettyByteBufUtil.writeInt32(out, msg.source());
    NettyByteBufUtil.writeInt32(out, msg.proto());
    NettyByteBufUtil.writeInt32(out, msg.msgId());
    serializer.writeObject(out, msg.packet());

    //set the length
    int length = out.writerIndex() - start;
    out.setInt(start, length);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    int lengthFieldLength = Integer.BYTES;
    if (in.readableBytes() < lengthFieldLength) {
      return;
    }

    int length = in.getInt(in.readerIndex());
    if (length <= 0) {
      throw new RuntimeException(
          String.format("address:%s, Body Size is incorrect:%s", ctx.channel().remoteAddress(),
              length));
    }

    if (in.readableBytes() < length) {
      return;
    }

    in.skipBytes(lengthFieldLength);

    Message message = new Message();
    message.target(NettyByteBufUtil.readInt32(in));
    message.source(NettyByteBufUtil.readInt32(in));
    message.proto(NettyByteBufUtil.readInt32(in));
    message.msgId(NettyByteBufUtil.readInt32(in));
    message.packet(serializer.readObject(in));

    out.add(message);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
  }
}
