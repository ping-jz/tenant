package org.example.benchmark.serde;


import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.example.serde.CollectionSerializer;
import org.example.serde.CommonSerializer;
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

  CommonSerializer codeSerde;
  CommonSerializer refSerde;

  @Setup
  public void prepare() {
    object = new CodecObject();

    codeSerde = new CommonSerializer();
    codeSerde.registerSerializer(CodecObject.class, new CodecObjectSerde(codeSerde));
    codeSerde.registerSerializer(List.class, new CollectionSerializer(codeSerde));

    refSerde = new CommonSerializer();
    refSerde.registerObject(CodecObject.class);
    refSerde.registerSerializer(List.class, new CollectionSerializer(refSerde));
  }

  @Setup(Level.Iteration)
  public void refreshObj() throws InterruptedException {
    object = new CodecObject();
  }


  @Benchmark
  @Warmup
  @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void codecSerdeTest(Blackhole bh) {
    ByteBuf buf2 = codeSerde.writeObject(object);
    CodecObject object2 = codeSerde.read(buf2);

    if (!object2.equals(object)) {
      throw new RuntimeException();
    }
    bh.consume(object2);

    ReferenceCountUtil.release(buf2);
  }

  @Benchmark
  @Warmup
  @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void codecRefTest(Blackhole bh) {
    ByteBuf buf2 = refSerde.writeObject(object);
    CodecObject object2 = refSerde.read(buf2);

    if (!object2.equals(object)) {
      throw new RuntimeException();
    }
    bh.consume(object2);

    ReferenceCountUtil.release(buf2);
  }

  public static void main(String[] args) throws RunnerException {
    //Benchmark                                                   Mode  Cnt     Score    Error   Units
    //SerdeTest.codecRefTest                                     thrpt    5   368.791 ±  4.650  ops/ms
    //SerdeTest.codecRefTest:·gc.alloc.rate                      thrpt    5   481.324 ±  6.366  MB/sec
    //SerdeTest.codecRefTest:·gc.alloc.rate.norm                 thrpt    5  1438.728 ±  0.163    B/op
    //SerdeTest.codecRefTest:·gc.churn.G1_Eden_Space             thrpt    5   481.874 ± 24.860  MB/sec
    //SerdeTest.codecRefTest:·gc.churn.G1_Eden_Space.norm        thrpt    5  1440.348 ± 63.070    B/op
    //SerdeTest.codecRefTest:·gc.churn.G1_Survivor_Space         thrpt    5     0.003 ±  0.004  MB/sec
    //SerdeTest.codecRefTest:·gc.churn.G1_Survivor_Space.norm    thrpt    5     0.009 ±  0.011    B/op
    //SerdeTest.codecRefTest:·gc.count                           thrpt    5   169.000           counts
    //SerdeTest.codecRefTest:·gc.time                            thrpt    5   156.000               ms
    //SerdeTest.codecSerdeTest                                   thrpt    5   407.170 ± 17.733  ops/ms
    //SerdeTest.codecSerdeTest:·gc.alloc.rate                    thrpt    5   490.034 ± 21.563  MB/sec
    //SerdeTest.codecSerdeTest:·gc.alloc.rate.norm               thrpt    5  1326.756 ±  0.127    B/op
    //SerdeTest.codecSerdeTest:·gc.churn.G1_Eden_Space           thrpt    5   490.455 ± 30.374  MB/sec
    //SerdeTest.codecSerdeTest:·gc.churn.G1_Eden_Space.norm      thrpt    5  1327.894 ± 56.607    B/op
    //SerdeTest.codecSerdeTest:·gc.churn.G1_Survivor_Space       thrpt    5     0.004 ±  0.004  MB/sec
    //SerdeTest.codecSerdeTest:·gc.churn.G1_Survivor_Space.norm  thrpt    5     0.010 ±  0.010    B/op
    //SerdeTest.codecSerdeTest:·gc.count                         thrpt    5   172.000           counts
    //SerdeTest.codecSerdeTest:·gc.time                          thrpt    5   157.000               ms
    //SerdeTest.codecRefTest                                      avgt    5     0.003 ±  0.001   ms/op
    //SerdeTest.codecRefTest:·gc.alloc.rate                       avgt    5   492.937 ± 18.432  MB/sec
    //SerdeTest.codecRefTest:·gc.alloc.rate.norm                  avgt    5  1438.760 ±  0.180    B/op
    //SerdeTest.codecRefTest:·gc.churn.G1_Eden_Space              avgt    5   493.281 ± 29.257  MB/sec
    //SerdeTest.codecRefTest:·gc.churn.G1_Eden_Space.norm         avgt    5  1439.809 ± 82.324    B/op
    //SerdeTest.codecRefTest:·gc.churn.G1_Survivor_Space          avgt    5     0.004 ±  0.004  MB/sec
    //SerdeTest.codecRefTest:·gc.churn.G1_Survivor_Space.norm     avgt    5     0.011 ±  0.011    B/op
    //SerdeTest.codecRefTest:·gc.count                            avgt    5   173.000           counts
    //SerdeTest.codecRefTest:·gc.time                             avgt    5   160.000               ms
    //SerdeTest.codecSerdeTest                                    avgt    5     0.002 ±  0.001   ms/op
    //SerdeTest.codecSerdeTest:·gc.alloc.rate                     avgt    5   491.346 ± 28.684  MB/sec
    //SerdeTest.codecSerdeTest:·gc.alloc.rate.norm                avgt    5  1326.747 ±  0.127    B/op
    //SerdeTest.codecSerdeTest:·gc.churn.G1_Eden_Space            avgt    5   493.001 ± 49.055  MB/sec
    //SerdeTest.codecSerdeTest:·gc.churn.G1_Eden_Space.norm       avgt    5  1331.057 ± 58.893    B/op
    //SerdeTest.codecSerdeTest:·gc.churn.G1_Survivor_Space        avgt    5     0.003 ±  0.003  MB/sec
    //SerdeTest.codecSerdeTest:·gc.churn.G1_Survivor_Space.norm   avgt    5     0.008 ±  0.009    B/op
    //SerdeTest.codecSerdeTest:·gc.count                          avgt    5   173.000           counts
    //SerdeTest.codecSerdeTest:·gc.time                           avgt    5   161.000               ms
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
