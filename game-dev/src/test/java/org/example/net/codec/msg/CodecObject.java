package org.example.net.codec.msg;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.example.serde.Serde;

@Serde
public class CodecObject {

  private List<String> msg;

  private int id;

  private long age;

  private long[] datas;

  private boolean signed;

  private byte[] bitArray;

  public List<String> getMsg() {
    return msg;
  }

  public void setMsg(List<String> msg) {
    this.msg = msg;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public long getAge() {
    return age;
  }

  public void setAge(long age) {
    this.age = age;
  }

  public long[] getDatas() {
    return datas;
  }

  public void setDatas(long[] datas) {
    this.datas = datas;
  }

  public boolean isSigned() {
    return signed;
  }

  public void setSigned(boolean signed) {
    this.signed = signed;
  }

  public byte[] getBitArray() {
    return bitArray;
  }

  public void setBitArray(byte[] bitArray) {
    this.bitArray = bitArray;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CodecObject that = (CodecObject) o;
    return id == that.id && age == that.age && signed == that.signed && Objects.equals(msg,
        that.msg) && Objects.deepEquals(datas, that.datas) && Objects.deepEquals(
        bitArray, that.bitArray);
  }

  @Override
  public int hashCode() {
    return Objects.hash(msg, id, age, Arrays.hashCode(datas), signed, Arrays.hashCode(bitArray));
  }
}
