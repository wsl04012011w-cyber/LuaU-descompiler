# LuauReadable

Aplicativo Android offline para analisar arquivos Luau bytecode que você tem autorização para examinar.

## Arquitetura
- Android nativo com interface local (sem servidor obrigatório).
- C++ integrado via Android NDK/CMake e JNI.
- GitHub Actions compila o APK e publica o artefato.

## Build
O workflow **Build Android APK** compila o APK no GitHub Actions. Abra a execução em **Actions** e baixe o artefato `LuauReadable-debug-apk`.

## Estado atual
Este é o esqueleto inicial: abre offline e confirma que a ponte JNI/C++ foi carregada. O motor real de decompilação ainda precisa ser integrado e testado com arquivos de exemplo autorizados.
