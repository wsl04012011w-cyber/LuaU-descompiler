cmake_minimum_required(VERSION 3.22.1)
project(luaureadable LANGUAGES CXX)
add_library(luaureadable SHARED native-lib.cpp)
target_compile_features(luaureadable PRIVATE cxx_std_17)
