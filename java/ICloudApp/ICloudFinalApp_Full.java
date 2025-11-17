import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

public class ICloudFinalApp_Full extends JFrame {

    // ==== UI 컴포넌트 ====
    private final JButton inputBtn  = new JButton("📂 입력 폴더 선택");
    private final JButton outputBtn = new JButton("📁 출력 폴더 선택");
    private final JButton startBtn  = new JButton("🚀 정리 시작");

    private final JLabel topTitle  = new JLabel("☁ iCloud 올리기 전 마지막 단계 (완전판)", SwingConstants.CENTER);
    private final JLabel statusLbl = new JLabel("입력 폴더와 출력 폴더를 선택하세요.", SwingConstants.CENTER);
    private final JLabel liveLog   = new JLabel("대기 중…", SwingConstants.CENTER);
    private final JLabel inputLbl  = new JLabel("입력 폴더: (미선택)");
    private final JLabel outputLbl = new JLabel("출력 폴더: (미선택)");

    private final ProgressCircle circle = new ProgressCircle();

    // 선택된 폴더
    private volatile File inputRoot;
    private volatile File outputRoot;

    // ==== 포맷 셋 ====
    private static final Set<String> COMPAT  = setOf("jpg","jpeg","png","heic","heif","gif");
    private static final Set<String> CONVERT = setOf("bmp","tif","tiff","webp");

    // ==== 품질 필터 기준 ====
    private static final int    MIN_W    = 256;
    private static final int    MIN_H    = 256;
    private static final double MONO_VAR = 2.0; // 이 값보다 분산이 작으면 거의 단색

    public ICloudFinalApp_Full() {
        initGlobalFont();

        setTitle("iCloud 올리기 전 마지막 단계 (완전판)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(16, 16));
        getContentPane().setBackground(new Color(248, 249, 252));

        // 상단 타이틀
        topTitle.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        add(topTitle, BorderLayout.NORTH);

        // 중앙: 원형 진행률 + 상태
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        circle.setPreferredSize(new Dimension(260, 260));
        circle.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLbl.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        statusLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLbl.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        centerPanel.add(Box.createVerticalStrut(12));
        centerPanel.add(circle);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(statusLbl);

        add(centerPanel, BorderLayout.CENTER);

        // 하단: 경로 표시 + 로그 + 버튼들
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        inputLbl.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        outputLbl.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        bottomPanel.add(inputLbl);
        bottomPanel.add(outputLbl);

        liveLog.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        liveLog.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        bottomPanel.add(liveLog);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 4));
        buttonRow.setOpaque(false);
        styleButton(inputBtn, false);
        styleButton(outputBtn, false);
        styleButton(startBtn, true);
        startBtn.setEnabled(false);

        buttonRow.add(inputBtn);
        buttonRow.add(outputBtn);
        buttonRow.add(startBtn);

        bottomPanel.add(buttonRow);

        add(bottomPanel, BorderLayout.SOUTH);

        // 이벤트
        inputBtn.addActionListener(this::onSelectInput);
        outputBtn.addActionListener(this::onSelectOutput);
        startBtn.addActionListener(this::onStart);

        setVisible(true);
    }

    private void initGlobalFont() {
        Font ui = new Font("Malgun Gothic", Font.PLAIN, 13);
        UIManager.put("Label.font", ui);
        UIManager.put("Button.font", ui.deriveFont(Font.BOLD, 13f));
        UIManager.put("ToolTip.font", ui);
    }

    private void styleButton(JButton b, boolean primary) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        if (primary) {
            b.setBackground(new Color(0, 122, 255));
            b.setForeground(Color.WHITE);
        } else {
            b.setBackground(new Color(235, 239, 245));
            b.setForeground(Color.DARK_GRAY);
        }
    }

    // ======================= UI 이벤트 =======================

    private void onSelectInput(ActionEvent e) {
        JFileChooser ch = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        ch.setDialogTitle("입력(원본) 사진 폴더 선택");
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            inputRoot = ch.getSelectedFile();
            inputLbl.setText("입력 폴더: " + inputRoot.getAbsolutePath());
            log("입력 폴더 선택: " + inputRoot.getAbsolutePath());
            updateStartButton();
        }
    }

    private void onSelectOutput(ActionEvent e) {
        JFileChooser ch = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        ch.setDialogTitle("출력 루트 폴더 선택 (정리본이 저장될 위치)");
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputRoot = ch.getSelectedFile();
            outputLbl.setText("출력 폴더: " + outputRoot.getAbsolutePath());
            log("출력 폴더 선택: " + outputRoot.getAbsolutePath());
            updateStartButton();
        }
    }

    private void updateStartButton() {
        startBtn.setEnabled(inputRoot != null && outputRoot != null);
        if (inputRoot != null && outputRoot != null) {
            statusLbl.setText("준비 완료: 정리 시작 버튼을 눌러주세요.");
        }
    }

    private void onStart(ActionEvent e) {
        if (inputRoot == null || outputRoot == null) return;
        startBtn.setEnabled(false);
        inputBtn.setEnabled(false);
        outputBtn.setEnabled(false);
        circle.setProgress(0.0);
        statusLbl.setText("파일 스캔 중...");
        log("정리 작업을 시작합니다.");

        new Thread(() -> runPipeline(inputRoot, outputRoot)).start();
    }

    // ======================= 메인 파이프라인 =======================

    private void runPipeline(File input, File outputBase) {
        long globalStartNs = System.nanoTime();

        File readyRoot  = new File(outputBase, "__iOS_READY");
        File failedRoot = new File(outputBase, "__FAILED");
        readyRoot.mkdirs();
        failedRoot.mkdirs();

        // 삭제는 마지막에 한 번에 실행
        List<File> trashList = Collections.synchronizedList(new ArrayList<>());

        // 1) 타겟 파일 스캔 (입력 폴더 기준)
        List<Path> all = new ArrayList<>();
        try {
            Files.walkFileTree(input.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName().toString();
                    if (name.equals("__iOS_READY") || name.equals("__FAILED")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (isTarget(n)) {
                        all.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            ui(() -> statusLbl.setText("스캔 오류: " + ex.getMessage()));
            resetButtons();
            return;
        }

        int total = all.size();
        if (total == 0) {
            ui(() -> {
                circle.setProgress(1.0);
                statusLbl.setText("정리할 이미지가 없습니다.");
                log("정리 대상 이미지가 없습니다.");
            });
            resetButtons();
            return;
        }

        AtomicInteger done      = new AtomicInteger(0);
        AtomicInteger kept      = new AtomicInteger(0);
        AtomicInteger converted = new AtomicInteger(0);
        AtomicInteger dups      = new AtomicInteger(0);
        AtomicInteger failed    = new AtomicInteger(0);
        AtomicInteger seq       = new AtomicInteger(0);

        ConcurrentHashMap<String, Path> seen = new ConcurrentHashMap<>();

        File reportFile  = new File(readyRoot, "report.txt");
        File mappingFile = new File(readyRoot, "mapping.csv");

        try (PrintWriter rep = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(reportFile, true), StandardCharsets.UTF_8));
             PrintWriter map = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(mappingFile, true), StandardCharsets.UTF_8))) {

            rep.println("===== 새로운 실행 =====");
            rep.println("Input Root : " + input.getAbsolutePath());
            rep.println("Output Root: " + outputBase.getAbsolutePath());
            rep.println("Start: " + now());

            int threads = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
            ExecutorService pool = Executors.newFixedThreadPool(threads);

            for (Path p : all) {
                pool.submit(() -> {
                    String name = p.getFileName().toString();
                    try {
                        // 1) 중복 검사 (크기 + 앞 1MB 해시)
                        String key = quickHash(p);
                        if (key != null && seen.putIfAbsent(key, p) != null) {
                            trashList.add(p.toFile()); // 입력 폴더의 중복본은 나중에 휴지통
                            dups.incrementAndGet();
                            log("중복 제거 예정: " + name);
                        } else {
                            // 2) 포맷/품질/변환 처리
                            String ext = extLower(name);
                            boolean compat = COMPAT.contains(ext);
                            String ios = iosName(seq.incrementAndGet(), compat ? ext : "jpg");
                            File dest = new File(readyRoot, ios);

                            if (compat) {
                                Quality q = checkQuality(p);
                                if (!q.ok) {
                                    if (q.reason.startsWith("read")) {
                                        // 읽기 실패는 __FAILED/READ_FAIL
                                        File dir = getFailedDir(failedRoot, "READ_FAIL");
                                        File fail = new File(dir, name);
                                        safeMove(p.toFile(), fail);
                                        failed.incrementAndGet();
                                        writeMapping(map, p, fail, "READ_FAIL");
                                        log("읽기 실패(READ_FAIL): " + name);
                                    } else if ("too-small".equals(q.reason) || "mono".equals(q.reason)) {
                                        // 너무 작거나 단색 → 삭제 후보
                                        trashList.add(p.toFile());
                                        dups.incrementAndGet();
                                        log("품질 제외 예정(" + q.reason + "): " + name);
                                    } else {
                                        File dir = getFailedDir(failedRoot, "UNKNOWN");
                                        File fail = new File(dir, name);
                                        safeMove(p.toFile(), fail);
                                        failed.incrementAndGet();
                                        writeMapping(map, p, fail, "UNKNOWN");
                                        log("품질 실패(UNKNOWN): " + name);
                                    }
                                } else {
                                    safeMove(p.toFile(), dest);
                                    kept.incrementAndGet();
                                    writeMapping(map, p, dest, "KEEP");
                                    log("이동: " + name + " → " + dest.getName());
                                }
                            } else if (CONVERT.contains(ext)) {
                                if (tryConvertToJpg(p.toFile(), dest)) {
                                    // 변환 성공 → 원본은 삭제 후보
                                    trashList.add(p.toFile());
                                    converted.incrementAndGet();
                                    writeMapping(map, p, dest, "CONVERT");
                                    log("변환: " + name + " → " + dest.getName());
                                } else {
                                    // 변환 실패 → __FAILED/CONVERT_FAIL
                                    File dir = getFailedDir(failedRoot, "CONVERT_FAIL");
                                    File fail = new File(dir, name);
                                    safeMove(p.toFile(), fail);
                                    failed.incrementAndGet();
                                    writeMapping(map, p, fail, "CONVERT_FAIL");
                                    log("변환 실패(CONVERT_FAIL): " + name);
                                }
                            } else {
                                // 기타 포맷은 이름만 iOS 형식으로 맞춰 이동
                                File keepFile = new File(readyRoot, iosName(seq.get(), ext));
                                safeMove(p.toFile(), keepFile);
                                kept.incrementAndGet();
                                writeMapping(map, p, keepFile, "OTHER");
                                log("기타 포맷 이동: " + name + " → " + keepFile.getName());
                            }
                        }
                    } catch (Exception ex) {
                        try {
                            File dir = getFailedDir(failedRoot, "ERROR");
                            File fail = new File(dir, name);
                            if (Files.exists(p)) safeMove(p.toFile(), fail);
                            failed.incrementAndGet();
                            writeMapping(map, p, fail, "ERROR");
                        } catch (Exception ignore) {}
                        log("오류(ERROR): " + name + " → " + ex.getMessage());
                    } finally {
                        int d = done.incrementAndGet();
                        updateProgress(globalStartNs, d, total);
                    }
                });
            }

            pool.shutdown();
            try {
                pool.awaitTermination(99, TimeUnit.HOURS);
            } catch (InterruptedException ignored) {}

            // 3) 마지막 단계: 삭제 예정 파일 일괄 휴지통 이동
            ui(() -> {
                statusLbl.setText("마지막 단계: 삭제 예정 파일을 휴지통으로 이동 중…");
                log("삭제 예정 파일 수: " + trashList.size() + "개");
            });
            for (File f : trashList) {
                moveToTrash(f);
            }

            long elapsedNs = System.nanoTime() - globalStartNs;
            double sec  = elapsedNs / 1e9;
            double rate = done.get() > 0 ? done.get() / sec : 0.0;

            rep.printf(Locale.ROOT,
                    "Total: %d, Kept: %d, Converted: %d, Duplicates/QualityRemoved: %d, Failed: %d%n",
                    done.get(), kept.get(), converted.get(), dups.get(), failed.get());
            rep.printf(Locale.ROOT, "Trash moved: %d%n", trashList.size());
            rep.printf(Locale.ROOT, "Elapsed: %.1fs, Rate: %.1f files/s%n", sec, rate);
            rep.println("End: " + now());
            rep.println();

            ui(() -> {
                circle.setProgress(1.0);
                statusLbl.setText(String.format(
                        "정리 완료: 총 %d개 / 유지 %d / 변환 %d / 제외 %d / 실패 %d / 삭제 %d",
                        done.get(), kept.get(), converted.get(), dups.get(), failed.get(), trashList.size()
                ));
                log(String.format("완료: %.1f초, 평균 속도 %.1f개/초", sec, rate));
            });

        } catch (Exception ex) {
            ui(() -> statusLbl.setText("리포트/로그 작성 오류: " + ex.getMessage()));
        }

        resetButtons();
    }

    // ======================= 보조 메서드 =======================

    private void resetButtons() {
        ui(() -> {
            inputBtn.setEnabled(true);
            outputBtn.setEnabled(true);
            updateStartButton();
        });
    }

    private static Set<String> setOf(String... arr) {
        Set<String> s = new HashSet<>();
        for (String a : arr) s.add(a.toLowerCase(Locale.ROOT));
        return s;
    }

    private boolean isTarget(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return COMPAT.contains(ext) || CONVERT.contains(ext);
    }

    private static String extLower(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String iosName(int seq, String ext) {
        return String.format("IMG_%05d.%s", seq, ext.toUpperCase(Locale.ROOT));
    }

    private void safeMove(File src, File dest) throws IOException {
        dest.getParentFile().mkdirs();
        try {
            Files.move(src.toPath(), dest.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean tryConvertToJpg(File src, File dest) {
        try {
            BufferedImage img = ImageIO.read(src);
            if (img == null) return false;
            Quality q = qualityOf(img);
            if (!q.ok) return false;
            dest.getParentFile().mkdirs();
            return ImageIO.write(img, "jpg", dest);
        } catch (Exception e) {
            return false;
        }
    }

    // 빠른 중복 키: 파일 크기 + 앞 1MB SHA-256
    private String quickHash(Path p) {
        try {
            long size = Files.size(p);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new BufferedInputStream(Files.newInputStream(p))) {
                byte[] buf = new byte[1024 * 1024];
                int r = in.read(buf);
                if (r > 0) md.update(buf, 0, r);
            }
            byte[] d = md.digest();
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return size + ":" + sb;
        } catch (Exception e) {
            return null;
        }
    }

    // 품질 체크(파일 기준)
    private Quality checkQuality(Path p) {
        try {
            BufferedImage img = ImageIO.read(p.toFile());
            if (img == null) return new Quality(false, "read-null");
            return qualityOf(img);
        } catch (Exception e) {
            return new Quality(false, "read-fail");
        }
    }

    // 품질 체크(이미지 객체 기준)
    private Quality qualityOf(BufferedImage img) {
        if (img.getWidth() < MIN_W || img.getHeight() < MIN_H)
            return new Quality(false, "too-small");

        long sum = 0, sumSq = 0, cnt = 0;
        int stepX = Math.max(1, img.getWidth() / 64);
        int stepY = Math.max(1, img.getHeight() / 64);

        for (int y = 0; y < img.getHeight(); y += stepY) {
            for (int x = 0; x < img.getWidth(); x += stepX) {
                int rgb = img.getRGB(x, y);
                int g = (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + (rgb & 0xff)) / 3;
                sum += g;
                sumSq += (long) g * g;
                cnt++;
            }
        }
        double mean = sum / (double) cnt;
        double var  = sumSq / (double) cnt - mean * mean;
        if (var < MONO_VAR) return new Quality(false, "mono");
        return new Quality(true, "ok");
    }

    // 실패 원인별 폴더
    private File getFailedDir(File failedRoot, String reasonKey) {
        String folder;
        switch (reasonKey) {
            case "READ_FAIL":
                folder = "READ_FAIL"; break;
            case "CONVERT_FAIL":
                folder = "CONVERT_FAIL"; break;
            case "ERROR":
                folder = "ERROR"; break;
            default:
                folder = "UNKNOWN"; break;
        }
        File dir = new File(failedRoot, folder);
        dir.mkdirs();
        return dir;
    }

    private void moveToTrash(File f) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().moveToTrash(f);
            } else {
                f.delete();
            }
        } catch (Exception e) {
            f.delete();
        }
    }

    private void writeMapping(PrintWriter map, Path src, File dest, String type) {
        synchronized (map) {
            map.printf("\"%s\",\"%s\",\"%s\"%n",
                    src.toFile().getAbsolutePath().replace("\"","\"\""),
                    dest.getAbsolutePath().replace("\"","\"\""),
                    type);
            map.flush();
        }
    }

    private void updateProgress(long startNs, int done, int total) {
        double p = Math.max(0, Math.min(1, done / (double) total));
        long now = System.nanoTime();
        double elapsed = (now - startNs) / 1e9;
        double rate = done > 0 ? done / elapsed : 0.0;
        double remain = (total - done) / Math.max(1e-6, rate);
        String eta = fmtDuration(remain);

        ui(() -> {
            circle.setProgress(p);
            statusLbl.setText(String.format(
                    "진행 %d%% | 처리 %d / %d | 속도 %.1f개/초 | 예상 남은 시간 %s",
                    (int) Math.round(p * 100), done, total, rate, eta
            ));
        });
    }

    private static String fmtDuration(double sec) {
        if (sec < 0) sec = 0;
        long s = Math.round(sec);
        long h = s / 3600; s %= 3600;
        long m = s / 60;   s %= 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private void ui(Runnable r) {
        SwingUtilities.invokeLater(r);
    }

    // 로그는 항상 한 줄, 최신 내용만 표시
    private void log(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String full = "[" + time + "] " + msg;
        ui(() -> liveLog.setText(full));
    }

    // 원형 진행률 UI
    static class ProgressCircle extends JPanel {
        private double progress = 0.0;
        public void setProgress(double p) {
            this.progress = Math.max(0, Math.min(1, p));
            repaint();
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 40;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            g2.setColor(new Color(234, 238, 245));
            g2.fill(new Ellipse2D.Double(x, y, size, size));

            g2.setColor(new Color(0, 122, 255));
            g2.fill(new Arc2D.Double(x, y, size, size, 90, -progress * 360, Arc2D.PIE));

            g2.setColor(getBackground());
            int inner = size - 40;
            g2.fill(new Ellipse2D.Double(x + 20, y + 20, inner, inner));

            g2.setColor(new Color(45, 45, 45));
            g2.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
            String txt = (int) Math.round(progress * 100) + "%";
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(txt)) / 2;
            int ty = (getHeight() + fm.getAscent() / 2) / 2;
            g2.drawString(txt, tx, ty);

            g2.dispose();
        }
    }

    static class Quality {
        final boolean ok;
        final String reason;
        Quality(boolean ok, String reason) {
            this.ok = ok; this.reason = reason;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ICloudFinalApp_Full::new);
    }
}
