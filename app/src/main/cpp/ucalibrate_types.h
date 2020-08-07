#ifndef UCALIBRATE_TYPES_H
#define UCALIBRATE_TYPES_H
#include <opencv2/opencv.hpp>

namespace uphoton {
namespace ucalibrate {

enum CameraDirectionTypes {
  HORIZONTAL            = 1,
  VERTICAL              = 2
};

struct ExtractPointsParams {
  float sigma1;
  float sigma2;
  int dog_blur_size;
  int dog_kernel_size;
  int peak_thresh;
};

struct RemapParams {
  ExtractPointsParams ir_extract_vga;
  ExtractPointsParams rgb_extract_1200;
  bool get_b_matrix;
  float distance_calib_board;
  float base_width;
  float ir_focal_length;
  float ir_pixel_size_vga;
  cv::Size2f calib_board_size;
  CameraDirectionTypes direction;
};

struct Error {
  float error_mean;
  float error_std_dev;
};

}  // namespace ucalibrate
}  // namespace uphoton
#endif // UCALIBRATE_TYPES_H
