package org.example.serde;


import io.netty.buffer.ByteBuf;

/**
 * 编码解码器接口
 *
 * <pre>
 *    +--------+-----------------+
 *    + 类型ID  | "内       容" |
 *    +--------+----------------+
 *    类型ID长度:1-5字节, 使用varint32编码
 *    内容长度:根据实现来确定
 * </pre>
 *
 * //TODO 增加自动关联
 * //TODO 增加对接口类型的自动关联
 * //TODO 增加对数组，集合，字段的优化。对单一类型优化(增加一个)
 *
 * @author ZJP
 * @since 2021年07月17日 15:59:16
 **/
public interface Serializer<T> {

  /**
   * 从{@param buff}反序列化对象
   *
   * @param buf 目标buff
   * @since 2021年07月17日 16:02:03
   */
  T readObject(ByteBuf buf);

  /**
   * 把{@param object}序列化至{@param buff}
   *
   * @param buf 目标buff
   * @param object 对象
   * @since 2021年07月17日 16:02:03
   */
  void writeObject(ByteBuf buf, T object);
}
