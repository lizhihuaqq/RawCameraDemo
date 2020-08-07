#ifndef UIMGPROC_H
#define UIMGPROC_H

#include <opencv2/opencv.hpp>

#define UIMGPROC_VERSION          "1.0.0"

namespace uphoton {
namespace uimgproc {

  bool AdjustBrightness(const cv::Mat &image_in,
                        cv::Mat &image_out,
                        const int &brightness);
  void AdjustContrast(const cv::Mat& image_in,
                      cv::Mat& image_out,
                      const float& contrast);
}  // namespace uimgproc
}  // namespace uphoton

#endif // UIMGPROC_H
