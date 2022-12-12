package org.example.util;

/**
 * 获取唯一ID接口
 *
 * @author ZJP
 * @since 2021年09月29日 16:31:02
 **/
@FunctionalInterface
public interface Identity<PK> {

  PK id();
}
