#include <jni.h>
#include <stdio.h>

#include "jihoon_son_JniHelper.h"
#include "immintrin.h"
#include "emmintrin.h"

const int BUFFER_SIZE = 1024;

JNIEXPORT void JNICALL Java_jihoon_son_JniHelper_vectorAggregate
  (JNIEnv *env, jclass thisClass, jobject jkeyBuffer, jobject jvalBuffer, jobject jresultBuffer, jint i) {

  int *keys = reinterpret_cast<int*>(env->GetDirectBufferAddress(jkeyBuffer));
  int *vals = reinterpret_cast<int*>(env->GetDirectBufferAddress(jvalBuffer));
  int *results = reinterpret_cast<int*>(env->GetDirectBufferAddress(jresultBuffer));

  int iterEnd = i + BUFFER_SIZE;
  for (int v = i; v < iterEnd; v++) {
    results[keys[v]] += vals[v];
  }
}
