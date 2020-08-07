#ifndef UDEPTHMAP_TYPES_H
#define UDEPTHMAP_TYPES_H
#include <opencv2/core/core.hpp>
#include <opencv2/opencv.hpp>

namespace uphoton {
namespace udepthmap {

enum ImageSampleTypes {
  SAMPLE_NUM_32           = 32,   // 深度图输出分辨率: 32pixel*32pixel
  SAMPLE_NUM_64           = 64,   // 深度图输出分辨率: 64pixel*64pixel
  SAMPLE_NUM_128          = 128,  // 深度图输出分辨率: 128pixel*128pixel
  SAMPLE_NUM_ROI          = 513,  // 深度图输出分辨率: 同roi尺寸
  SAMPLE_NUM_ROI_COPY     = 514,  // 深度图输出分辨率: 全尺寸，采用SAMPLE_NUM_ROI方式处理后贴回原图
};

enum GrayColorTypes {
  MEAN_WHITE2BLACK        = 0,
  MEAN_BLACK2WHITE        = 1,
  LOG_WHITE2BLACK         = 2,
  LOG_BLACK2WHITE         = 3
};

enum PseudoColorTypes {
  COLOR_TURBO             = 0,
  COLOR_YELLOW            = 1,
  COLOR_GRAY              = 2,
//  COLOR_VIRIDIS           = 3
};

enum VSearchRanges {
  VERTICAL_RANGE_0        = 0,
  VERTICAL_RANGE_1        = 1,
  VERTICAL_RANGE_2        = 2,
  VERTICAL_RANGE_3        = 3
};

enum MatchWinSizes {
  MATCH_WINSIZE_11         = 11,
  MATCH_WINSIZE_13         = 13,
  MATCH_WINSIZE_15         = 15,
  MATCH_WINSIZE_17         = 17,
  MATCH_WINSIZE_19         = 19,
  MATCH_WINSIZE_21         = 21,
  MATCH_WINSIZE_31         = 31
};

enum CostWinSizes {
  COST_WINSIZE_3           = 3,
  COST_WINSIZE_5           = 5,
  COST_WINSIZE_7           = 7,
  COST_WINSIZE_9           = 9,
  COST_WINSIZE_11          = 11
};

enum HoleWinRadius {
  HOLE_WINRADIUS_1         = 1,
  HOLE_WINRADIUS_2         = 2,
  HOLE_WINRADIUS_3         = 3,
  HOLE_WINRADIUS_7         = 7
};

enum FilterTypes {
  FILTER_MEAN              = 1,
  FILTER_NONE              = 2
};

enum CostCalculateTypes {
  NCC                      = 1,
  NCC_MINI                 = 2,
  ZNCC                     = 3
};

// 预处理参数
struct InitParams {
  float reference_depth;        // 参考距离，单位：cm

  float focal_length;           // 焦距，单位：mm
  float base_width;             // 基线距离，单位：mm
  float pixel_size;             // 单个像素尺寸，单位：mm
  int disparity_period_DOE;     // 周期，单位:像素

  float depth_detection_min;    // 深度图最小检测距离，单位：cm
  float depth_detection_max;    // 深度图最大检测距离，单位：cm
  bool horizontal_flip;         // 目标图参考图水平镜像标志位
};

struct CalculateSettings {
  cv::Rect roi_bbox;
  bool depth_estimate = false;
  float depth_estimate_forward;
  float depth_estimate_backward;

  ImageSampleTypes output_sample;
  MatchWinSizes match_win_size;
  VSearchRanges vertical_search_range;
  FilterTypes filter_type;
  CostCalculateTypes cost_cal_type;

  bool hole_fill_flag = false;
  HoleWinRadius hole_win_radius = HOLE_WINRADIUS_3;
  CostWinSizes cost_filter_size = COST_WINSIZE_5;
  bool post_process = false;
  bool compensate_ref_flag = false;
};

/**@struct DepthMapResults
* @brief 深度图计算结果结构体 \n
* 定义存储深度图计算结果结构体 \n
*/
struct DepthMapResults {
  cv::Mat depth_map;            ///< 输出的深度图，类型：16UC1，精度：0.1mm
  float depth_center;           ///< 深度图图像中心处深度,类型：float，单位：cm
};

// color map的显示设置
struct GrayColorMap {
  uint range;                   ///< 显示人脸前后range内的深度，单位：cm
  uchar min;                    ///< 设置color map中最小的像素值
  uchar max;                    ///< 设置color map中最大的像素值
  uchar background;             ///< 设置color map的背景像素值
  float depth_base = -1.0f;
  GrayColorTypes type;
};

}  // namespace udepthmap
}  // namespace uphoton

#endif // UDEPTHMAP_TYPES_H
