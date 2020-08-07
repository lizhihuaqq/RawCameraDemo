#include <jni.h>
#include <string.h>
#include <opencv2/opencv.hpp>
#include <ctime>
#include <cstdlib>
#include <android/log.h>
#include <math.h>
#include <unistd.h>
#include "ucalibrate.h"
#include "udepthmap_types.h"
#include "udepthmap.h"
#include "uimgproc.h"


// log标签
#define  TAG    "native-lib"
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
// 定义debug信息
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
// 定义error信息
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

using namespace std;
using namespace cv;

//人脸检测器
static cv::CascadeClassifier *face_detecter = nullptr;


extern "C"
JNIEXPORT jstring

JNICALL
Java_cc_uphoton_rawcamera_activity_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello I am from C++";
    return env->NewStringUTF(hello.c_str());
}
//预处理RGB和IR的对齐函数
extern "C"
JNIEXPORT void
        JNICALL
Java_cc_uphoton_rawcamera_activity_MainActivity_PreprocessRemappedRGB2IR(JNIEnv
* env,
jclass type, jstring
ymlPath){
//读取YML文件得P矩阵
cv::Mat P;
const char *nativepath = env->GetStringUTFChars(ymlPath, 0);
cv::FileStorage storage(nativepath, cv::FileStorage::READ);
P = storage["P"].mat();
storage.

release();

env->
ReleaseStringUTFChars(ymlPath, nativepath
);
// 预处理PreprocessRemappedRGB2IRVGA
uphoton::ucalibrate::PreprocessRemappedRGB2IRVGA(P, uphoton::ucalibrate::VERTICAL
);
}



extern "C"
JNIEXPORT void
        JNICALL
Java_cc_uphoton_rawcamera_activity_MainActivity_GetRemappedRGBImage2IRVGA(JNIEnv
* env,
jclass type, jstring
ymlPath,
jstring src_rgb_path, jstring
des_rgb_path,
jstring irPath, jstring
mixPath ){
//读取YML文件得P矩阵
cv::Mat P;
const char *nativepath = env->GetStringUTFChars(ymlPath, 0);
cv::FileStorage storage(nativepath, cv::FileStorage::READ);
P = storage["P"].mat();
storage.

release();

env->
ReleaseStringUTFChars(ymlPath, nativepath
);

uint64_t start_time, end_time;
struct timeval tv;
gettimeofday(&tv,
nullptr);
start_time = static_cast<uint64_t>(tv.tv_sec) * 1000000 + tv.tv_usec;
//读取原图并resize成VGA
cv::Mat matched_image;
cv::Size vga_size(480, 640);  // 480宽度, 640高度
const char *srcRgbNativePath = env->GetStringUTFChars(src_rgb_path, 0);
const char *desRgbNativePath = env->GetStringUTFChars(des_rgb_path, 0);
cv::Mat rgb_image = cv::imread(srcRgbNativePath);
cv::resize(rgb_image, rgb_image, vga_size
);
uphoton::ucalibrate::GetRemappedRGBImage2IRVGA(rgb_image, matched_image, P, uphoton::ucalibrate::VERTICAL
);
cv::imwrite(desRgbNativePath, matched_image
);


//IRpath 和mixPath 都不为空时，可以出Mix后的图if(irPath.is&&mixPath!=null)
//const char *irNativePath = env->GetStringUTFChars(irPath, 0);
//cv::Mat ir_image_unified = cv::imread(irNativePath);
//cv::Mat add_image;
//cv::addWeighted(ir_image_unified, 0.9, matched_image, 0.2, 1, add_image);
//const char *mixNativePath = env->GetStringUTFChars(mixPath, 0);
//cv::imwrite(mixNativePath, add_image);

env->
ReleaseStringUTFChars(src_rgb_path, srcRgbNativePath
);
env->
ReleaseStringUTFChars(des_rgb_path, desRgbNativePath
);
gettimeofday(&tv,
nullptr);
end_time = static_cast<uint64_t>(tv.tv_sec) * 1000000 + tv.tv_usec;
double time = (end_time - start_time) / 1000.0;
//LOGD("lzh--C 对齐耗时--%f", time);
}


extern "C"
JNIEXPORT void
        JNICALL
Java_cc_uphoton_rawcamera_activity_MainActivity_PreprocessRaw(JNIEnv
* env,
jclass type, jbyteArray
ref_data){

cv::Mat reference_image;
jbyte *ref_native_data = env->GetByteArrayElements(ref_data, 0);
uphoton::udepthmap::LoadMatFromRawBytes((ushort*)ref_native_data,reference_image,1088,1280);
struct uphoton::udepthmap::InitParams parameter;
parameter.
reference_depth = 50;              // 设置参考图距离，单位cm，根据需要修改
parameter.
focal_length = 2.74f;              // ir摄像头焦距，单位mm
parameter.
base_width = 40.0f;                // 基线距离，单位mm
parameter.
pixel_size = 0.0054f;              // 像素大小，单位mm
parameter.
depth_detection_min = 26;          // 设置的最小检测距离，单位cm
parameter.
depth_detection_max = 130;         // 设置的最大检测距离，单位cm
parameter.
disparity_period_DOE = 200;
parameter.
horizontal_flip = false;
uphoton::udepthmap::Preprocess(reference_image, parameter
);
}

extern "C"
JNIEXPORT void
        JNICALL
Java_cc_uphoton_rawcamera_activity_MainActivity_CalRawDepth(JNIEnv
* env,
jclass type, jbyteArray
obj_img,
jstring depth_dat, jstring
depth_img_path,jint iscompensate){

cv::Rect roi_bbox = {0, 0, 480, 640};  // VGA分辨率
cv::Mat object_image;
jbyte *obj_native_data = env->GetByteArrayElements(obj_img, 0);
uphoton::udepthmap::LoadMatFromRawBytes((ushort
*)obj_native_data,object_image,1088,1280);

// 设置计算深度图的参数
uphoton::udepthmap::CalculateSettings settings;
// 设置是否使用后处理
settings.
post_process = false;
// 如果使用后处理，设置是否进行孔洞填充
settings.
hole_fill_flag = false;
// 如果使用孔洞填充，设置孔洞填充窗口大小
settings.
hole_win_radius = uphoton::udepthmap::HOLE_WINRADIUS_3;
// 设置是否进行深度预估
settings.
depth_estimate = false;
// 如果使用深度预估，设置深度预估范围
settings.
depth_estimate_forward = -3;  // cm
settings.
depth_estimate_backward = 3;  // cm
// 设置ROI区域
settings.
roi_bbox = roi_bbox;
// 设置输出分辨率
settings.
output_sample = uphoton::udepthmap::SAMPLE_NUM_ROI;
// 设置匹配时垂直方向搜索范围
settings.
vertical_search_range = uphoton::udepthmap::VERTICAL_RANGE_0;
// 设置是否使用置信度滤波
settings.
filter_type = uphoton::udepthmap::FILTER_MEAN;
settings.
cost_filter_size = uphoton::udepthmap::COST_WINSIZE_5;
// 设置是否对参考图进行矫正，用于解决深度图失效
if(iscompensate==1){
settings.
compensate_ref_flag = true;
}else{
settings.
compensate_ref_flag = false;
}


/*******************************************
// 使用ZNCC方式计算置信度
settings.cost_cal_type = ZNCC;
// 设置匹配窗口大小
settings.match_win_size = MATCH_WINSIZE_13;
********************************************/

/*******************************************
// 使用NCC方式计算置信度
settings.cost_cal_type = NCC;
// 设置匹配窗口大小
settings.match_win_size = MATCH_WINSIZE_17;
********************************************/

// 使用NCC方式计算置信度
settings.
cost_cal_type = uphoton::udepthmap::NCC_MINI;
// 设置匹配窗口大小
settings.
match_win_size = uphoton::udepthmap::MATCH_WINSIZE_13;

uphoton::udepthmap::DepthMapResults depth_results;

uphoton::udepthmap::CalculateDepthMap(object_image, depth_results, settings
);
const char *native_dep_path = env->GetStringUTFChars(depth_dat, 0);
const char *native_dep_image_path = env->GetStringUTFChars(depth_img_path, 0);
uphoton::udepthmap::SaveDepthToFile(depth_results
.depth_map, native_dep_path);
env->
ReleaseStringUTFChars(depth_dat, native_dep_path
);

cv::Mat color_map;
// sdk提供三种伪彩方式：COLOR_TURBO，COLOR_YELLOW，COLOR_GRAY
uphoton::udepthmap::GetDepthPseudoColorMap(depth_results
.depth_map, color_map, uphoton::udepthmap::COLOR_YELLOW, 0, 255);
cv::imwrite(native_dep_image_path, color_map
);
env->
ReleaseStringUTFChars(depth_img_path, native_dep_image_path
);

}
extern "C"
JNIEXPORT void
        JNICALL
Java_cc_uphoton_rawcamera_activity_MainActivity_AdjustBrightnessAndContrast(JNIEnv
* env,
jclass type, jbyteArray
led_img,
jint brightness,jint contrast,jstring adjust_led_path){

cv::Mat led_image;
jbyte *led_native_data = env->GetByteArrayElements(led_img, 0);
uphoton::udepthmap::LoadMatFromRawBytes((ushort
*)led_native_data,led_image,1088,1280);


led_image = led_image.colRange(60, 1020);
cv::resize(led_image, led_image, cv::Size(480, 640));

// 调节明亮度
cv::Mat processed_image;
// 输入是16位的图像  -100 150 -255-255
uphoton::uimgproc::AdjustBrightness(led_image, processed_image, brightness);
uphoton::uimgproc::AdjustContrast(processed_image, processed_image, contrast);
const char *native_adjust_led_path = env->GetStringUTFChars(adjust_led_path, 0);
cv::imwrite(native_adjust_led_path, processed_image);
//LOGD("lzh--adjust保存完毕--%d", 1111);

env->ReleaseStringUTFChars(adjust_led_path, native_adjust_led_path);

}



