#include "ImageProcessor.h"
#include "color.h"
#include <limits>
#include <cstdint>
#include <vector>
#include <cstring>
#include <string>
#include "opencv2/ximgproc.hpp"

cv::Vec3b ImageProcessor::map_color(uint8_t r, uint8_t g, uint8_t b) {
    return color_map[r][g][b];
}

std::string ImageProcessor::map_name(uint8_t r, uint8_t g, uint8_t b) {
    return *(name_map[r][g][b]);
}

void ImageProcessor::map_image(cv::Mat& rgb_img) {
    std::vector<cv::Vec3b> n;
    std::vector<std::string> m;
    map_image(rgb_img, n, m);
}

void ImageProcessor::map_image(cv::Mat& rgb_img,
                               std::vector<cv::Vec3b>& palette, std::vector<std::string>& color_names) {
    std::vector<std::vector<std::vector<bool>>> visit(256, std::vector<std::vector<bool>>(
                                                      256, std::vector<bool>(
                                                      256, false)));
    palette.clear(), color_names.clear();

    for (int i = 0; i < rgb_img.rows; ++i) {
        for (int j = 0; j < rgb_img.cols; ++j) {
            auto& pixel = rgb_img.at<cv::Vec3b>(i, j);
            pixel = map_color(pixel[0], pixel[1], pixel[2]);

            if (visit[pixel[0]][pixel[1]][pixel[2]]) continue;
            visit[pixel[0]][pixel[1]][pixel[2]] = true;

            auto name = map_name(pixel[0], pixel[1], pixel[2]);
            palette.push_back(pixel);
            color_names.push_back(name);
        }
    }
}

void ImageProcessor::cluster_image(cv::Mat& rgb, cv::Mat& labels_out, ClusteringAlgorithm algorithm,
                                   int region_size, cv::uint32_t & n_superpixels, cv::Mat* contour_mask_out) {
    cv::GaussianBlur(rgb, rgb, cv::Size(3, 3), 1);
    cv::Mat lab;
    cv::cvtColor(rgb, lab, cv::COLOR_RGB2Lab);

    switch (algorithm) {
        case DBSCAN: {
            auto cluster = cv::ximgproc::createScanSegment(rgb.cols, rgb.rows, (int) n_superpixels);
            cluster->iterate(lab);
            n_superpixels = cluster->getNumberOfSuperpixels();
            cluster->getLabels(labels_out);
            if (contour_mask_out) {
                cluster->getLabelContourMask(*contour_mask_out);
            }
        } break;
        case LSC: {
            auto cluster = cv::ximgproc::createSuperpixelLSC(lab, region_size);
            cluster->iterate();
            n_superpixels = cluster->getNumberOfSuperpixels();
            cluster->getLabels(labels_out);
            if (contour_mask_out) {
                cluster->getLabelContourMask(*contour_mask_out);
            }
        } break;
        case SLIC: {
            auto cluster = cv::ximgproc::createSuperpixelSLIC(lab, cv::ximgproc::SLICType::SLIC, region_size);
            cluster->iterate();
            n_superpixels = cluster->getNumberOfSuperpixels();
            cluster->getLabels(labels_out);
            if (contour_mask_out) {
                cluster->getLabelContourMask(*contour_mask_out);
            }
        } break;
        case SLICO: {
            auto cluster = cv::ximgproc::createSuperpixelSLIC(lab, cv::ximgproc::SLICType::SLICO, region_size);
            cluster->iterate();
            n_superpixels = cluster->getNumberOfSuperpixels();
            cluster->getLabels(labels_out);
            if (contour_mask_out) {
                cluster->getLabelContourMask(*contour_mask_out);
            }
        } break;
        case MSLIC: {
            auto cluster = cv::ximgproc::createSuperpixelSLIC(lab, cv::ximgproc::SLICType::MSLIC, region_size);
            cluster->iterate();
            n_superpixels = cluster->getNumberOfSuperpixels();
            cluster->getLabels(labels_out);
            if (contour_mask_out) {
                cluster->getLabelContourMask(*contour_mask_out);
            }
        } break;
    }
}

void ImageProcessor::cluster_region_merge(cv::Mat& rgb, cv::Mat& new_labels_out, cv::Mat& old_labels,
                                          cv::uint32_t& n_superpixel, double thresh) {
    static const int D[4][2] = {
            {0, 1}, {1, 0}, {-1, 0}, {0, -1}
    };
    // union-find, function inner class with single private field, parent
    class UnionFind {
        std::vector<cv::uint32_t> parent;
    public:
        explicit UnionFind(cv::uint32_t n_superpixel)
                : parent(std::vector<cv::uint32_t>(n_superpixel))
        {
            for (cv::uint32_t i = 0; i < n_superpixel; ++i) {
                parent[i] = i;
            }
        }
        cv::uint32_t find(cv::uint32_t v) {
            if (parent[v] == v) return v;
            return parent[v] = find(parent[v]);
        }
        void merge(cv::uint32_t v, cv::uint32_t u) {
            auto v_parent = find(v);
            auto u_parent = find(u);
            if (v_parent == u_parent) return;
            if (v > u)
                parent[u_parent] = v_parent;
            else
                parent[v_parent] = u_parent;
        }
    };

    std::vector<cv::MatND> hists(n_superpixel);
    std::vector<std::set<cv::uint32_t>> neighbours(n_superpixel);

    cv::Mat cmp;
    cv::cvtColor(rgb, cmp, cv::COLOR_RGB2Lab);
    int channels[3] = {0, 1, 2};
    int histSize[3] = {8, 8, 8};
    float range[2] = {0, 256};
    const float* ranges[3] = {range, range, range};

    // find neighbour(graphing) and histogram calculation for each superpixels
    for (cv::uint32_t sp_idx = 0; sp_idx < n_superpixel; ++sp_idx) {
        cv::Mat1b mask = cv::Mat1b::zeros(rgb.size());

        for (int i = 0; i < old_labels.rows; ++i) {
            for (int j = 0; j < old_labels.cols; ++j) {
                if (old_labels.at<cv::uint32_t>(i, j) != sp_idx) continue;

                for (auto& d : D) {
                    int adj_i = i + d[0];
                    int adj_j = j + d[1];
                    if (adj_i < 0 || adj_j < 0 || adj_i >= old_labels.rows || adj_j >= old_labels.cols)
                        continue;

                    cv::uint32_t adj_sp = old_labels.at<cv::uint32_t>(adj_i, adj_j);
                    if (adj_sp != sp_idx) {
                        neighbours[sp_idx].insert(adj_sp);
                    }
                }

                mask.at<cv::uint8_t>(i, j) = 1;
            }
        }

        cv::calcHist(&cmp, 1, channels, mask, hists[sp_idx],
                     3, histSize, ranges);
        cv::normalize(hists[sp_idx], hists[sp_idx], 0, 1, cv::NORM_MINMAX);
    }

    UnionFind union_find(n_superpixel);
    std::vector<bool> visit(n_superpixel, false);
    std::queue<cv::uint32_t> q;
    q.push(0), visit[0] = true;

    // merge with union-find by comparison of histogram during BFS
    while (!q.empty()) {
        auto front = q.front();
        q.pop();
        for (auto adj_sp : neighbours[front]) {
            if (adj_sp < 0 || adj_sp >= n_superpixel) continue;
            if (visit[adj_sp]) continue;
            q.push(adj_sp), visit[adj_sp] = true;

            double hist_cmp = cv::compareHist(hists[front], hists[adj_sp], cv::HISTCMP_CORREL);
//            hist_cmp = cv::abs(hist_cmp);

            if (hist_cmp >= thresh) {
                union_find.merge(front, adj_sp);
            }
        }
    }

    // map new label with coordinate compression
    std::vector<cv::uint32_t> old_idxes;
    old_idxes.reserve(n_superpixel);
    for (cv::uint32_t old_idx = 0; old_idx < n_superpixel; ++old_idx) {
        old_idxes.push_back(union_find.find(old_idx));
    }
    sort(old_idxes.begin(), old_idxes.end());
    old_idxes.erase(std::unique(old_idxes.begin(), old_idxes.end()), old_idxes.end());

    std::map<cv::uint32_t, cv::uint32_t> idx_map;

    for (cv::uint32_t new_idx = 0; new_idx < old_idxes.size(); ++new_idx) {
        idx_map[old_idxes[new_idx]] = new_idx;
    }

    new_labels_out = cv::Mat(old_labels.size(), old_labels.type());
    for (int i = 0; i < old_labels.rows; ++i) {
        for (int j = 0; j < old_labels.cols; ++j) {
            cv::uint32_t old = old_labels.at<cv::uint32_t>(i, j);
            if (old < 0 || old >= n_superpixel) continue;

            new_labels_out.at<cv::uint32_t>(i, j) =
                    idx_map[union_find.find(old)];
        }
    }

    n_superpixel = old_idxes.size();
}

void ImageProcessor::cluster_color_mapping(cv::Mat& rgb, cv::Mat& labels, cv::uint32_t n_superpixel) {
    for (cv::uint32_t sp_idx = 0; sp_idx < n_superpixel; ++sp_idx) {
        cv::Mat1b mask = cv::Mat1b::zeros(rgb.size());

        for (int i = 0; i < labels.rows; ++i) {
            for (int j = 0; j < labels.cols; ++j) {
                if (labels.at<cv::uint32_t>(i, j) != sp_idx) continue;
                mask.at<cv::uint8_t>(i, j) = 1;
            }
        }

        auto mean = cv::mean(rgb, mask);
        auto mapped_color = map_color(
                cvRound(mean[0]), cvRound(mean[1]), cvRound(mean[2]));
        rgb.setTo(mapped_color, mask);
    }
}

void ImageProcessor::draw_index(cv::Mat& rgb, cv::Mat& out, std::vector<cv::Vec3b>& palette,
                                double font_scale, int radius, int padding) {
    const cv::Vec3b white{255, 255, 255};
    out = cv::Mat(rgb.size(), rgb.type(), white);

    for (int color_idx = 0; color_idx < palette.size(); ++color_idx) {
        cv::Mat mask = cv::Mat::zeros(rgb.size(), CV_8UC1);
        cv::Mat indexed = cv::Mat(rgb.size(), rgb.type(), white);

        const cv::Size text_size = cv::getTextSize(std::to_string(color_idx), cv::FONT_HERSHEY_SIMPLEX,
                                                   font_scale, 1, nullptr);
        const auto text_color = palette[color_idx] * 0.6 + white * 0.3;

        for (int i = 0; i < rgb.rows; ++i) {
            for (int j = 0; j < rgb.cols; ++j) {
                auto& pixel = rgb.at<cv::Vec3b>(i, j);
                if (palette[color_idx] == pixel) {
                    mask.at<uchar>(i, j) = 255;
                }
            }
        }

        for (int i = 0; i < rgb.rows; i += ((radius * 2) + padding)) {
            for (int j = 0; j < rgb.cols; j += ((radius * 2) + padding)) {
                cv::circle(indexed, {j, i}, radius, text_color);
                cv::Point text_origin(j - (text_size.width / 2), i + (text_size.height / 2));
                cv::putText(indexed, std::to_string(color_idx), text_origin,
                            cv::FONT_HERSHEY_SIMPLEX, font_scale, text_color, 1);
            }
        }

        cv::copyTo(indexed, out, mask);

        std::vector<std::vector<cv::Point>> contours;
        cv::findContours(mask, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);
        for (int contour_idx = 0; contour_idx < contours.size(); ++contour_idx) {
            cv::drawContours(out, contours, contour_idx, white * 0.5);
        }
    }
}

const int COLOR_N = 61;
const std::string COLORS_NAME[] = {
        "502 ALIZARIN CRIMSON",
        "503 CARMINE",
        "504 NAPHTHOL RED LIGHT",
        "506 CORAL RED",
        "543 SHELL PINK",
        "592 CADMIUM RED LIGHT",
        "505 VERMILION HUE",
        "593 CADMIUM ORANGE",
        "510 PERMANENT ORANGE",
        "511 PERMANENT YELLOW ORANGE",
        "595 CADMIUM YELLOW",
        "515 PERMANENT YELLOW DEEP",
        "516 PERMANENT YELLOW LIGHT",
        "517 LEMON YELLOW",
        "518 NAPLES YELLOW",
        "519 JAUNE BRILLIANT",
        "529 OLIVE GREEN",
        "525 BRILLIANT YELLOW GREEN",
        "526 PERMANENT GREEN LIGHT",
        "597 CADMIUM GREEN LIGHT",
        "599 CADMIUM GREEN",
        "527 SAP GREEN",
        "528 HOOKERS GREEN",
        "530 VIRIDIAN HUE",
        "523 EMERALD GREEN",
        "524 BRIGHT AQUA GREEN",
        "535 PERMANENT BLUE LIGHT",
        "536 BRILLIANT BLUE",
        "532 CERULEAN BLUE",
        "537 CERULEAN BLUE HUE",
        "548 LIGHT BLUE VIOLET",
        "533 COBALT BLUE",
        "538 COBALT BLUE HUE",
        "539 ULTRAMARINE BLUE",
        "540 PHTHALOCYANINE BLUE",
        "545 PERMANENT VIOLET",
        "546 BRILLIANT PURPLE",
        "544 LILAC",
        "547 MIDIUM MAGENTA",
        "542 MAGENTA",
        "549 COMPOSE ROSE",
        "554 BROWN RED",
        "553 BURNT SIENNA",
        "552 RAW SIENNA",
        "556 BURNT UMBER",
        "557 VANDYKE BROWN",
        "555 RAW UMBER",
        "551 YELLOW OCHRE",
        "560 BLACK",
        "561 FRENCH GREY 1",
        "562 TITANIUM WHITE",
        "563 PEARL WHITE",
        "564 PEARL GOLD",
        "565 GOLD",
        "566 SILVER",
        "567 COPPER",
        "571 FLUORESCENT MAGENTA",
        "572 FLUORESCENT ORANGE",
        "573 FLUORESCENT LEMON",
        "574 FLUORESCENT GREEN",
        "575 FLUORESCENT PINK"
};
const cv::Vec3b COLORS_RGB[] = {
        C502_ALIZARIN_CRIMSON,
        C503_CARMINE,
        C504_NAPHTHOL_RED_LIGHT,
        C506_CORAL_RED,
        C543_SHELL_PINK,
        C592_CADMIUM_RED_LIGHT,
        C505_VERMILION_HUE,
        C593_CADMIUM_ORANGE,
        C510_PERMANENT_ORANGE,
        C511_PERMANENT_YELLOW_ORANGE,
        C595_CADMIUM_YELLOW,
        C515_PERMANENT_YELLOW_DEEP,
        C516_PERMANENT_YELLOW_LIGHT,
        C517_LEMON_YELLOW,
        C518_NAPLES_YELLOW,
        C519_JAUNE_BRILLIANT,
        C529_OLIVE_GREEN,
        C525_BRILLIANT_YELLOW_GREEN,
        C526_PERMANENT_GREEN_LIGHT,
        C597_CADMIUM_GREEN_LIGHT,
        C599_CADMIUM_GREEN,
        C527_SAP_GREEN,
        C528_HOOKERS_GREEN,
        C530_VIRIDIAN_HUE,
        C523_EMERALD_GREEN,
        C524_BRIGHT_AQUA_GREEN,
        C535_PERMANENT_BLUE_LIGHT,
        C536_BRILLIANT_BLUE,
        C532_CERULEAN_BLUE,
        C537_CERULEAN_BLUE_HUE,
        C548_LIGHT_BLUE_VIOLET,
        C533_COBALT_BLUE,
        C538_COBALT_BLUE_HUE,
        C539_ULTRAMARINE_BLUE,
        C540_PHTHALOCYANINE_BLUE,
        C545_PERMANENT_VIOLET,
        C546_BRILLIANT_PURPLE,
        C544_LILAC,
        C547_MIDIUM_MAGENTA,
        C542_MAGENTA,
        C549_COMPOSE_ROSE,
        C554_BROWN_RED,
        C553_BURNT_SIENNA,
        C552_RAW_SIENNA,
        C556_BURNT_UMBER,
        C557_VANDYKE_BROWN,
        C555_RAW_UMBER,
        C551_YELLOW_OCHRE,
        C560_BLACK,
        C561_FRENCH_GREY_1,
        C562_TITANIUM_WHITE,
        C563_PEARL_WHITE,
        C564_PEARL_GOLD,
        C565_GOLD,
        C566_SILVER,
        C567_COPPER,
        C571_FLUORESCENT_MAGENTA,
        C572_FLUORESCENT_ORANGE,
        C573_FLUORESCENT_LEMON,
        C574_FLUORESCENT_GREEN,
        C575_FLUORESCENT_PINK
};
cv::Vec3b COLORS_LAB[61];

ImageProcessor::ImageProcessor() {
    bool deltaE = false;
    if (deltaE) {
        for (int i = 0; i < COLOR_N; ++i) {
            auto& color = COLORS_RGB[i];
            cv::Mat3b color_rgb(cv::Vec3b(color[0], color[1], color[2])), color_lab;
            cv::cvtColor(color_rgb, color_lab, cv::COLOR_RGB2Lab);
            COLORS_LAB[i] = color_lab.at<cv::Vec3b>(0);;
        }
        for (auto& color : COLORS_RGB) {
            cv::Mat3b color_rgb(cv::Vec3b(color[0], color[1], color[2])), color_lab;
            cv::cvtColor(color_rgb, color_lab, cv::COLOR_RGB2Lab);
        }
    }

    for (int r = 0; r <= 255; ++r) {
        for (int g = 0; g <= 255; ++g) {
            for (int b = 0; b <= 255; ++b) {
                auto p = get_nearest(r, g, b, deltaE);
                color_map[r][g][b] = p.first;
                name_map[r][g][b] = p.second;
            }
        }
    }
}

std::pair<cv::Vec3b, const std::string*> get_nearest(uint8_t r, uint8_t g, uint8_t b, bool deltaE = false) {
    cv::Vec3b nearest;
    const std::string* nearest_name;
    if (deltaE) {
        cv::Mat3b rgb(cv::Vec3b(r, g, b)), lab;
        cv::cvtColor(rgb, lab, cv::COLOR_RGB2Lab);
        auto& lab_v = lab.at<cv::Vec3b>(0);
        int mMin = std::numeric_limits<int>::max();
        for (int i = 0; i < COLOR_N; ++i) {
            auto& color_lab_v = COLORS_LAB[i];
            auto& color = COLORS_RGB[i];
            int l_dist = (int)color_lab_v[0] - lab_v[0];
            int a_dist = (int)color_lab_v[1] - lab_v[1];
            int b_dist = (int)color_lab_v[2] - lab_v[2];
            int distance = l_dist * l_dist + a_dist * a_dist + b_dist * b_dist;
            if (distance < mMin) {
                nearest = color;
                nearest_name = &(COLORS_NAME[i]);
                mMin = distance;
            }
        }
        return {nearest, nearest_name};
    }
    else {
        int mMin = std::numeric_limits<int>::max();
        for (int i = 0; i < COLOR_N; ++i) {
            auto& color = COLORS_RGB[i];
            int r_dist = (int)color[0] - r;
            int g_dist = (int)color[1] - g;
            int b_dist = (int)color[2] - b;
            int distance = r_dist * r_dist + g_dist * g_dist + b_dist * b_dist;
            if (distance < mMin) {
                nearest = color;
                nearest_name = &(COLORS_NAME[i]);
                mMin = distance;
            }
        }
        return {nearest, nearest_name};
    }
}

ImageProcessor::~ImageProcessor() = default;
