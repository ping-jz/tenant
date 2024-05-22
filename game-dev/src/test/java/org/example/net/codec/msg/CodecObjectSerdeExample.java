package org.example.net.codec.msg;

import io.netty.buffer.ByteBuf;
import org.example.serde.CommonSerializer;

import org.example.serde.NettyByteBufUtil;
import org.example.serde.Serializer;


public class CodecObjectSerdeExample implements Serializer<CodecObject> {

  /**
   * 序列实现集合
   */
  private CommonSerializer serializer;

  public CodecObjectSerdeExample(CommonSerializer serializer) {
    this.serializer = serializer;
  }

  @Override
  public CodecObject readObject(ByteBuf buf) {
    CodecObject codecObject = new CodecObject();
    codecObject.setType(NettyByteBufUtil.readInt32(buf));
    codecObject.setMsg(serializer.read(buf));
    codecObject.setId(NettyByteBufUtil.readInt32(buf));
    codecObject.setAge(NettyByteBufUtil.readInt64(buf));
    codecObject.setDatas(serializer.read(buf));
    codecObject.setSigned(buf.readBoolean());
    codecObject.setBitArray(serializer.read(buf));
    return codecObject;
  }

  @Override
  public void writeObject(ByteBuf buf, CodecObject object) {
    NettyByteBufUtil.writeInt32(buf, object.getType());
    serializer.writeObject(buf, object.getMsg());
    NettyByteBufUtil.writeInt32(buf, object.getId());
    NettyByteBufUtil.writeInt64(buf, object.getAge());
    serializer.writeObject(buf, object.getDatas());
    buf.writeBoolean(object.isSigned());
    serializer.writeObject(buf, object.getBitArray());
  }
}
