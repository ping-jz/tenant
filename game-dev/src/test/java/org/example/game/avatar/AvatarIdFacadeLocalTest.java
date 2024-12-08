package org.example.game.avatar;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.example.common.model.AvatarId;
import org.example.common.model.ReqMove;
import org.example.common.model.ResMove;
import org.example.net.CompleteAbleFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;

public class AvatarIdFacadeLocalTest {

  private static AvatarIdFacadeLocal facade;
  private static AvatarIdFacadeLocalInvoker invoker;

  @BeforeAll
  public static void beforeAll() {
    facade = new AvatarIdFacadeLocal();
    invoker = new AvatarIdFacadeLocalInvoker(facade);
  }

  @RepeatedTest(10)
  public void set() throws Exception {
    AvatarId id = new AvatarId(ThreadLocalRandom.current().nextInt());
    invoker.set(id);
    TimeUnit.MILLISECONDS.sleep(10);
    Assertions.assertEquals(id, facade.id);
  }

  @RepeatedTest(10)
  public void echo() throws Exception {
    AvatarId avatarId = new AvatarId(ThreadLocalRandom.current().nextInt());
    Assertions.assertEquals(avatarId, invoker.echo(avatarId).get(10, TimeUnit.MILLISECONDS));
  }


  @RepeatedTest(10)
  public void callBack() throws Exception {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    AvatarId avatarId = new AvatarId(random.nextInt());
    boolean boolean1 = random.nextBoolean();
    byte[] byte1 = new byte[random.nextInt(10)];
    random.nextBytes(byte1);
    short short1 = (short) random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
    char char1 = (char) random.nextInt();
    int int1 = random.nextInt();
    long long1 = random.nextLong();
    float float1 = random.nextFloat();
    double double1 = random.nextDouble();

    ReqMove reqMove = new ReqMove();
    reqMove.setId(random.nextInt());
    reqMove.setX(random.nextFloat());
    reqMove.setY(random.nextFloat());

    ResMove resMove = new ResMove();
    resMove.setId(random.nextInt());
    resMove.setX(random.nextFloat());
    resMove.setY(random.nextFloat());
    resMove.setDir(random.nextInt());

    int hashcode = Objects.hash(avatarId, boolean1, Arrays.hashCode(byte1), short1, char1, int1,
        long1,
        float1, double1, reqMove, resMove);

    CompleteAbleFuture<Integer> callback = invoker
        .callback(avatarId, boolean1, byte1, short1, char1, int1, long1, float1, double1, reqMove,
            resMove);

    Assertions.assertThrows(NullPointerException.class,
        () -> callback.whenComplete((ignorea, ingorea) -> {
        })
    );

    Assertions.assertEquals(hashcode, callback.get(10, TimeUnit.MILLISECONDS));
  }


}
