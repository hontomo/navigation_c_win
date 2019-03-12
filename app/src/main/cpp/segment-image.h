//
// Created by honto on 2018/05/25.
//

#ifndef NAVIGATION_C_WIN_SEGMENT_IMAGE_H
#define NAVIGATION_C_WIN_SEGMENT_IMAGE_H

#include<random>
#include<opencv2/opencv.hpp>
#include"segment-graph.h"
#include<algorithm>
#include<cmath>



// dissimilarity measure between pixels
static inline float diff(cv::Mat r, cv::Mat g, cv::Mat b,
                         int x1, int y1, int x2, int y2);


cv::Mat segment_image(cv::Mat im, float sigma, float c, int min_size,
                      int *num_ccs);


#endif //NAVIGATION_C_WIN_SEGMENT_IMAGE_H
