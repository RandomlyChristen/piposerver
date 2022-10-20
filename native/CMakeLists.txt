cmake_minimum_required(VERSION 3.10.2)
project('piposerver')
set(CMAKE_CXX_STANDARD 17)
include_directories(/usr/local/include)

# OpenCV
find_package(OpenCV)
message(STATUS "OpenCV_INCLUDE_DIRS = ${OpenCV_INCLUDE_DIRS}")
message(STATUS "OpenCV_LIBS = ${OpenCV_LIBS}")
include_directories( ${OpenCV_INCLUDE_DIRS} )

# Native Lib & Link OpenCV
add_library(image-processor STATIC ImageProcessor.cpp)
add_library(native-lib SHARED jni-native.cpp)
target_link_libraries(native-lib ${OpenCV_LIBS} image-processor)

# JNI
include_directories(/Library/Java/JavaVirtualMachines/jdk1.8.0_192.jdk/Contents/Home/include)
include_directories(/Library/Java/JavaVirtualMachines/jdk1.8.0_192.jdk/Contents/Home/include/darwin)

# Test main
add_executable(piposerver main.cpp)
target_link_libraries(piposerver native-lib)