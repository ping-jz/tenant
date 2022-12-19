package org.example.game.facade.example;

import java.util.Collections;
import java.util.List;
import org.example.game.facade.example.model.CommonRes;
import org.example.net.anno.ReqMethod;
import org.example.net.anno.RpcModule;

/**
 * 游戏门面(文档生产插件测试)
 *
 * @author ZJP
 * @since 2021年09月27日 15:54:54
 **/
@RpcModule
public class GameFacade {

  /**
   * 获取此进程的全部游戏服ID
   *
   * @author ZJP
   * @since 2021年09月27日 16:01:08
   **/
  @ReqMethod(200)
  public List<Integer> getServerIds() {
    return Collections.emptyList();
  }

  /**
   * 消耗指定物品
   *
   * @param ids 物品ID
   * @since 2021年09月28日 11:58:07
   */
  @ReqMethod(201)
  public CommonRes<Integer> consumerItem(List<Long> ids) {
    CommonRes<Integer> res = new CommonRes<>();
    return res.suc(true).res(10);
  }

  /**
   * 请求我
   *
   * @author ZJP
   * @since 2021年09月28日 12:16:00
   **/
  @ReqMethod(202)
  public String req() {
    return "ok";
  }


}
