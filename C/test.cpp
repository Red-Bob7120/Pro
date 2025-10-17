#include <iostream>
#include <filesystem>
#include <vector>
#include <algorithm>
#include <iomanip>
#include <sstream>

namespace fs = std::filesystem;

int main(int argc, char* argv[]) {
    if (argc < 3) {
        std::cerr << "사용법: " << argv[0] << " <폴더경로> <파일이름접두어>\n";
        return 1;
    }

    fs::path folder = argv[1];
    std::string prefix = argv[2];

    if (!fs::exists(folder) || !fs::is_directory(folder)) {
        std::cerr << "폴더를 찾을 수 없습니다: " << folder << "\n";
        return 1;
    }

    // 파일 목록 수집
    std::vector<fs::directory_entry> files;
    for (const auto& entry : fs::directory_iterator(folder)) {
        if (entry.is_regular_file()) {
            files.push_back(entry);
        }
    }

    // 수정 시간 기준으로 정렬
    std::sort(files.begin(), files.end(),
              [](const auto& a, const auto& b) {
                  return fs::last_write_time(a) < fs::last_write_time(b);
              });

    int counter = 1;
    int digits = 4; // 0001 형식

    for (const auto& entry : files) {
        fs::path old_path = entry.path();
        std::ostringstream oss;
        oss << prefix << "_" << std::setw(digits) << std::setfill('0') << counter << old_path.extension().string();
        fs::path new_path = folder / oss.str();

        try {
            fs::rename(old_path, new_path);
            std::cout << old_path.filename().string() << " -> " << new_path.filename().string() << "\n";
        } catch (const fs::filesystem_error& e) {
            std::cerr << "[에러] " << e.what() << "\n";
        }

        counter++;
    }

    std::cout << "총 " << files.size() << "개 파일 이름 변경 완료!\n";
    return 0;
}
