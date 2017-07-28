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

    long before, after;

    before = System.nanoTime();
    benchmark.groupByVector(context, new Blackhole(str));
    after = System.nanoTime();
    System.out.println("groupByVector!\t" + (after - before) + " ns");
    final ByteBuffer vectorResults = context.resultBuffer.duplicate();

    context.resetResults();

    before = System.nanoTime();
    benchmark.groupByJniVector(context, new Blackhole(str));
    after = System.nanoTime();
    System.out.println("groupByJniVector!\t\t" + (after - before) + " ns");

    before = System.nanoTime();
    benchmark.groupByScala(context, new Blackhole(str));
    after = System.nanoTime();
    System.out.println("groupByScala!\t\t" + (after - before) + " ns");
    final ByteBuffer scalaResults = context.resultBuffer.duplicate();

    context.resetResults();

    for (int i = 0; i < scalaResults.capacity(); i++) {
      if (scalaResults.get(i) != vectorResults.get(i)) {
        throw new RuntimeException("Different results");
      }
      if (scalaResults.get(i) != context.resultBuffer.get(i)) {
        throw new RuntimeException("Different results");
      }
    }
  }
}
