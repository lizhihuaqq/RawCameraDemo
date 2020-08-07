#ifndef UDEPTHMAP_H
#define UDEPTHMAP_H
#include "udepthmap_types.h"

#define UDEPTH_VERSION         "3.0.2"

namespace uphoton {
namespace udepthmap {

extern "C" {

/**
 * @brief 将ushort类型的raw数据保存成cv::Mat形式
 * @param[in] raw               内存中的raw数据
 * @param[in] mat               cv::Mat类型数据
 * @param[in] width             raw数据宽度
 * @param[in] height            raw数据高度
 * @return  bool
 * - true      数据类型转换成功
 * - false     数据类型转换失败
 */
bool LoadMatFromRawBytes(const ushort *raw,
                         cv::Mat &mat,
                         const int &width,
                         const int &height);
/**@brief 参考图预处理
* @param[in]  ref_image            参考图
* @param[in]  parameter            预处理设置参数
* @return  void
*/
void Preprocess(const cv::Mat &ref_image,
                const InitParams &parameter);

/**@brief 计算深度图
* @param[in]  obj_image            目标图
* @param[out] results              输出的结果
* @param[in]  settings             设置参数
* @return  bool
* - true      计算成功
* - false     计算失败
*/
bool CalculateDepthMap(const cv::Mat &obj_image,
                       DepthMapResults &results,
                       const CalculateSettings& settings);

/**@brief 将深度图保存为二进制文件
* @param[in]  depth_map            深度图
* @param[in]  file                 文件保存路径
* @return  bool
* - true      保存成功
* - false     保存失败
*/
bool SaveDepthToFile(const cv::Mat &depth_map,
                     const char *file);

/**@brief 将表示真实距离的深度图映射为用于显示的灰度图
* @param[in]  depth_map            深度图
* @param[out] color_map            灰度图
* @param[in]  params               显示设置
* @return  void
*/
void GetDepthGrayColorMap(const cv::Mat &depth_map,
                          cv::Mat &color_map,
                          const GrayColorMap &params);

/**@brief 将表示真实距离的深度图映射为用于显示的伪彩图
* @param[in]  depth_map            深度图
* @param[out] color_map            伪彩图
* @param[in]  params               显示设置
* @return  void
*/
void GetDepthPseudoColorMap(const cv::Mat &depth_map,
                            cv::Mat &color_map,
                            const PseudoColorTypes &types,
                            const uchar &min,
                            const uchar &max);

}

}  // namespace udepthmap
}  // namespace uphoton

#endif // UDEPTHMAP_H
