package org.example.benchmark.serde;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.example.serde.CollectionSerializer;
import org.example.serde.Serdes;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
public class SerdeTest {

  CodecObject object;

  Serdes codeSerde;
  Serdes refSerde;

  @Setup
  public void prepare() {
    object = new CodecObject();

    codeSerde = new Serdes();
    codeSerde.registerSerializer(CodecObject.class, new CodecObjectSerde());
    codeSerde.registerSerializer(List.class, new CollectionSerializer());
    codeSerde.registerSerializer(ArrayList.class, new CollectionSerializer());

    refSerde = new Serdes();
    refSerde.registerObject(CodecObject.class);
    refSerde.registerSerializer(List.class, new CollectionSerializer());
    refSerde.registerSerializer(ArrayList.class, new CollectionSerializer());
  }

  @Setup(Level.Iteration)
  public void refreshObj() throws InterruptedException {
    object = new CodecObject();
  }


  @Benchmark
  @Warmup
  @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void codecSerdeTest(Blackhole bh) {
    ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer();
    codeSerde.writeObject(buf, object);
    CodecObject object2 = codeSerde.readObject(buf);

    if (!object2.equals(object)) {
      throw new RuntimeException();
    }
    bh.consume(object2);

    ReferenceCountUtil.release(buf);
  }

  @Benchmark
  @Warmup
  @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void codecRefTest(Blackhole bh) {
    ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer();
    refSerde.writeObject(buf, object);
    CodecObject object2 = refSerde.readObject(buf);

    if (!object2.equals(object)) {
      throw new RuntimeException();
    }
    bh.consume(object2);

    ReferenceCountUtil.release(buf);
  }

  public static void main(String[] args) throws RunnerException {
//    Benchmark                                                   Mode  Cnt       Score       Error   Units
//    SerdeTest.codecRefTest                                     thrpt    5  312301.663 ± 43310.793   ops/s
//    SerdeTest.codecRefTest:·gc.alloc.rate                      thrpt    5     400.878 ±    55.653  MB/sec
//    SerdeTest.codecRefTest:·gc.alloc.rate.norm                 thrpt    5    1414.787 ±     0.147    B/op
//    SerdeTest.codecRefTest:·gc.churn.G1_Eden_Space             thrpt    5     402.048 ±    71.808  MB/sec
//    SerdeTest.codecRefTest:·gc.churn.G1_Eden_Space.norm        thrpt    5    1418.573 ±    81.986    B/op
//    SerdeTest.codecRefTest:·gc.churn.G1_Survivor_Space         thrpt    5       0.003 ±     0.002  MB/sec
//    SerdeTest.codecRefTest:·gc.churn.G1_Survivor_Space.norm    thrpt    5       0.011 ±     0.007    B/op
//    SerdeTest.codecRefTest:·gc.count                           thrpt    5     141.000              counts
//    SerdeTest.codecRefTest:·gc.time                            thrpt    5     127.000                  ms
//    SerdeTest.codecSerdeTest                                   thrpt    5  342376.390 ±  4012.424   ops/s
//    SerdeTest.codecSerdeTest:·gc.alloc.rate                    thrpt    5     404.704 ±     4.662  MB/sec
//    SerdeTest.codecSerdeTest:·gc.alloc.rate.norm               thrpt    5    1302.716 ±     0.183    B/op
//    SerdeTest.codecSerdeTest:·gc.churn.G1_Eden_Space           thrpt    5     405.054 ±    30.793  MB/sec
//    SerdeTest.codecSerdeTest:·gc.churn.G1_Eden_Space.norm      thrpt    5    1303.862 ±   102.128    B/op
//    SerdeTest.codecSerdeTest:·gc.churn.G1_Survivor_Space       thrpt    5       0.003 ±     0.004  MB/sec
//    SerdeTest.codecSerdeTest:·gc.churn.G1_Survivor_Space.norm  thrpt    5       0.008 ±     0.013    B/op
//    SerdeTest.codecSerdeTest:·gc.count                         thrpt    5     142.000              counts
//    SerdeTest.codecSerdeTest:·gc.time                          thrpt    5     137.000                  ms
//    SerdeTest.codecRefTest                                      avgt    5      ≈ 10⁻⁵                s/op
//    SerdeTest.codecRefTest:·gc.alloc.rate                       avgt    5     380.532 ±     6.200  MB/sec
//    SerdeTest.codecRefTest:·gc.alloc.rate.norm                  avgt    5    1414.773 ±     0.141    B/op
//    SerdeTest.codecRefTest:·gc.churn.G1_Eden_Space              avgt    5     382.226 ±    25.051  MB/sec
//    SerdeTest.codecRefTest:·gc.churn.G1_Eden_Space.norm         avgt    5    1421.049 ±    83.740    B/op
//    SerdeTest.codecRefTest:·gc.churn.G1_Survivor_Space          avgt    5       0.002 ±     0.002  MB/sec
//    SerdeTest.codecRefTest:·gc.churn.G1_Survivor_Space.norm     avgt    5       0.009 ±     0.007    B/op
//    SerdeTest.codecRefTest:·gc.count                            avgt    5     134.000              counts
//    SerdeTest.codecRefTest:·gc.time                             avgt    5     127.000                  ms
//    SerdeTest.codecSerdeTest                                    avgt    5      ≈ 10⁻⁶                s/op
//    SerdeTest.codecSerdeTest:·gc.alloc.rate                     avgt    5     406.005 ±    14.311  MB/sec
//    SerdeTest.codecSerdeTest:·gc.alloc.rate.norm                avgt    5    1302.780 ±     0.134    B/op
//    SerdeTest.codecSerdeTest:·gc.churn.G1_Eden_Space            avgt    5     405.056 ±    30.423  MB/sec
//    SerdeTest.codecSerdeTest:·gc.churn.G1_Eden_Space.norm       avgt    5    1299.735 ±    85.520    B/op
//    SerdeTest.codecSerdeTest:·gc.churn.G1_Survivor_Space        avgt    5       0.003 ±     0.003  MB/sec
//    SerdeTest.codecSerdeTest:·gc.churn.G1_Survivor_Space.norm   avgt    5       0.010 ±     0.011    B/op
//    SerdeTest.codecSerdeTest:·gc.count                          avgt    5     142.000              counts
//    SerdeTest.codecSerdeTest:·gc.time                           avgt    5     133.000                  ms
    //运行效率有约10%的提升
    //gc相差不多是因为主要消耗在序列的数据，而不是实现框架。新的解码有略微下降是因为少了反射的调用·
    Options opt = new OptionsBuilder()
        .include(SerdeTest.class.getSimpleName())
        .addProfiler(GCProfiler.class)
        .forks(1)
        .build();

    new Runner(opt).run();
  }


}
