import config.AppConfig;
import core.*;
import ui.ProgressCircle;
import ui.UILogger;
import util.ImageUtils;
import util.TimeUtils;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ICloudFinalApp_Full extends JFrame {

    // UI ELEMENTS
    private final JButton inputBtn  = new JButton("📂 입력 폴더 선택");
    private final JButton outputBtn = new JButton("📁 출력 폴더 선택");
    private final JButton startBtn  = new JButton("🚀 정리 시작");

    private final JLabel topTitle  = new JLabel("☁ iCloud 올리기 전 마지막 단계 (최적화 버전)", SwingConstants.CENTER);
    private final JLabel statusLbl = new JLabel("입력/출력 폴더를 선택하세요.", SwingConstants.CENTER);
    private final JLabel liveLog   = new JLabel("대기 중…", SwingConstants.CENTER);

    private final JLabel inputLbl  = new JLabel("입력 폴더: (미선택)");
    private final JLabel outputLbl = new JLabel("출력 폴더: (미선택)");

    private final ProgressCircle circle = new ProgressCircle();
    private final UILogger logger = new UILogger(liveLog);

    // 선택 폴더
    private volatile File inputRoot;
    private volatile File outputRoot;

    // 포맷 세트
    private static final Set<String> COMPAT  = Set.of("jpg","jpeg","png","gif","heic","heif");
    private static final Set<String> CONVERT = Set.of("bmp","tif","tiff","webp");

    // UI 업데이트 쓰로틀링
    private long lastUIUpdate = 0;


    // =============== 생성자 ==================

    public ICloudFinalApp_Full() {
        initUI();
        setVisible(true);
    }


    // =============== UI 초기화 ==================

    private void initUI() {
        setTitle("iCloud Final Processor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 620);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(16, 16));
        getContentPane().setBackground(new Color(248, 249, 252));

        topTitle.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        add(topTitle, BorderLayout.NORTH);

        // CENTER
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        circle.setPreferredSize(new Dimension(260, 260));
        circle.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLbl.setFont(new Font("Malgun Gothic", Font.PLAIN, 15));
        statusLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalStrut(12));
        center.add(circle);
        center.add(statusLbl);

        add(center, BorderLayout.CENTER);

        // BOTTOM
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 12, 12, 12));

        bottom.add(inputLbl);
        bottom.add(outputLbl);
        bottom.add(liveLog);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 4));
        row.setOpaque(false);

        styleButton(inputBtn, false);
        styleButton(outputBtn, false);
        styleButton(startBtn, true);
        startBtn.setEnabled(false);

        row.add(inputBtn);
        row.add(outputBtn);
        row.add(startBtn);

        bottom.add(row);
        add(bottom, BorderLayout.SOUTH);

        // EVENT
        inputBtn.addActionListener(this::chooseInput);
        outputBtn.addActionListener(this::chooseOutput);
        startBtn.addActionListener(this::startProcess);
    }


    // =============== 버튼 스타일 ==================

    private void styleButton(JButton b, boolean primary) {
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (primary) {
            b.setBackground(new Color(0, 122, 255));
            b.setForeground(Color.WHITE);
        } else {
            b.setBackground(new Color(230, 235, 242));
            b.setForeground(Color.DARK_GRAY);
        }
    }


    // =============== UI 업데이트 ==================

    private void ui(Runnable r) {
        SwingUtilities.invokeLater(r);
    }

    private void updateUIThrottled(Runnable r) {
        long now = System.nanoTime();
        if ((now - lastUIUpdate) / 1e9 > AppConfig.UI_UPDATE_INTERVAL) {
            lastUIUpdate = now;
            ui(r);
        }
    }


    // =============== 입력/출력 선택 ==================

    private void chooseInput(ActionEvent e) {
        JFileChooser ch = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        ch.setDialogTitle("입력 폴더 선택");

        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            inputRoot = ch.getSelectedFile();
            inputLbl.setText("입력 폴더: " + inputRoot.getAbsolutePath());
            logger.log("입력 폴더: " + inputRoot.getAbsolutePath());
            updateStartBtn();
        }
    }

    private void chooseOutput(ActionEvent e) {
        JFileChooser ch = new JFileChooser(FileSystem.getFileSystemView().getHomeDirectory());
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        ch.setDialogTitle("출력 폴더 선택");

        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputRoot = ch.getSelectedFile();
            outputLbl.setText("출력 폴더: " + outputRoot.getAbsolutePath());
            logger.log("출력 폴더: " + outputRoot.getAbsolutePath());
            updateStartBtn();
        }
    }

    private void updateStartBtn() {
        startBtn.setEnabled(inputRoot != null && outputRoot != null);
        if (startBtn.isEnabled())
            statusLbl.setText("준비 완료. 정리 시작 버튼을 눌러주세요.");
    }


    // =============== 정리 시작 ==================

    private void startProcess(ActionEvent e) {
        if (inputRoot == null || outputRoot == null) return;

        inputBtn.setEnabled(false);
        outputBtn.setEnabled(false);
        startBtn.setEnabled(false);

        logger.log("정리 작업 시작");
        statusLbl.setText("사진 스캔 중…");
        circle.setProgress(0);

        new Thread(() -> runPipeline()).start();
    }


    // =============== 전체 파이프라인 실행 ==================

    private void runPipeline() {
        long startNs = System.nanoTime();

        try {
            // 출력용 폴더 구조 생성
            File readyDir  = new File(outputRoot, "__iOS_READY");
            File failedDir = new File(outputRoot, "__FAILED");
            readyDir.mkdirs();
            failedDir.mkdirs();

            // 클래스 로딩
            FileScanner scanner       = new FileScanner(merge(COMPAT, CONVERT));
            DuplicateChecker dup      = new DuplicateChecker(AppConfig.HASH_READ_SIZE);
            QualityChecker qc         = new QualityChecker();
            ImageConverter converter  = new ImageConverter();
            FileMover mover           = new FileMover();

            // 스캔
            List<Path> files = scanner.scan(inputRoot);
            int total = files.size();

            if (total == 0) {
                ui(() -> statusLbl.setText("정리할 이미지 없음"));
                return;
            }

            // 통계
            AtomicInteger done      = new AtomicInteger();
            AtomicInteger kept      = new AtomicInteger();
            AtomicInteger converted = new AtomicInteger();
            AtomicInteger removed   = new AtomicInteger();
            AtomicInteger failed    = new AtomicInteger();

            // 삭제 일괄 처리 목록
            List<File> trashList = Collections.synchronizedList(new ArrayList<>());

            // iOS 파일명 IMG_00001.jpg
            AtomicInteger seq = new AtomicInteger(0);

            // CSV 맵핑 파일
            File mapping = new File(readyDir, "mapping.csv");
            PrintWriter map = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(mapping, true), StandardCharsets.UTF_8));

            // 병렬 처리
            ForkJoinPool pool = ForkJoinPool.commonPool();

            pool.submit(() -> files.parallelStream().forEach(p -> {

                String name = p.getFileName().toString();

                try {
                    // 1) 중복 검사
                    if (dup.isDuplicate(p)) {
                        trashList.add(p.toFile());
                        removed.incrementAndGet();
                        logger.log("중복 제외: " + name);
                        return;
                    }

                    // 2) 호환 포맷/변환 포맷 판별
                    String ext = ext(name);

                    File target = new File(readyDir, iosName(seq.incrementAndGet(), ext.equals("jpg") ? "jpg" : "jpg"));

                    // 호환 확장자
                    if (COMPAT.contains(ext)) {

                        QualityChecker.Result q = qc.check(p);

                        if (!q.ok) {
                            switch (q.reason) {
                                case "read-fail":
                                    failTo(p, failedDir, "READ_FAIL", map, failed);
                                    break;
                                case "too-small":
                                case "mono":
                                    trashList.add(p.toFile());
                                    removed.incrementAndGet();
                                    break;
                                default:
                                    failTo(p, failedDir, "UNKNOWN", map, failed);
                            }
                            return;
                        }

                        mover.move(p.toFile(), target);
                        kept.incrementAndGet();
                        writeMap(map, p, target, "KEEP");
                        logger.log("이동: " + name);
                        return;
                    }

                    // 변환 가능 확장자
                    if (CONVERT.contains(ext)) {
                        if (converter.convertToJpg(p.toFile(), target)) {
                            trashList.add(p.toFile());
                            converted.incrementAndGet();
                            writeMap(map, p, target, "CONVERT");
                            logger.log("변환: " + name);
                        } else {
                            failTo(p, failedDir, "CONVERT_FAIL", map, failed);
                        }
                        return;
                    }

                    // 그 외 확장자
                    mover.move(p.toFile(), target);
                    kept.incrementAndGet();
                    writeMap(map, p, target, "OTHER");

                } catch (Exception ex) {
                    failTo(p, failedDir, "ERROR", map, failed);
                }

                // 진행률 UI 업데이트
                int d = done.incrementAndGet();
                updateUIThrottled(() -> updateProgress(startNs, d, total));

            })).get();

            map.close();

            // 3) 마지막에 삭제 일괄 처리
            logger.log("삭제 예정: " + trashList.size() + "개");
            for (File f : trashList) {
                try { Desktop.getDesktop().moveToTrash(f); } catch (Exception ex) { f.delete(); }
            }

            // 완료 UI
            ui(() -> {
                circle.setProgress(1.0);
                statusLbl.setText("정리 완료! 유지 " + kept.get() +
                        " / 변환 " + converted.get() +
                        " / 제외 " + removed.get() +
                        " / 실패 " + failed.get());
                logger.log("완료");
            });

        } catch (Exception e) {
            ui(() -> statusLbl.setText("오류: " + e.getMessage()));
        }

        ui(() -> {
            inputBtn.setEnabled(true);
            outputBtn.setEnabled(true);
            startBtn.setEnabled(true);
        });
    }


    // =============== 보조 함수들 ==================

    private void updateProgress(long startNs, int done, int total) {
        double p = done / (double)total;
        double elapsed = (System.nanoTime() - startNs) / 1e9;
        double rate = done / Math.max(0.0001, elapsed);
        double remain = (total - done) / Math.max(0.0001, rate);

        circle.setProgress(p);
        statusLbl.setText(String.format(
                "진행 %d%% | %d/%d | %.1f개/초 | 남은시간 %s",
                (int)(p*100), done, total, rate, TimeUtils.eta(remain)
        ));
    }

    private void writeMap(PrintWriter map, Path src, File dest, String type) {
        synchronized (map) {
            map.printf("\"%s\",\"%s\",\"%s\"%n",
                    src.toAbsolutePath(),
                    dest.getAbsolutePath(),
                    type);
        }
    }

    private void failTo(Path src, File base, String reason, PrintWriter map, AtomicInteger cnt) {
        try {
            File folder = FailClassifier.getFailedFolder(base, reason);
            File dest = new File(folder, src.getFileName().toString());
            Files.move(src, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            cnt.incrementAndGet();
            writeMap(map, src, dest, reason);
            logger.log(reason + ": " + src.getFileName());
        } catch (Exception ignored) {}
    }

    private static String ext(String name) {
        int dot = name.lastIndexOf(".");
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private static String iosName(int seq, String ext) {
        return String.format("IMG_%05d.%s", seq, ext.toUpperCase());
    }

    private static Set<String> merge(Set<String> a, Set<String> b) {
        Set<String> s = new HashSet<>();
        s.addAll(a); s.addAll(b);
        return s;
    }


    // =============== MAIN ==================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ICloudFinalApp_Full::new);
    }
}
