package org.example.util;

import java.security.MessageDigest;

/**
 * MD5加密工具
 *
 * @author zhongjianping
 * @since 2022/12/13 14:22
 */
public final class MD5Util {

  /**
   * MD5加密
   *
   * @since 2022年12月13日“ 14:33
   */
  public static String md5(String val) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] data = val.getBytes();
      byte[] mdbs = md.digest(data);
      return printHexBinary(mdbs);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

  public static String printHexBinary(byte[] data) {
    StringBuilder r = new StringBuilder(data.length * 2);
    for (byte b : data) {
      r.append(hexCode[(b >> 4) & 0xF]);
      r.append(hexCode[(b & 0xF)]);
    }
    return r.toString();
  }

}
