package org.example.benchmark.serde;


import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;


public final class SerdeRunner {

  public static void main(String[] args) throws RunnerException {
    //  Benchmark                  Mode  Cnt        Score       Error  Units
    // RefImple.codecRefTest     thrpt    5  1209216.894 ± 22915.387  ops/s
    // SerdeImpl.codecSerdeTest  thrpt    5  1485607.020 ± 20903.181  ops/s
    //运行效率有约20%的提升
    //gc相差不多是因为主要消耗在序列的数据，而不是实现框架。新的解码有略微下降是因为少了反射的调用·
    Options opt = new OptionsBuilder()
        .include(RefImple.class.getSimpleName())
        .include(SerdeImpl.class.getSimpleName())
        .build();

    new Runner(opt).run();
  }


}
