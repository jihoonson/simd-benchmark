package jihoon.son;

import jihoon.son.SimdBenchmark.Context;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.ByteBuffer;

public class Verifier
{
  private static final String str = "Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.";
  public static void main(String[] args)
  {
    final SimdBenchmark benchmark = new SimdBenchmark();
    final Context context = new Context();
    context.setup();

    benchmark.groupByVector(context, new Blackhole(str));
    final ByteBuffer vectorResults = context.resultBuffer.duplicate();

    context.resetResults();
    benchmark.groupByJniVector(context, new Blackhole(str));
    final ByteBuffer jniVectorResults = context.resultBuffer.duplicate();

    context.resetResults();
    benchmark.groupByScala(context, new Blackhole(str));

    for (int i = 0; i < vectorResults.capacity(); i++) {
      if (vectorResults.get(i) != jniVectorResults.get(i)) {
        throw new RuntimeException("Different results");
      }
      if (vectorResults.get(i) != context.resultBuffer.get(i)) {
        throw new RuntimeException("Different results");
      }
    }
  }
}
