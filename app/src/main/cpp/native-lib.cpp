#include <jni.h>
#include <exception>
#include <string>
#include <vector>
#include "bytecode/Bytecode.h"
#include "lift/Lift.h"

extern "C" JNIEXPORT jstring JNICALL
Java_app_luau_readable_MainActivity_nativeStatus(JNIEnv* env, jobject) {
    return env->NewStringUTF("Motor de decompilação C++ carregado.");
}

extern "C" JNIEXPORT jstring JNICALL
Java_app_luau_readable_MainActivity_nativeDecompile(JNIEnv* env, jobject, jbyteArray input) {
    if (!input) return env->NewStringUTF("-- Erro: nenhum bytecode recebido.");
    const jsize length = env->GetArrayLength(input);
    if (length <= 0) return env->NewStringUTF("-- Erro: arquivo vazio.");
    if (length > 32 * 1024 * 1024) return env->NewStringUTF("-- Erro: arquivo maior que 32 MiB.");
    std::vector<uint8_t> bytes(static_cast<size_t>(length));
    env->GetByteArrayRegion(input, 0, length, reinterpret_cast<jbyte*>(bytes.data()));
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return env->NewStringUTF("-- Erro ao ler o arquivo.");
    }
    try {
        luaudec::Module module = luaudec::parseBytecode(bytes.data(), bytes.size());
        std::string source = luaudec::decompile(module, luaudec::LiftOptions{});
        return env->NewStringUTF(source.c_str());
    } catch (const std::exception& e) {
        return env->NewStringUTF((std::string("-- Não foi possível decompilar: ") + e.what()).c_str());
    } catch (...) {
        return env->NewStringUTF("-- Erro desconhecido durante a decompilação.");
    }
}
