#include <jni.h>
#include <string>
#include<opencv2/opencv.hpp>
#include"segment-image.h"

using namespace cv;
using namespace std;

extern "C"
JNIEXPORT void JNICALL
Java_com_honto_navigation_1c_1win_MainActivity_OutputImage(
        JNIEnv* env,
        jobject /* this */,
        jlong inputAddr) {
    Mat &input = *(Mat *) inputAddr;
    cvtColor(input,input,CV_BGR2GRAY);
    Canny(input,input,50,100,3);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_honto_navigation_1c_1win_SubActivity_GrabCut(
        JNIEnv* env,
        jobject /* this */,
        jlong inputAddr,
        jlong maskAddr) {

    Mat &input = *(Mat *) inputAddr;
    Mat &mask = *(Mat *) maskAddr;


    /*Close contour*/
    Mat input_edge;
    cvtColor(input,input_edge,CV_BGR2GRAY);
    Canny(input_edge,input_edge,50,100,3);

    Mat edge_mask;
    input_edge.copyTo(edge_mask,mask);

    //resize
    resize(edge_mask,edge_mask,Size(),(float)1/2,(float)1/2,INTER_AREA);
    resize(mask,mask,Size(),(float)1/2,(float)1/2,INTER_AREA);

    morphologyEx(edge_mask,edge_mask,MORPH_CLOSE,Mat(),Point(-1,-1),3);

    Mat gc_mask = Mat(mask.rows,mask.cols,CV_8UC1);
    for(int x = 0;x< mask.cols;x++){
        for(int y=0;y<mask.rows;y++){
            if(mask.at<uchar>(y,x)==255)
                gc_mask.at<uchar>(y,x) = GC_PR_FGD;
            else
                gc_mask.at<uchar>(y,x) = GC_BGD;
        }
    }
    //flood fill/
    for(int x =0 ; x < edge_mask.cols;x+=500){
        floodFill(edge_mask,Mat(),Point(x,0),Scalar(255));
    }
    for(int x =0 ; x < edge_mask.cols;x+=500){
        floodFill(edge_mask,Mat(),Point(x, edge_mask.rows -1 ),Scalar(255));
    }
    for(int y =0 ; y < edge_mask.rows;y+=500){
        floodFill(edge_mask,Mat(),Point(0,y),Scalar(255));
    }
    for(int y =0 ; y < edge_mask.rows;y+=500){
        floodFill(edge_mask,Mat(),Point(edge_mask.cols - 1,y),Scalar(255));
    }

    for(int x = 0;x< edge_mask.cols;x++){
        for(int y=0;y<edge_mask.rows;y++){
            if(edge_mask.at<uchar>(y,x)==0)
                gc_mask.at<uchar>(y,x) = GC_FGD;
        }
    }
    /*Close contour*/

    /*GrabCut*/
    //resize
    Mat input_resize;
    resize(input,input_resize,Size(),(float)1/8,(float)1/8,INTER_AREA);
    resize(gc_mask,gc_mask,input_resize.size(),INTER_NEAREST);

    cvtColor(input_resize,input_resize,CV_RGBA2RGB);
    Mat bgModel,fgModel;
    Rect rect;
    grabCut(input_resize,gc_mask,rect,bgModel,fgModel,1,GC_INIT_WITH_MASK);

    Mat PR_mask;
    compare(gc_mask,GC_FGD,mask,CMP_EQ);
    compare(gc_mask,GC_PR_FGD,PR_mask,CMP_EQ);
    bitwise_or(mask,PR_mask,mask);
    morphologyEx(mask, mask, MORPH_OPEN, cv::Mat(), cv::Point(-1, -1), 3);
    morphologyEx(mask, mask, MORPH_CLOSE, cv::Mat(), cv::Point(-1, -1), 3);

    Mat output;// = Mat(input.rows,input.cols,CV_8UC1,Scalar(0));
    resize(mask,mask,input.size(),INTER_NEAREST);
    input.copyTo(output,mask);
    input = output;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_honto_navigation_1c_1win_SubActivity_ImageSegmentation(
        JNIEnv* env,
        jobject /* this */,
        jlong labelAddr) {

    Mat &label = *(Mat *) labelAddr;
    resize(label,label,Size(),(float)1/4,(float)1/4,INTER_AREA);
    float sigma=1;
    int num_css;
    int c = 6; //threshold
    int min_size = (int)((float)label.cols*label.rows/1000);
    int k = 300; //threshold

    Mat seg = segment_image(label,sigma,k,min_size,&num_css);
    label = seg;

}

extern "C"
JNIEXPORT void JNICALL
Java_com_honto_navigation_1c_1win_SubActivity_PassageWaySegment(
        JNIEnv* env,
        jobject /*this*/,
        jlong passageAddr,jlong labelAddr,jlong gbAddr,jint x,jint y) {

    Mat &label = *(Mat *) labelAddr;
    Mat &passage = *(Mat*)passageAddr;
    Mat &gb_img = *(Mat*)gbAddr;

    float comp = label.at<float>(y,x);

    //cvtColor(passage,passage,CV_RGBA2GRAY);
    for(int x_ = 0 ;x_< passage.cols;x_++){
        for (int y_ = 0; y_ < passage.rows;y_++){
            if(label.at<float>(y_,x_)==comp){
                if(passage.at<uchar>(y_,x_)==0)
                    passage.at<uchar>(y_,x_)=255;
                else if (passage.at<uchar>(y_,x_)==255)
                    passage.at<uchar>(y_,x_)=0;
            }
        }
    }

    cvtColor(gb_img,gb_img,CV_RGBA2RGB);
    Vec3b white = {255,255,255};
    for(int x_ = 0 ;x_< passage.cols;x_++) {
        for (int y_ = 0; y_ < passage.rows; y_++) {
            if(passage.at<uchar>(y_, x_) == 255) {
                gb_img.at<Vec3b>(y_, x_) = white;
            }
        }
    }



}
