package org.example.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;
import org.example.net.Message;
import org.example.util.NettyByteBufUtil;

/**
 * {@link Message} 序列化和反序列
 *
 * @author ZJP
 * @since 2021年07月24日 16:28:07
 **/
public class MessageCodec extends ByteToMessageCodec<Message> {

  public MessageCodec() {
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
    int lengthFieldLength = Integer.BYTES;
    int start = out.writerIndex();
    //serializing
    out.writerIndex(start + lengthFieldLength);
    NettyByteBufUtil.writeVarInt32(out, msg.proto());
    out.writeBytes(msg.packet());

    //set the length
    int length = out.writerIndex() - start;
    out.setInt(start, length);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    final int lengthFieldLength = Integer.BYTES;
    if (in.readableBytes() < lengthFieldLength) {
      return;
    }

    int readIdx = in.readerIndex();
    int length = in.getInt(in.readerIndex());
    if (length <= 0) {
      throw new RuntimeException(
          String.format("address:%s, Body Size is incorrect:%s", ctx.channel().remoteAddress(),
              length));
    }

    if (in.readableBytes() < length) {
      return;
    }

    in.skipBytes(length);
    ByteBuf buf = in.retainedSlice(readIdx, length).skipBytes(lengthFieldLength);

    out.add(Message.of(
        NettyByteBufUtil.readVarInt32(buf),
        buf));
  }
}
