package org.example.net.codec.msg;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.example.serde.Serde;

@Serde
public class CodecObject extends CodecParentObject {

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
    return getId() == that.getId() && getAge() == that.getAge() && isSigned() == that.isSigned()
        && Objects.equals(getMsg(), that.getMsg()) && Objects.deepEquals(
        getDatas(), that.getDatas()) && Objects.deepEquals(getBitArray(),
        that.getBitArray());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMsg(), getId(), getAge(), Arrays.hashCode(getDatas()), isSigned(),
        Arrays.hashCode(getBitArray()));
  }

  @Override
  public String toString() {
    return "CodecObject{" +
        "msg=" + msg +
        ", id=" + id +
        ", age=" + age +
        ", datas=" + Arrays.toString(datas) +
        ", signed=" + signed +
        ", bitArray=" + Arrays.toString(bitArray) +
        '}';
  }
}
