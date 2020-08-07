#ifndef UCALIBRATE_H
#define UCALIBRATE_H
#include <opencv2/opencv.hpp>
#include "ucalibrate_types.h"

#define UCALIBRATE_VERSION     "0.0.6"

namespace uphoton {
namespace ucalibrate {

extern "C" {

// 标定函数
bool GetStereoRemapMatrix(cv::Mat &P,
                          Error& error,
                          const cv::Mat &rgb_image,
                          const cv::Mat &ir_image,
                          const RemapParams &params,
                          const bool& debug);

bool GetPMatrix(cv::Mat& P,
                const std::vector<cv::Point2f>& camera_rgb_points,
                const std::vector<cv::Point2f>& camera_ir_points,
                const RemapParams& params);

bool RemapRoiBboxToIR(const cv::Rect &rgb_roi_bbox,
                      const cv::Size &rgb_image_size,
                      cv::Rect& ir_roi_bbox,
                      const CameraDirectionTypes direction,
                      const cv::Mat &P,
                      const double distance = 0.5);

bool SavePMatrix(const char *path, const cv::Mat& P);

bool ReadPMatrix(const char *path, cv::Mat &P);

bool GetInverseProjectPoints(const std::vector<cv::Point> &ir_points,
                             std::vector<cv::Point2f> &rgb_points,
                             const cv::Mat &P, const float &depth = 0.5);

bool PreprocessRemappedRGB2IRVGA(const cv::Mat &P,
                                 const CameraDirectionTypes &direction);

bool GetRemappedRGBImage2IRVGA(const cv::Mat &rgb_image,
                               cv::Mat &remapped_rgb_image,
                               const cv::Mat &P,
                               const CameraDirectionTypes &direction);

bool GetRemapMatrixError(Error &error,
                         const cv::Mat &rgb_image,
                         const cv::Mat &ir_image,
                         const cv::Mat &P,
                         const RemapParams &params);
// 通过rgb image的尺寸判断pad是横版和竖版
CameraDirectionTypes GetCameraDirectionType(const cv::Mat &rgb_image);
}

}  // namespace ucalibrate
}  // namespace uphoton

#endif // UCALIBRATE_H
