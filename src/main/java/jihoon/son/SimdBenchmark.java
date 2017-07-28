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
import org.openjdk.jmh.infra.Blackhole;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    static final int KEY_SIZE = Integer.BYTES;
    static final int VAL_SIZE = Integer.BYTES;
    static final int ROWS_NUM = 1_000_000;
    static final int VECTOR_SIZE = 1024;

    static final int KEY_CARDINALITY = ROWS_NUM / 1000;
//    static final int KEY_CARDINALITY = 10;
    static final int VAL_CARDINALITY = KEY_CARDINALITY * 10;

    @State(Scope.Benchmark)
    public static class Context
    {
        final ByteBuffer keyBuffer = ByteBuffer.allocateDirect(KEY_SIZE * ROWS_NUM).order(ByteOrder.LITTLE_ENDIAN);
        final ByteBuffer valBuffer = ByteBuffer.allocateDirect(VAL_SIZE * ROWS_NUM).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer resultBuffer;

        @Setup(Level.Trial)
        public void setup()
        {
            final Random random = new Random();
            for (int i = 0; i < ROWS_NUM; i++) {
                keyBuffer.putInt(random.nextInt(KEY_CARDINALITY));
                valBuffer.putInt(random.nextInt(VAL_CARDINALITY));
            }

            resetResults();
        }

        public void resetResults()
        {
            resultBuffer = ByteBuffer.allocateDirect(VAL_SIZE * KEY_CARDINALITY).order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    @Benchmark
    public void groupByScala(Context context, Blackhole blackhole)
    {
        final ByteBuffer keyBuffer = context.keyBuffer;
        final ByteBuffer valBuffer = context.valBuffer;
        final ByteBuffer resultBuffer = context.resultBuffer;

        for (int i = 0; i < ROWS_NUM; i++) {
            aggregateSingleRow(keyBuffer, valBuffer, resultBuffer, i);
        }

        blackhole.consume(resultBuffer);
    }

    @Benchmark
    public void groupByScalaInline(Context context, Blackhole blackhole)
    {
        final ByteBuffer keyBuffer = context.keyBuffer;
        final ByteBuffer valBuffer = context.valBuffer;
        final ByteBuffer resultBuffer = context.resultBuffer;

        for (int i = 0; i < ROWS_NUM; i++) {
            final int key = keyBuffer.getInt(i * KEY_SIZE);
            final int val = valBuffer.getInt(i * VAL_SIZE);
            final int r = key * VAL_SIZE;
            final int acc = resultBuffer.getInt(r);
            resultBuffer.putInt(r, acc + val);
        }

        blackhole.consume(resultBuffer);
    }


    private static void aggregateSingleRow(ByteBuffer keyBuffer, ByteBuffer valBuffer, ByteBuffer resultBuffer, int i)
    {
        final int key = keyBuffer.getInt(i * KEY_SIZE);
        final int val = valBuffer.getInt(i * VAL_SIZE);
        final int r = key * VAL_SIZE;
        final int acc = resultBuffer.getInt(r);
        resultBuffer.putInt(r, acc + val);
    }

    @Benchmark
    public void groupByVector(Context context, Blackhole blackhole)
    {
        final ByteBuffer keyBuffer = context.keyBuffer;
        final ByteBuffer valBuffer = context.valBuffer;
        final ByteBuffer resultBuffer = context.resultBuffer;

        final int maxIter = ROWS_NUM / VECTOR_SIZE;
        final int extraStart = maxIter * VECTOR_SIZE;

        for (int i = 0; i < extraStart; i += VECTOR_SIZE) {
            aggregateRows(keyBuffer, valBuffer, resultBuffer, i);
        }

        for (int i = extraStart; i < ROWS_NUM; i++) {
            aggregateSingleRow(keyBuffer, valBuffer, resultBuffer, i);
        }

        blackhole.consume(resultBuffer);
    }

    private static void aggregateRows(ByteBuffer keyBuffer, ByteBuffer valBuffer, ByteBuffer resultBuffer, int i)
    {
        final int iterEnd = i + VECTOR_SIZE;
        for (int v = i; v < iterEnd; v++) {
            final int key = keyBuffer.getInt(v * KEY_SIZE);
            final int val = valBuffer.getInt(v * VAL_SIZE);
            final int r = key * VAL_SIZE;
            final int acc = resultBuffer.getInt(r);
            resultBuffer.putInt(r, acc + val);
        }
    }

    @Benchmark
    public void groupByJniVector(Context context, Blackhole blackhole)
    {
        final ByteBuffer keyBuffer = context.keyBuffer;
        final ByteBuffer valBuffer = context.valBuffer;
        final ByteBuffer resultBuffer = context.resultBuffer;

        final int maxIter = ROWS_NUM / VECTOR_SIZE;
        final int extraStart = maxIter * VECTOR_SIZE;

        for (int i = 0; i < extraStart; i += VECTOR_SIZE) {
            JniHelper.vectorAggregate(keyBuffer, valBuffer, resultBuffer, i);
        }

        for (int i = extraStart; i < ROWS_NUM; i++) {
            aggregateSingleRow(keyBuffer, valBuffer, resultBuffer, i);
        }

        blackhole.consume(resultBuffer);
    }
}