cmake_minimum_required(VERSION 3.22.1)
project(luaureadable LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(LUAUDEC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/luaudec)
set(LUAU_ROOT ${LUAUDEC_DIR}/third_party/luau)
add_library(luaureadable SHARED
 native-lib.cpp
 ${LUAUDEC_DIR}/src/bytecode/Bytecode.cpp
 ${LUAUDEC_DIR}/src/disasm/Disasm.cpp
 ${LUAUDEC_DIR}/src/cfg/CFG.cpp
 ${LUAUDEC_DIR}/src/lift/Lift.cpp
 ${LUAUDEC_DIR}/src/emit/Emit.cpp
)
target_include_directories(luaureadable PRIVATE ${LUAUDEC_DIR}/src ${LUAU_ROOT}/Common/include)
target_compile_features(luaureadable PRIVATE cxx_std_17)
target_compile_options(luaureadable PRIVATE -Wall -Wextra -Wno-unused-parameter)
