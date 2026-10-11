#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_app_luau_readable_MainActivity_nativeStatus(JNIEnv* env, jobject) {
    return env->NewStringUTF("Motor C++ carregado (decompilador ainda não integrado).");
}
