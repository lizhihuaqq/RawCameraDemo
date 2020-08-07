# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

#工程路径
set(pathToProject F:/Tencent/Demo/demo/MyDemo/8/collect/8.3/RawCameraDemo)
#OpenCV-android-sdk路径
set(pathToOpenCV F:/demo/Code/SDK)

# CMake版本信息
cmake_minimum_required(VERSION 3.4.1)

#支持-std=gnu++11
set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "-std=c++11 -O3")
#配置加载native头文件
include_directories(${pathToOpenCV}/native/jni/include)
#CPP文件夹下带编译的cpp文件
add_library(native-lib SHARED src/main/cpp/native-lib.cpp)
#动态方式加载
add_library(lib_opencv SHARED IMPORTED)
add_library( lib_UCalibrate SHARED IMPORTED )
add_library( lib_UDepthMap SHARED IMPORTED )
add_library( lib_UImgproc SHARED IMPORTED )




#导入so文件
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${pathToProject}/app/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)
set_target_properties(lib_UCalibrate PROPERTIES IMPORTED_LOCATION ${pathToProject}/app/src/main/jniLibs/${ANDROID_ABI}/libUCalibrate.so)
set_target_properties(lib_UDepthMap PROPERTIES IMPORTED_LOCATION ${pathToProject}/app/src/main/jniLibs/${ANDROID_ABI}/libUDepthMap.so)
set_target_properties(lib_UImgproc PROPERTIES IMPORTED_LOCATION ${pathToProject}/app/src/main/jniLibs/${ANDROID_ABI}/libUImgproc.so)

#C++日志
find_library(log-lib log)
target_link_libraries(native-lib ${log-lib} lib_opencv)
target_link_libraries( native-lib ${log-lib} lib_UCalibrate )
target_link_libraries( native-lib ${log-lib} lib_UDepthMap )
target_link_libraries( native-lib ${log-lib} lib_UImgproc )



