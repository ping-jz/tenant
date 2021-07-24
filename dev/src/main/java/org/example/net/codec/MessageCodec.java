package org.example.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;
import org.example.net.Message;
import org.example.serde.Serializer;

/**
 * {@link Message} 序列化和反序列
 *
 * @author ZJP
 * @since 2021年07月24日 16:28:07
 **/
public class MessageCodec extends ByteToMessageCodec<Message> {

  /** 长度字段占多少个字节 */
  private int lengthFieldLength;
  /** 序列化实现 */
  private Serializer<Object> serializer;

  public MessageCodec(Serializer<Object> serializer) {
    this(Integer.BYTES, serializer);
  }

  public MessageCodec(int lengthFieldLength,
      Serializer<Object> serializer) {
    this.lengthFieldLength = lengthFieldLength;
    this.serializer = serializer;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
    int start = out.writerIndex();
    //serializing
    int messageStart = start + lengthFieldLength;
    out.writerIndex(messageStart);
    out.writeInt(msg.proto());
    out.writeInt(msg.optIdx());
    serializer.writeObject(out, msg.packet());

    //set the length
    int length = out.writerIndex() - messageStart;
    out.setInt(start, length);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (in.readableBytes() < lengthFieldLength) {
      return;
    }

    int length = in.getInt(in.readerIndex());
    if (length <= 0) {
      throw new RuntimeException(String
          .format("address:%s, Body Size is incorrect:%s", ctx.channel().remoteAddress(), length));
    }

    if (in.readableBytes() - lengthFieldLength < length) {
      return;
    }

    in.skipBytes(lengthFieldLength);

    Message message = new Message();
    message.proto(in.readInt());
    message.optIdx(in.readInt());
    message.packet(serializer.readObject(in));

    out.add(message);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
      throws Exception {
    ctx.close();
  }
}
