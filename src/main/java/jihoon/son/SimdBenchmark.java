package jihoon.son;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Arrays;
import java.util.Random;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.openjdk.jmh.annotations.Mode.AverageTime;

@State(Scope.Benchmark)
@OutputTimeUnit(NANOSECONDS)
@BenchmarkMode(AverageTime)
@Fork(value = 1, jvmArgsAppend = {
    "-XX:+UseSuperWord",
    "-Djava.library.path=/home/jihoonson/Projects/simd-benchmark"
//    "-XX:+UnlockDiagnosticVMOptions",
//    "-XX:+PrintAssembly"
})
@Warmup(iterations = 5)
@Measurement(iterations = 10)
public class SimdBenchmark
{
    static {
        System.loadLibrary("jnihelper");
    }

    static final int ROWS_NUM = 1_000_000;
    static final int VECTOR_SIZE = 4096;
//    static final int KEY_CARDINALITY = ROWS_NUM / 1000;
static final int KEY_CARDINALITY = 10;

    @State(Scope.Benchmark)
    public static class Context
    {
        final int[] keys = new int[ROWS_NUM];
        final int[] vals = new int[ROWS_NUM];
        final int[] results = new int[KEY_CARDINALITY];

        @Setup(Level.Trial)
        public void setup()
        {
            final Random random = new Random();
            for (int i = 0; i < ROWS_NUM; i++) {
                keys[i] = random.nextInt(KEY_CARDINALITY);
                vals[i] = random.nextInt(100);
            }
        }
    }

    @Benchmark
    public int[] groupByScala(Context context)
    {
        final int[] keys = context.keys;
        final int[] vals = context.vals;
        final int[] results = context.results;

        for (int i = 0; i < ROWS_NUM; i++) {
            results[keys[i]] += vals[i];
        }

        return results;
    }

    @Benchmark
    public int[] groupByVector(Context context)
    {
        final int[] keys = context.keys;
        final int[] vals = context.vals;
        final int[] results = context.results;

        final int maxIter = ROWS_NUM / VECTOR_SIZE;
        final int extraStart = maxIter * VECTOR_SIZE;

        for (int i = 0; i < extraStart; i += VECTOR_SIZE) {
            final int iterEnd = i + VECTOR_SIZE;
            for (int v = i; v < iterEnd; v++) {
                results[keys[v]] += vals[v];
            }
        }

        for (int i = extraStart; i < ROWS_NUM; i++) {
            results[keys[i]] += vals[i];
        }

        return results;
    }

    @Benchmark
    public int[] groupBySimd(Context context)
    {
        final int[] keys = context.keys;
        final int[] vals = context.vals;
        final int[] results = context.results;

        final int[] subkeys = new int[VECTOR_SIZE];
        final int[] subvals = new int[VECTOR_SIZE];

        final int maxIter = ROWS_NUM / VECTOR_SIZE;
        final int extraStart = maxIter * VECTOR_SIZE;

        for (int i = 0; i < extraStart; i += VECTOR_SIZE) {
            System.arraycopy(keys, 0, subkeys, 0, VECTOR_SIZE);
            System.arraycopy(vals, 0, subvals, 0, VECTOR_SIZE);
            JniHelper.vectorAggregate(subkeys, subvals, results, i);
        }

        for (int i = extraStart; i < ROWS_NUM; i++) {
            results[keys[i]] += vals[i];
        }

        return results;
    }
}