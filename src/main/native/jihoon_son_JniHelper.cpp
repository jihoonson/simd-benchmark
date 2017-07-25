#include <jni.h>
#include <stdio.h>
#include "jihoon_son_JniHelper.h"
#include "immintrin.h"
#include "emmintrin.h"

const int BUFFER_SIZE = 4096;

JNIEXPORT void JNICALL Java_jihoon_son_JniHelper_vectorAggregate
  (JNIEnv *env, jclass thisClass, jintArray jkeys, jintArray jvals, jintArray jresults, jint start_index) {
  int *keys = reinterpret_cast<int*>(env->GetPrimitiveArrayCritical(jkeys, NULL));
  int *vals = reinterpret_cast<int*>(env->GetPrimitiveArrayCritical(jvals, NULL));
  int *results = reinterpret_cast<int*>(env->GetPrimitiveArrayCritical(jresults, NULL));

  for (int i = 0; i < BUFFER_SIZE; i++) {
    results[keys[i]] += vals[i];
  }

  env->ReleasePrimitiveArrayCritical(jresults, results, 0);
  env->ReleasePrimitiveArrayCritical(jkeys, keys, 0);
  env->ReleasePrimitiveArrayCritical(jvals, vals, 0);
}
