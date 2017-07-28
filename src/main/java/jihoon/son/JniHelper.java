package jihoon.son;

import java.nio.ByteBuffer;

public class JniHelper
{
  public static native void vectorAggregate(ByteBuffer keyBuffer, ByteBuffer valBuffer, ByteBuffer resultBuffer, int i);
}
