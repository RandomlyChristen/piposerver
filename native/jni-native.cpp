//
// Created by 이수균 on 2022/05/16.
//

#include <string>

#include "opencv2/opencv.hpp"
#include "com_mobilex_piposerver_PiposerverApplication.h"
#include "com_mobilex_piposerver_service_PipoService.h"
#include "ImageProcessor.h"

ImageProcessor* imageProcessor;

void labels_to_contour(cv::Mat& labels, cv::Mat& contour_masked_out, uint32_t n_superpixels) {
    for (uint32_t sp_idx = 0; sp_idx < n_superpixels; ++sp_idx) {
        cv::Mat1b mask = cv::Mat1b::zeros(labels.size());

        for (int i = 0; i < labels.rows; ++i) {
            for (int j = 0; j < labels.cols; ++j) {
                if (labels.at<uint32_t>(i, j) != sp_idx) continue;
                mask.at<uint8_t>(i, j) = 1;
            }
        }

        std::vector<std::vector<cv::Point>> contours;
        cv::findContours(mask, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);
        for (int contour_idx = 0; contour_idx < contours.size(); ++contour_idx) {
            cv::drawContours(contour_masked_out, contours, contour_idx, {0, 0, 0});
        }
    }
}

std::string ImportationTest() {
    return "SUCCESS";
}

JNIEXPORT jstring JNICALL Java_com_mobilex_piposerver_PiposerverApplication_ImportationTest
        (JNIEnv * env, jclass) {
    return env->NewStringUTF(ImportationTest().c_str());
}

std::vector<std::string> process_pipo(const std::string& origin_path, const std::string& processed_path,
                                      const std::string& preview_path, bool test = false) {
    if (test) {
        std::cout << origin_path << '\n' << processed_path << '\n' << preview_path << std::endl;
    }
    imageProcessor = ImageProcessor::get_instance();
    cv::Mat origin_mat = cv::imread(origin_path), out_mat;
    cv::resize(origin_mat, origin_mat, cv::Size(600, 800));
    cv::Mat labels, merged_labels;
    std::vector<cv::Vec3b> palette;
    std::vector<std::string> color_names;
    cv::uint32_t n_superpixels = 10;
    double merge_threshold = 10;
    ImageProcessor::cluster_image(origin_mat, labels, ClusteringAlgorithm::SLICO,
                                  10, n_superpixels);
    ImageProcessor::cluster_region_merge(origin_mat, merged_labels, labels, n_superpixels,
                                         merge_threshold);
    imageProcessor->cluster_color_mapping(origin_mat, merged_labels, n_superpixels);
    if (test) {
        cv::imshow(preview_path, origin_mat);
    }
    else {
        cv::imwrite(preview_path, origin_mat);
    }
    imageProcessor->map_image(origin_mat, palette, color_names);
    cv::resize(origin_mat, origin_mat, origin_mat.size() * 2, 0, 0, cv::INTER_NEAREST);
    ImageProcessor::draw_index(origin_mat, out_mat, palette, 0.5, 14, 6);
    if (test) {
        cv::imshow(processed_path, out_mat);
    }
    else {
        cv::imwrite(processed_path, out_mat);
    }
    return color_names;
}

std::string jstring2string(JNIEnv *env, jstring jStr) {
    if (!jStr)
        return "";

    jclass stringClass = env->GetObjectClass(jStr);
    jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    auto stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, env->NewStringUTF("UTF-8"));

    auto length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte* pBytes = env->GetByteArrayElements(stringJbytes, nullptr);

    std::string ret = std::string((char *)pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}

#include <iostream>

JNIEXPORT void JNICALL Java_com_mobilex_piposerver_service_PipoService_processPipo
        (JNIEnv *env, jobject thiz, jstring originFile, jstring processedFile, jstring previewFile, jint difficulty) {
    std::string origin_path = jstring2string(env, originFile);
    std::string processed_path = jstring2string(env, processedFile);
    std::string preview_path = jstring2string(env, previewFile);
    auto colors = process_pipo(origin_path, processed_path, preview_path, false);
}