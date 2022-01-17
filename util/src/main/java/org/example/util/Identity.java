package org.example.util;

import java.io.Serializable;

/**
 * 获取唯一ID接口
 *
 * @author ZJP
 * @since 2021年09月29日 16:31:02
 **/
public interface Identity<PK extends Serializable & Comparable<PK>> {

  PK id();
}
