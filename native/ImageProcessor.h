//
// Created by 이수균 on 2022/05/16.
//

#ifndef PIPOSERVER_IMAGEPROCESSOR_H
#define PIPOSERVER_IMAGEPROCESSOR_H
#include "opencv2/opencv.hpp"
#include <vector>

enum ClusteringAlgorithm {
    DBSCAN, // n_superpixel must be determined
    LSC,    // region_size must be determined
    SLIC,   // region_size must be determined
    SLICO,  // region_size must be determined
    MSLIC   // region_size must be determined
};

class ImageProcessor {
private:
    cv::Vec3b color_map[256][256][256];
    const std::string* name_map[256][256][256];
    ImageProcessor();
    ImageProcessor(const ImageProcessor& other);
    ~ImageProcessor();

public:
    static ImageProcessor* get_instance() {
        static ImageProcessor ins;
        return &ins;
    }

    cv::Vec3b map_color(cv::uint8_t r, cv::uint8_t g, cv::uint8_t b);
    std::string map_name(cv::uint8_t r, cv::uint8_t g, cv::uint8_t b);
    void map_image(cv::Mat& rgb_img);
    void map_image(cv::Mat& rgb_img,
                   std::vector<cv::Vec3b>& palette_out, std::vector<std::string>& color_names_out);
    void cluster_color_mapping(cv::Mat& rgb, cv::Mat& labels, cv::uint32_t n_superpixel);

    static void cluster_image(cv::Mat& rgb, cv::Mat& labels_out, ClusteringAlgorithm algorithm,
                              int region_size, cv::uint32_t & n_superpixels, cv::Mat* contour_mask_out = nullptr);
    static void cluster_region_merge(cv::Mat& rgb, cv::Mat& new_labels_out, cv::Mat& old_labels, cv::uint32_t& n_superpixels, double thresh);
    static void draw_index(cv::Mat& rgb, cv::Mat& out, std::vector<cv::Vec3b>& palette, double font_scale = 0.25, int radius = 7, int padding = 3);
};

std::pair<cv::Vec3b, const std::string*> get_nearest(uint8_t r, uint8_t g, uint8_t b, bool deltaE);

#endif //PIPOSERVER_IMAGEPROCESSOR_H
