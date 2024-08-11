package org.example.benchmark.serde;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import org.example.serde.Serde;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@Serde
public class CodecObject {

  private List<String> msg;

  private int id;

  private long age;

  private short s;

  private long[] datas;

  private boolean signed;

  private byte[] bitArray;

  public CodecObject() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    msg = new ArrayList<>(List.of(String.valueOf(random.nextLong()), String.valueOf(random.nextLong()), String.valueOf(random.nextLong())));
    id = random.nextInt();
    age = random.nextLong();
    s = (short) random.nextInt();
    datas = new long[]{random.nextLong(), random.nextLong(), random.nextLong(), random.nextLong()};
    signed = random.nextBoolean();
    bitArray = new byte[10];
    random.nextBytes(bitArray);
  }

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

  public short getS() {
    return s;
  }

  public void setS(short s) {
    this.s = s;
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
    return getId() == that.getId() && getAge() == that.getAge() && getS() == that.getS()
        && isSigned() == that.isSigned() && Objects.equals(getMsg(), that.getMsg())
        && Objects.deepEquals(getDatas(), that.getDatas()) && Objects.deepEquals(
        getBitArray(), that.getBitArray());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMsg(), getId(), getAge(), getS(), Arrays.hashCode(getDatas()),
        isSigned(),
        Arrays.hashCode(getBitArray()));
  }
}
