import os
import hashlib

# 해시값으로 중복 확인
def file_hash(path):
    """이미지 파일을 열어 해시값 생성"""
    import hashlib
    hasher = hashlib.md5()
    with open(path, 'rb') as f:
        buf = f.read()
        hasher.update(buf)
    return hasher.hexdigest()

def remove_duplicates_and_rename(folder, base_name):
    seen = {}
    idx = 1

    for filename in os.listdir(folder):
        filepath = os.path.join(folder, filename)

        # 이미지 파일만 처리
        if not filename.lower().endswith(('.png', '.jpg', '.jpeg', '.gif', '.bmp', '.tiff')):
            continue

        try:
            # 해시값 생성
            h = file_hash(filepath)
            if h in seen:
                print(f"중복 삭제: {filename}")
                os.remove(filepath)   # 중복이면 삭제
                continue
            else:
                seen[h] = filepath

            # 새 파일 이름 지정
            ext = os.path.splitext(filename)[1].lower()
            new_name = f"{base_name}_{idx}{ext}"
            new_path = os.path.join(folder, new_name)

            os.rename(filepath, new_path)
            print(f"{filename} → {new_name}")

            idx += 1
        except Exception as e:
            print(f"에러 ({filename}): {e}")

if __name__ == "__main__":
    # 사용자 입력 받기
    folder = input("정리할 사진 폴더 경로를 입력하세요: ").strip()
    base_name = input("새 파일 이름 접두사를 입력하세요: ").strip()

    if os.path.isdir(folder):
        remove_duplicates_and_rename(folder, base_name)
    else:
        print("❌ 폴더 경로가 잘못되었습니다.")
