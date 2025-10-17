#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
GUI Media Sorter
- 소스 폴더와 (옵션) 결과를 만들 '상위(부모) 폴더'를 각각 지정
- 옵션: 복사/이동, 드라이런, 기타 제외, 제자리 정리(in-place), ZIP 보관
- 결과 구조:
    in-place: src/(Photos, GIFs, Videos, Others)
    일반:    dest_parent/Sorted_타임스탬프/(Photos, GIFs, Videos, Others)
"""

import os
import sys
import shutil
import zipfile
from pathlib import Path
from datetime import datetime
import threading
import queue

import tkinter as tk
from tkinter import ttk, filedialog, messagebox

# ===== 분류 기준 =====
IMAGE_EXTS = {
    ".jpg", ".jpeg", ".png", ".webp", ".bmp", ".tif", ".tiff",
    ".heic", ".heif", ".raw", ".arw", ".cr2", ".nef", ".orf", ".rw2"
}
GIF_EXTS = {".gif"}
VIDEO_EXTS = {
    ".mp4", ".mov", ".m4v", ".mkv", ".avi", ".wmv", ".flv", ".webm", ".3gp",
    ".mts", ".m2ts", ".ts"
}

FOLDER_PHOTOS = "Photos"
FOLDER_GIFS = "GIFs"
FOLDER_VIDEOS = "Videos"
FOLDER_OTHERS = "Others"


def classify(path: Path) -> str:
    ext = path.suffix.lower()
    if ext in GIF_EXTS:
        return FOLDER_GIFS
    if ext in IMAGE_EXTS:
        return FOLDER_PHOTOS
    if ext in VIDEO_EXTS:
        return FOLDER_VIDEOS
    return FOLDER_OTHERS


def iter_files(root: Path):
    for dirpath, dirnames, filenames in os.walk(root):
        # 숨김 폴더 제외
        dirnames[:] = [d for d in dirnames if not d.startswith(".")]
        for name in filenames:
            if name.startswith("."):
                continue
            p = Path(dirpath) / name
            if p.is_file():
                yield p


def should_skip(path: Path, dest_root: Path) -> bool:
    # 이미 목적 보관 폴더 내부면 스킵
    for sub in [FOLDER_PHOTOS, FOLDER_GIFS, FOLDER_VIDEOS, FOLDER_OTHERS]:
        try:
            path.relative_to(dest_root / sub)
            return True
        except ValueError:
            pass
    return False


def safe_move_or_copy(src: Path, dest_dir: Path, copy: bool, dry_run: bool, log):
    dest_dir.mkdir(parents=True, exist_ok=True)
    target = dest_dir / src.name

    if target.exists():
        stem, suffix = target.stem, target.suffix
        counter = 1
        while True:
            candidate = dest_dir / f"{stem} ({counter}){suffix}"
            if not candidate.exists():
                target = candidate
                break
            counter += 1

    action = "COPY" if copy else "MOVE"
    if dry_run:
        log(f"[DRY-RUN] {action}: {src}  ->  {target}")
        return target

    if copy:
        shutil.copy2(src, target)
    else:
        shutil.move(src, target)
    log(f"[{action}] {src}  ->  {target}")
    return target


def make_zip_from_folder(folder: Path, zip_path: Path, dry_run: bool, log=print):
    if dry_run:
        log(f"[DRY-RUN] ZIP: {folder} -> {zip_path}")
        return
    with zipfile.ZipFile(zip_path, 'w', compression=zipfile.ZIP_DEFLATED) as zf:
        for p in folder.rglob("*"):
            if p.is_file():
                zf.write(p, p.relative_to(folder))
    log(f"[ZIP] {zip_path}")


# ===== GUI =====
class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Media Sorter (GUI)")
        self.geometry("760x560")
        self.minsize(720, 520)

        # 상태
        self.running = False
        self.log_queue = queue.Queue()

        # 변수
        self.var_src = tk.StringVar()
        self.var_dest_parent = tk.StringVar()
        self.var_copy = tk.BooleanVar(value=False)
        self.var_dry = tk.BooleanVar(value=False)
        self.var_no_others = tk.BooleanVar(value=False)
        self.var_inplace = tk.BooleanVar(value=False)
        self.var_zip = tk.BooleanVar(value=False)

        self._build_ui()
        self.after(100, self._poll_log_queue)

    def _build_ui(self):
        pad = {"padx": 8, "pady": 6}

        frm_top = ttk.Frame(self)
        frm_top.pack(fill="x", **pad)

        # 소스 폴더
        ttk.Label(frm_top, text="소스 폴더 (정리 대상)").grid(row=0, column=0, sticky="w")
        ent_src = ttk.Entry(frm_top, textvariable=self.var_src)
        ent_src.grid(row=1, column=0, sticky="we", padx=(0, 6))
        btn_src = ttk.Button(frm_top, text="찾기…", command=self.choose_src)
        btn_src.grid(row=1, column=1, sticky="e")
        frm_top.columnconfigure(0, weight=1)

        # 결과 상위(부모) 폴더
        ttk.Label(frm_top, text="결과 상위(부모) 폴더").grid(row=2, column=0, sticky="w", pady=(10, 0))
        ent_dest = ttk.Entry(frm_top, textvariable=self.var_dest_parent, state="normal")
        ent_dest.grid(row=3, column=0, sticky="we", padx=(0, 6))
        btn_dest = ttk.Button(frm_top, text="찾기…", command=self.choose_dest_parent)
        btn_dest.grid(row=3, column=1, sticky="e")

        # 옵션들
        frm_opts = ttk.LabelFrame(self, text="옵션")
        frm_opts.pack(fill="x", **pad)

        chk_copy = ttk.Checkbutton(frm_opts, text="복사 모드(원본 유지)", variable=self.var_copy)
        chk_dry = ttk.Checkbutton(frm_opts, text="드라이런(시뮬레이션)", variable=self.var_dry)
        chk_no_others = ttk.Checkbutton(frm_opts, text="기타(Others) 제외", variable=self.var_no_others)
        chk_inplace = ttk.Checkbutton(frm_opts, text="제자리 정리(in-place, src 바로 아래)", variable=self.var_inplace,
                                      command=self._toggle_inplace)
        chk_zip = ttk.Checkbutton(frm_opts, text="정리 결과 ZIP 보관", variable=self.var_zip)

        chk_copy.grid(row=0, column=0, sticky="w", padx=6, pady=4)
        chk_dry.grid(row=0, column=1, sticky="w", padx=6, pady=4)
        chk_no_others.grid(row=0, column=2, sticky="w", padx=6, pady=4)
        chk_inplace.grid(row=1, column=0, sticky="w", padx=6, pady=4)
        chk_zip.grid(row=1, column=1, sticky="w", padx=6, pady=4)

        # 실행/중지 버튼
        frm_run = ttk.Frame(self)
        frm_run.pack(fill="x", **pad)

        self.btn_run = ttk.Button(frm_run, text="실행", command=self.run_worker)
        self.btn_run.pack(side="left")

        self.btn_stop = ttk.Button(frm_run, text="중지", command=self.stop_worker, state="disabled")
        self.btn_stop.pack(side="left", padx=(8, 0))

        # 진행 바
        self.prog = ttk.Progressbar(frm_run, mode="indeterminate")
        self.prog.pack(side="right", fill="x", expand=True)

        # 로그 창
        frm_log = ttk.LabelFrame(self, text="로그")
        frm_log.pack(fill="both", expand=True, **pad)

        self.txt = tk.Text(frm_log, height=16, wrap="none")
        self.txt.pack(fill="both", expand=True, padx=6, pady=6)
        self.txt.configure(state="disabled")

        # 초기 상태
        self._toggle_inplace()

    def choose_src(self):
        path = filedialog.askdirectory(title="소스 폴더 선택")
        if path:
            self.var_src.set(path)

    def choose_dest_parent(self):
        path = filedialog.askdirectory(title="결과 상위(부모) 폴더 선택")
        if path:
            self.var_dest_parent.set(path)

    def _toggle_inplace(self):
        inplace = self.var_inplace.get()
        # in-place면 상위 폴더 선택을 비활성화
        state = "disabled" if inplace else "normal"
        # Entry와 버튼을 찾아 상태 변경
        for child in self.children.values():
            pass
        # 좀 더 직접적으로 탐색
        for widget in self.winfo_children():
            if isinstance(widget, ttk.Frame):
                for w in widget.winfo_children():
                    if isinstance(w, ttk.Entry) and w.cget("textvariable") == str(self.var_dest_parent):
                        w.configure(state=state)
                    if isinstance(w, ttk.Button) and w.cget("text") == "찾기…":
                        # dest 쪽 버튼만 막아야 하므로 라벨 근처 상태로 구분 어렵다.
                        # 간단히: in-place이면 dest 버튼을 비활성화, 아니면 활성화
                        if w["command"] == self.choose_dest_parent:
                            w.configure(state=state)

    def log(self, msg):
        self.log_queue.put(msg)

    def _poll_log_queue(self):
        try:
            while True:
                msg = self.log_queue.get_nowait()
                self.txt.configure(state="normal")
                self.txt.insert("end", msg + "\n")
                self.txt.see("end")
                self.txt.configure(state="disabled")
        except queue.Empty:
            pass
        self.after(100, self._poll_log_queue)

    def run_worker(self):
        if self.running:
            return
        src = Path(self.var_src.get().strip()).expanduser()
        if not src.exists() or not src.is_dir():
            messagebox.showerror("오류", "유효한 소스 폴더를 선택해 주세요.")
            return

        if self.var_inplace.get():
            dest_parent = None  # in-place 모드
        else:
            dest_parent_str = self.var_dest_parent.get().strip()
            if not dest_parent_str:
                messagebox.showerror("오류", "in-place가 아니면 '결과 상위(부모) 폴더'를 지정해야 합니다.")
                return
            dest_parent = Path(dest_parent_str).expanduser()
            if not dest_parent.exists() or not dest_parent.is_dir():
                messagebox.showerror("오류", "유효한 '결과 상위(부모) 폴더'를 선택해 주세요.")
                return

        # 실행 상태 전환
        self.running = True
        self.btn_run.configure(state="disabled")
        self.btn_stop.configure(state="normal")
        self.prog.start(10)

        t = threading.Thread(
            target=self._do_sort,
            args=(src, dest_parent,
                  self.var_copy.get(), self.var_dry.get(),
                  self.var_no_others.get(), self.var_inplace.get(),
                  self.var_zip.get()),
            daemon=True
        )
        t.start()

    def stop_worker(self):
        # 간단한 중지 플래그만 제공 (긴 zip 중에는 즉시 멈추진 않을 수 있음)
        if self.running:
            self.running = False
            self.log("[중지 요청] 다음 안전 지점에서 중단됩니다.")

    def _finish_ui(self):
        self.running = False
        self.btn_run.configure(state="normal")
        self.btn_stop.configure(state="disabled")
        self.prog.stop()

    def _do_sort(self, src_root: Path, dest_parent: Path,
                 copy: bool, dry_run: bool, no_others: bool,
                 inplace: bool, do_zip: bool):
        try:
            self.log("===== 작업 시작 =====")
            self.log(f"소스: {src_root}")

            # 목적지 루트 결정
            if inplace:
                dest_root = src_root
                self.log("[모드] 제자리 정리(in-place): src 바로 아래에 분류 폴더 생성")
            else:
                ts = datetime.now().strftime("%Y%m%d_%H%M%S")
                dest_root = (dest_parent / f"Sorted_{ts}")
                if not dry_run:
                    dest_root.mkdir(parents=True, exist_ok=True)
                self.log(f"[모드] 자동 보관 폴더 생성: {dest_root}")

            # 하위 카테고리 폴더 생성
            targets = [FOLDER_PHOTOS, FOLDER_GIFS, FOLDER_VIDEOS]
            if not no_others:
                targets.append(FOLDER_OTHERS)
            if not dry_run:
                for tname in targets:
                    (dest_root / tname).mkdir(parents=True, exist_ok=True)

            total = moved = skipped = 0

            # 안전장치: in-place가 아니면서 dest_root가 src_root 내부가 아닌지 체크 (의도적으로 내부에 두고 싶으면 in-place 사용)
            if not inplace:
                try:
                    # dest_root가 src_root 하위인지
                    dest_root.relative_to(src_root)
                    self.log("[경고] 결과 폴더가 소스 내부에 있습니다. 자동으로 처리되지만, 대량 정리 시 권장하지 않습니다.")
                except ValueError:
                    pass

            for file_path in iter_files(src_root):
                if not self.running:
                    self.log("[중단됨] 사용자 요청으로 작업을 멈춥니다.")
                    break

                total += 1

                # 보관 폴더 내부 파일은 스킵
                if should_skip(file_path, dest_root):
                    skipped += 1
                    continue

                category = classify(file_path)
                if category == FOLDER_OTHERS and no_others:
                    skipped += 1
                    continue

                dest_dir = dest_root / category
                try:
                    safe_move_or_copy(file_path, dest_dir, copy=copy, dry_run=dry_run, log=self.log)
                    moved += 1
                except Exception as e:
                    skipped += 1
                    self.log(f"[에러] {file_path} 처리 실패: {e}")

            # ZIP 옵션
            zip_path = None
            if do_zip:
                zip_name = dest_root.name if dest_root != src_root else f"{src_root.name}_Sorted"
                zip_path = (dest_root.parent / f"{zip_name}.zip").with_suffix(".zip")
                try:
                    make_zip_from_folder(dest_root, zip_path, dry_run=dry_run, log=self.log)
                except Exception as e:
                    self.log(f"[에러] ZIP 생성 실패: {e}")

            mode = "COPY" if copy else "MOVE"
            self.log("\n===== 요약 =====")
            self.log(f"대상 폴더: {src_root}")
            self.log(f"보관 폴더: {dest_root}")
            if do_zip:
                self.log(f"ZIP 보관: {zip_path if zip_path else '(생성 실패)'}")
            self.log(f"모드: {mode} | 드라이런: {dry_run}")
            self.log(f"총 파일: {total} | 처리: {moved} | 스킵: {skipped}")
            self.log(f"생성 구조: {dest_root}/({FOLDER_PHOTOS}, {FOLDER_GIFS}, {FOLDER_VIDEOS}"
                     f"{'' if no_others else f', {FOLDER_OTHERS}'})")
            self.log("===== 작업 종료 =====")

        finally:
            self._finish_ui()


def main():
    app = App()
    app.mainloop()


if __name__ == "__main__":
    main()
