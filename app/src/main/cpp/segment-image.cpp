//
// Created by honto on 2018/05/25.
//

#include"segment-image.h"
#include <time.h>
//#include<direct.h>

using namespace std;
using namespace cv;


// dissimilarity measure between pixels
static inline float diff(cv::Mat r, cv::Mat g, cv::Mat b,
                         int x1, int y1, int x2, int y2) {
    return abs(r.at<float>(y1, x1) - r.at<float>(y2, x2)) + abs(g.at<float>(y1, x1) - g.at<float>(y2, x2)) +
           abs(b.at<float>(y1, x1) - b.at<float>(y2, x2));

}

/*
* Segment an image
*
* Returns a color image representing the segmentation.
*
* im: image to segment.
* sigma: to smooth the image.
* c: constant for treshold function.
* min_size: minimum component size (enforced by post-processing stage).
* num_ccs: number of connected components in the segmentation.
*/

cv::Mat segment_image(cv::Mat im, float sigma, float c, int min_size,
                      int *num_ccs) {
    int width = im.cols;
    int height = im.rows;


    cv::Mat r, g, b,im2;
    std::vector<cv::Mat>planes,planes2;
    cv::split(im, planes);
    r = planes[2]; g = planes[1]; b = planes[0];
    r.convertTo(r, CV_32FC1); g.convertTo(g, CV_32FC1); b.convertTo(b, CV_32FC1);

    // smooth each color channel
    cv::Mat smooth_r;// = cv::Mat(cv::Size(width, height), CV_32FC1);
    cv::Mat smooth_g, smooth_b;


    cv::GaussianBlur(r, smooth_r, cv::Size(), sigma);
    cv::GaussianBlur(g, smooth_g, cv::Size(), sigma);
    cv::GaussianBlur(b, smooth_b, cv::Size(), sigma);


    // build graph
    edge *edges = new edge[width*height * 4];
    int num = 0;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            if (r.at<float>(y, x) == 0 & g.at<float>(y, x) == 0 & b.at<float>(y, x) == 0){

            }
            else{
                if (x < width - 1) {
                    edges[num].a = y * width + x;
                    edges[num].b = y * width + (x + 1);
                    edges[num].w = diff(smooth_r, smooth_g, smooth_b, x, y, x + 1, y);
                    num++;
                }

                if (y < height - 1) {
                    edges[num].a = y * width + x;
                    edges[num].b = (y + 1) * width + x;
                    edges[num].w = diff(smooth_r, smooth_g, smooth_b, x, y, x, y + 1);
                    num++;
                }

                if ((x < width - 1) && (y < height - 1)) {
                    edges[num].a = y * width + x;
                    edges[num].b = (y + 1) * width + (x + 1);
                    edges[num].w = diff(smooth_r, smooth_g, smooth_b, x, y, x + 1, y + 1);
                    num++;
                }

                if ((x < width - 1) && (y > 0)) {
                    edges[num].a = y * width + x;
                    edges[num].b = (y - 1) * width + (x + 1);
                    edges[num].w = diff(smooth_r, smooth_g, smooth_b, x, y, x + 1, y - 1);
                    num++;
                }
            }
        }
    }
    // segment
    universe *u = segment_graph(width*height, num, edges, c);


    // post process small components
    for (int i = 0; i < num; i++) {
        int a = u->find(edges[i].a);
        int b = u->find(edges[i].b);
        if ((a != b) && ((u->size(a) < min_size) || (u->size(b) < min_size)))
            u->join(a, b);
    }
    delete[] edges;

    *num_ccs = u->num_sets();

    cv::Mat output;
    output = cv::Mat(height, width, CV_8UC3);

    // pick random colors for each component
    uchar *colorsR = new uchar[width*height];
    uchar *colorsG = new uchar[width*height];
    uchar *colorsB = new uchar[width*height];

    for (int i = 0; i < width*height; i++){
        colorsR[i] = (uchar)rand();
        colorsG[i] = (uchar)rand();
        colorsB[i] = (uchar)rand();
    }

    cv::Mat label_matrix = cv::Mat(height, width, CV_32SC1);
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int comp = u->find(y * width + x);
            if (r.at<float>(y, x) == 0 && g.at<float>(y, x) == 0 && b.at<float>(y, x) == 0)
                label_matrix.at<int>(y, x) = -2;
            else if (u->size(comp) >= min_size){
                label_matrix.at<int>(y, x) = comp;
            }
            else label_matrix.at<int>(y, x) =  -1;
        }
    }


/*
    cv::Mat output2 = label_matrix.clone();
    cv::Mat compim;
    int pre_count = 0;
    std::vector<int> comps;
    int comp;
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            comp = abs(output2.at<int>(y, x));
            if (comp == 1){
                output.at<cv::Vec3b>(y, x)[0] = 255;
                output.at<cv::Vec3b>(y, x)[1] = 255;
                output.at<cv::Vec3b>(y, x)[2] = 255;
            }
            else{
                output.at<cv::Vec3b>(y, x)[0] = colorsB[comp];
                output.at<cv::Vec3b>(y, x)[1] = colorsG[comp];
                output.at<cv::Vec3b>(y, x)[2] = colorsR[comp];
            }
            comps.push_back(comp);
            //}
        }
    }
    sort(comps.begin(), comps.end());
    comps.erase(unique(comps.begin(), comps.end()), comps.end());
*/

    delete[] colorsR;
    delete[] colorsG;
    delete[] colorsB;

    delete u;

    //return output;
    return label_matrix;
}
