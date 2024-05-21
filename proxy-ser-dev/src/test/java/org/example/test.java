package org.example;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class test {



  @Test
  public void  AA() {
    var a = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1676172325356L), ZoneId.systemDefault());
    System.out.println(a);

    var b = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1676172302219L), ZoneId.systemDefault());
    System.out.println(b);

    System.out.println(TimeUnit.MILLISECONDS.toSeconds(1000000));

    double cd = Math.max(0.3D, 0.8D - 6.366579406033772) * 0.128D * 1000D;
    System.out.println(cd);
    System.currentTimeMillis();
  }
}
