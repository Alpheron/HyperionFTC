#include "opencv2/core.hpp"
#include <jni.h>
#include <opencv2/imgproc.hpp>
#include "Vision.h"

using namespace cv;
using namespace std;

extern "C"
JNIEXPORT void JNICALL
Java_org_firstinspires_ftc_robotcontroller_internal_Vision_ringStack(JNIEnv *env, jclass type, jlong addrRgba) {
    Mat &img = *(Mat *) addrRgba;
    cvtColor(img, img, COLOR_RGB2BGR);
    tuple<Mat, Mat> images = preProcess(img);
    Mat imageHSVMasked = getHSVImage(get<0>(images));
    Mat imageYCrCbMasked = getYCrCbImage(imageHSVMasked);
    img = postProcessImg(imageYCrCbMasked, images);
}