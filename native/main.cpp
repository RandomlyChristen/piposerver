#include <iostream>
#include <algorithm>
#include "jni-native.cpp"
#include "opencv2/opencv.hpp"
#include "opencv2/ximgproc.hpp"
#include "ImageProcessor.h"

using namespace std;
using namespace cv;
using namespace cv::ml;
using namespace cv::ximgproc;

int main() {
    auto v = process_pipo("lupy.jpeg", "lupy_processed.jpeg", "lupy_preview.jpeg", false);
    for (int i = 0; i < v.size(); ++i) {
        cout << i << " : " << v[i] << endl;
    }
//    waitKey();

    return 0;

    imageProcessor = ImageProcessor::get_instance();

    while (true) {
        Mat img = imread("lupy.jpeg", IMREAD_COLOR);
        Mat origin;
        {
            resize(img, img, Size(600, 800));
//            imageProcessor->map_image(img);
            img.copyTo(origin);
            imshow("origin::img", img);
        }

        int region_size;
        uint32_t n_superpixels;
        double merge_threshold;
        cout << "region_size >> n_superpixels >> merge_threshold : ";
        cin >> region_size >> n_superpixels >> merge_threshold;

        Mat labels, merged_labels, contour_mask, normalized_labels, out;
        vector<Vec3b> palette;
        vector<string> color_names;

        {
            ImageProcessor::cluster_image(img, labels, ClusteringAlgorithm::SLICO,
                                          region_size, n_superpixels, &contour_mask);
//            cv::normalize(labels, normalized_labels, 0, 255, NORM_MINMAX);
//            normalized_labels.convertTo(normalized_labels, CV_8UC1);
//            imshow("cluster_image::img", img);
//            imshow("cluster_image::contour_mask", contour_mask);
//            imshow("cluster_image::normalized_labels", normalized_labels);

            Mat contour_masked;
            origin.copyTo(contour_masked);
            labels_to_contour(labels, contour_masked, n_superpixels);
            imshow("cluster_image::contour_masked", contour_masked);
        }

//        waitKey(10);
//        continue;

        {
            ImageProcessor::cluster_region_merge(img, merged_labels, labels, n_superpixels, merge_threshold);
//            cv::normalize(merged_labels, normalized_labels, 0, 255, NORM_MINMAX);
//            normalized_labels.convertTo(normalized_labels, CV_8UC1);
//            imshow("cluster_region_merge::img", img);
//            imshow("cluster_region_merge::normalized_labels", normalized_labels);

            Mat contour_masked;
            origin.copyTo(contour_masked);
            labels_to_contour(merged_labels, contour_masked, n_superpixels);
            imshow("cluster_region_merge::contour_masked", contour_masked);
        }

        {
            imageProcessor->cluster_color_mapping(img, merged_labels, n_superpixels);
            imshow("cluster_color_mapping::img", img);
        }

        {
            imageProcessor->map_image(img, palette, color_names);
            resize(img, img, img.size() * 2, 0, 0, INTER_NEAREST);
            ImageProcessor::draw_index(img, out, palette, 0.5, 14, 6);
            imshow("draw_index::out", out);
        }

        waitKey(10);
    }

    return 0;
}