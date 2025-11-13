import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

public class ICloudFinalApp_Full extends JFrame {

    // ==== UI ====
    private final DefaultListModel<File> foldersModel = new DefaultListModel<>();
    private final JList<File> foldersList = new JList<>(foldersModel);
    private final JButton addFolderBtn = new JButton("＋ 폴더 추가");
    private final JButton removeFolderBtn = new JButton("－ 제거");
    private final JButton startBtn = new JButton("🚀 시작");
    private final JLabel statusLbl = new JLabel("대상 폴더를 추가하세요", SwingConstants.CENTER);
    private final JTextArea logArea = new JTextArea(12, 48);
    private final JScrollPane logPane = new JScrollPane(logArea);
    private final ProgressCircle circle = new ProgressCircle();

    // ==== 옵션(필요시 조정) ====
    private static final int MIN_W = 256, MIN_H = 256; // 너무 작은 이미지 컷
    private static final double MONO_VAR = 2.0;       // 거의 단색(분산 임계)

    // ==== 포맷 집합 ====
    private final Set<String> COMPAT = setOf("jpg","jpeg","png","heic","heif","gif");
    private final Set<String> CONVERT = setOf("bmp","tif","tiff","webp");

    public ICloudFinalApp_Full() {
        setTitle("📸 iCloud 올리기 전 마지막 단계 (완전판)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        // 좌측: 폴더 목록
        foldersList.setVisibleRowCount(10);
        foldersList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel l = new JLabel(value.getAbsolutePath());
            l.setOpaque(true);
            l.setBorder(BorderFactory.createEmptyBorder(6,8,6,8));
            l.setBackground(isSelected ? new Color(230,240,255) : Color.WHITE);
            return l;
        });
        JPanel left = new JPanel(new BorderLayout(8,8));
        left.setBorder(BorderFactory.createTitledBorder("📂 대상 폴더 (여러 개 추가 가능)"));
        left.add(new JScrollPane(foldersList), BorderLayout.CENTER);
        JPanel leftBtns = new JPanel(new GridLayout(1,2,8,8));
        style(addFolderBtn,false); style(removeFolderBtn,false);
        leftBtns.add(addFolderBtn); leftBtns.add(removeFolderBtn);
        left.add(leftBtns, BorderLayout.SOUTH);
        add(left, BorderLayout.WEST);

        // 중앙: 원형 퍼센트
        JPanel center = new JPanel(new BorderLayout());
        circle.setPreferredSize(new Dimension(360, 360));
        center.add(circle, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // 우측: 로그
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logPane.setBorder(BorderFactory.createTitledBorder("📋 실시간 로그"));
        add(logPane, BorderLayout.EAST);

        // 하단: 시작/상태
        JPanel bottom = new JPanel(new BorderLayout(8,8));
        JPanel runBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 16,4));
        style(startBtn,true);
        runBar.add(startBtn);
        bottom.add(runBar, BorderLayout.NORTH);
        statusLbl.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        bottom.add(statusLbl, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        // 타이틀
        JLabel title = new JLabel("☁️ iCloud 업로드 준비 (완전판)", SwingConstants.CENTER);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        // 이벤트
        addFolderBtn.addActionListener(this::onAddFolder);
        removeFolderBtn.addActionListener(e -> {
            for (File f : foldersList.getSelectedValuesList()) foldersModel.removeElement(f);
            statusLbl.setText("폴더 수: " + foldersModel.size());
        });
        startBtn.addActionListener(this::onStart);
        startBtn.setEnabled(false);

        setVisible(true);
    }

    private void style(JButton b, boolean primary) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (primary) { b.setBackground(new Color(50,140,255)); b.setForeground(Color.WHITE); }
        else b.setBackground(new Color(242,242,242));
    }

    private void onAddFolder(ActionEvent e) {
        JFileChooser ch = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        ch.setDialogTitle("대상 폴더 선택");
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = ch.getSelectedFile();
            if (!foldersModel.contains(f)) foldersModel.addElement(f);
            statusLbl.setText("폴더 수: " + foldersModel.size());
            startBtn.setEnabled(foldersModel.size() > 0);
            log("📂 추가됨 → " + f.getAbsolutePath());
        }
    }

    private void onStart(ActionEvent e) {
        if (foldersModel.isEmpty()) return;
        startBtn.setEnabled(false); addFolderBtn.setEnabled(false); removeFolderBtn.setEnabled(false);
        logArea.setText(""); circle.setProgress(0); statusLbl.setText("준비 중...");
        new Thread(this::runAll).start();
    }

    // ================== 전체 파이프라인 ==================
    private void runAll() {
        long globalStart = System.nanoTime();
        AtomicInteger globalTotal = new AtomicInteger();
        AtomicInteger globalDone = new AtomicInteger();
        AtomicInteger globalKept = new AtomicInteger();
        AtomicInteger globalConv = new AtomicInteger();
        AtomicInteger globalDup  = new AtomicInteger();
        AtomicInteger globalFail = new AtomicInteger();

        // 폴더별 순차, 폴더 내부는 멀티스레드
        for (int idx = 0; idx < foldersModel.size(); idx++) {
            File root = foldersModel.get(idx);
            processOneRoot(root, globalStart, globalTotal, globalDone, globalKept, globalConv, globalDup, globalFail);
        }

        long elapsed = System.nanoTime() - globalStart;
        double sec = elapsed / 1e9;
        double rate = globalDone.get() > 0 ? globalDone.get() / sec : 0;

        ui(() -> {
            circle.setProgress(1.0);
            statusLbl.setText(String.format("✅ 전체 완료 | 총 %d / 유지 %d / 변환 %d / 중복 %d / 실패 %d | %.1f개/초",
                    globalDone.get(), globalKept.get(), globalConv.get(), globalDup.get(), globalFail.get(), rate));
            startBtn.setEnabled(true); addFolderBtn.setEnabled(true); removeFolderBtn.setEnabled(true);
            log("✅ 작업 전체 완료");
        });
    }

    private void processOneRoot(File root,
                                long globalStart,
                                AtomicInteger gTotal, AtomicInteger gDone, AtomicInteger gKept,
                                AtomicInteger gConv, AtomicInteger gDup, AtomicInteger gFail) {

        log("────────────────────────────────────────────────────");
        log("📁 처리 시작: " + root.getAbsolutePath());
        File outDir = new File(root, "__iOS_READY");    outDir.mkdirs();
        File failDir = new File(root, "__FAILED");      failDir.mkdirs();

        // 스캔(스트리밍)
        List<Path> paths = new ArrayList<>();
        try {
            Files.walkFileTree(root.toPath(), new SimpleFileVisitor<>() {
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (isTarget(n)) paths.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log("⚠ 스캔 오류: " + e.getMessage());
            return;
        }

        final int total = paths.size();
        gTotal.addAndGet(total);
        if (total == 0) {
            log("ℹ 처리할 이미지 없음");
            return;
        }
        log("🔎 스캔 완료: " + total + "개");

        // 리포트/매핑 로그 준비
        File report = new File(outDir, "report.txt");
        File mapping = new File(outDir, "mapping.csv");
        try (PrintWriter rep = new PrintWriter(new OutputStreamWriter(new FileOutputStream(report, true), "UTF-8"));
             PrintWriter map = new PrintWriter(new OutputStreamWriter(new FileOutputStream(mapping, true), "UTF-8"))) {

            String startTs = now();
            rep.println("=== REPORT START ===");
            rep.println("Root: " + root.getAbsolutePath());
            rep.println("Start: " + startTs);

            // 중복 관리
            ConcurrentHashMap<String, Path> seen = new ConcurrentHashMap<>();

            // 스레드풀
            int threads = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
            ExecutorService pool = Executors.newFixedThreadPool(threads);

            AtomicInteger seq = new AtomicInteger(0);
            AtomicInteger kept = new AtomicInteger(0);
            AtomicInteger conv = new AtomicInteger(0);
            AtomicInteger dups = new AtomicInteger(0);
            AtomicInteger fail = new AtomicInteger(0);
            AtomicInteger done = new AtomicInteger(0);

            long localStart = System.nanoTime();

            for (Path p : paths) {
                pool.submit(() -> {
                    String name = p.getFileName().toString();
                    try {
                        // 1) 중복
                        String key = quickHash(p);
                        if (key != null && seen.putIfAbsent(key, p) != null) {
                            moveToTrash(p.toFile());
                            dups.incrementAndGet(); gDup.incrementAndGet();
                            log("🗑 중복 제거 → " + name);
                            updateProgress(globalStart, done.incrementAndGet(), total, gDone.incrementAndGet(), gTotal.get());
                            return;
                        }

                        // 2) 포맷/품질 체크
                        String ext = extLower(name);
                        boolean compat = COMPAT.contains(ext);
                        String iosName = iosName(seq.incrementAndGet(), compat ? ext : "jpg");
                        File dest = new File(outDir, iosName);

                        if (compat) {
                            // 품질 필터
                            Quality q = checkQuality(p);
                            if (!q.ok) {
                                moveToTrash(p.toFile());
                                dups.incrementAndGet(); gDup.incrementAndGet();
                                log("🧹 품질 제외(" + q.reason + ") → " + name);
                            } else {
                                safeMove(p.toFile(), dest);
                                kept.incrementAndGet(); gKept.incrementAndGet();
                                mapLine(map, p, dest);
                                log("📦 이동 → " + iosName);
                            }
                        } else if (CONVERT.contains(ext)) {
                            // 변환 시도
                            if (tryConvertToJpg(p.toFile(), dest)) {
                                moveToTrash(p.toFile());
                                conv.incrementAndGet(); gConv.incrementAndGet();
                                mapLine(map, p, dest);
                                log("⚙ 변환 → " + iosName);
                            } else {
                                // 실패 → FAIL 폴더로 이동
                                File tgt = new File(failDir, name);
                                safeMove(p.toFile(), tgt);
                                fail.incrementAndGet(); gFail.incrementAndGet();
                                log("❌ 변환 실패(FAIL로 이동) → " + name);
                            }
                        } else {
                            // 기타 포맷은 데이터 보존 이동
                            File keep = new File(outDir, iosName(seq.get(), ext));
                            safeMove(p.toFile(), keep);
                            kept.incrementAndGet(); gKept.incrementAndGet();
                            mapLine(map, p, keep);
                            log("📂 기타 이동 → " + keep.getName());
                        }
                    } catch (Exception ex) {
                        try {
                            File tgt = new File(failDir, name);
                            if (Files.exists(p)) safeMove(p.toFile(), tgt);
                        } catch (Exception ignore) {}
                        fail.incrementAndGet(); gFail.incrementAndGet();
                        log("❌ 오류: " + name + " → " + ex.getMessage());
                    } finally {
                        updateProgress(globalStart, done.incrementAndGet(), total, gDone.incrementAndGet(), gTotal.get());
                    }
                });
            }

            pool.shutdown();
            try { pool.awaitTermination(99, TimeUnit.HOURS); } catch (InterruptedException ignored) {}

            // ZIP 압축
            File zip = new File(outDir.getParentFile(), "__iOS_READY.zip");
            try { zipFolder(outDir.toPath(), zip.toPath()); log("🗜 ZIP 생성 → " + zip.getName()); }
            catch (Exception ze) { log("⚠ ZIP 실패: " + ze.getMessage()); }

            long locElapsed = System.nanoTime() - localStart;
            double sec = locElapsed / 1e9;
            double rate = done.get() > 0 ? done.get() / sec : 0;

            // 리포트 작성
            rep.printf(Locale.ROOT, "Total: %d, Kept: %d, Converted: %d, Duplicates: %d, Failed: %d%n",
                    done.get(), kept.get(), conv.get(), dups.get(), fail.get());
            rep.printf(Locale.ROOT, "Elapsed: %.1fs, Rate: %.1f files/s%n", sec, rate);
            rep.println("End: " + now());
            rep.println("=== REPORT END ===");
            rep.flush();

            log(String.format("📑 리포트 저장 | 총 %d / 유지 %d / 변환 %d / 중복 %d / 실패 %d | %.1f개/초",
                    done.get(), kept.get(), conv.get(), dups.get(), fail.get(), rate));

        } catch (Exception ioe) {
            log("⚠ 리포트/매핑 파일 오류: " + ioe.getMessage());
        }
    }

    // ================== 보조 로직 ==================
    private boolean isTarget(String n) {
        int i = n.lastIndexOf('.');
        if (i < 0) return false;
        String ext = n.substring(i+1);
        return COMPAT.contains(ext) || CONVERT.contains(ext);
    }

    private static String extLower(String n) {
        int i = n.lastIndexOf('.');
        return (i<0) ? "" : n.substring(i+1).toLowerCase(Locale.ROOT);
    }

    private static String iosName(int seq, String ext) {
        return String.format("IMG_%05d.%s", seq, ext.toUpperCase(Locale.ROOT));
    }

    private void safeMove(File src, File dest) throws IOException {
        dest.getParentFile().mkdirs();
        try {
            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean tryConvertToJpg(File src, File destJpg) {
        try {
            BufferedImage img = ImageIO.read(src);
            if (img == null) return false;
            // 품질 필터 적용
            Quality q = qualityOf(img);
            if (!q.ok) return false;
            destJpg.getParentFile().mkdirs();
            return ImageIO.write(img, "jpg", destJpg);
        } catch (Exception e) {
            return false;
        }
    }

    // 빠른 중복 키: 파일 크기 + 앞 1MB SHA-256
    private String quickHash(Path f) {
        try {
            long size = Files.size(f);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new BufferedInputStream(Files.newInputStream(f))) {
                byte[] buf = new byte[1024*1024];
                int r = in.read(buf);
                if (r > 0) md.update(buf, 0, r);
            }
            byte[] d = md.digest();
            StringBuilder sb = new StringBuilder(d.length*2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return size + ":" + sb;
        } catch (Exception e) {
            return null;
        }
    }

    // 품질 검사: 너무 작거나 거의 단색이면 제외
    private Quality checkQuality(Path p) {
        try {
            BufferedImage img = ImageIO.read(p.toFile());
            if (img == null) return new Quality(false, "read-null");
            return qualityOf(img);
        } catch (Exception e) {
            return new Quality(false, "read-fail");
        }
    }

    private Quality qualityOf(BufferedImage img) {
        if (img.getWidth() < MIN_W || img.getHeight() < MIN_H)
            return new Quality(false, "too-small");
        // 샘플링 분산
        long sum=0, sumsq=0, cnt=0;
        int stepX = Math.max(1, img.getWidth()/64);
        int stepY = Math.max(1, img.getHeight()/64);
        for (int y=0; y<img.getHeight(); y+=stepY) {
            for (int x=0; x<img.getWidth(); x+=stepX) {
                int rgb = img.getRGB(x,y);
                int g = ((rgb>>16)&0xff + (rgb>>8)&0xff + (rgb&0xff)) / 3;
                sum += g; sumsq += (long)g*g; cnt++;
            }
        }
        double mean = sum / (double)cnt;
        double var = sumsq / (double)cnt - mean*mean;
        if (var < MONO_VAR) return new Quality(false, "mono-ish");
        return new Quality(true, "ok");
    }

    private void moveToTrash(File f) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().moveToTrash(f);
                return;
            }
            f.delete();
        } catch (Exception e) { f.delete(); }
    }

    private void updateProgress(long globalStart, int localDone, int localTotal, int gDone, int gTotal) {
        double p = Math.max(0, Math.min(1, gDone / (double) Math.max(1,gTotal)));
        long now = System.nanoTime();
        double elapsed = (now - globalStart) / 1e9;
        double rate = gDone > 0 ? gDone / elapsed : 0.0;
        double remain = (gTotal - gDone) / Math.max(1e-6, rate);
        String eta = fmtDuration(remain);

        ui(() -> {
            circle.setProgress(p);
            statusLbl.setText(String.format("진행 %d%% | 처리 %d/%d | 속도 %.1f개/초 | 남은 시간 %s",
                    (int)Math.round(p*100), gDone, gTotal, rate, eta));
        });
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private static String fmtDuration(double sec) {
        if (sec < 0) sec = 0;
        long s = Math.round(sec);
        long h = s/3600; s%=3600;
        long m = s/60;   s%=60;
        return String.format("%02d:%02d:%02d", h,m,s);
    }

    private void zipFolder(Path folder, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipFile)))) {
            Files.walkFileTree(folder, new SimpleFileVisitor<>() {
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path rel = folder.relativize(file);
                    ZipEntry e = new ZipEntry(rel.toString().replace('\\','/'));
                    zos.putNextEntry(e);
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void mapLine(PrintWriter map, Path src, File dest) {
        synchronized (map) {
            map.printf("\"%s\",\"%s\"%n", src.toFile().getAbsolutePath().replace("\"","\"\""),
                    dest.getAbsolutePath().replace("\"","\"\""));
            map.flush();
        }
    }

    private static Set<String> setOf(String... s) {
        Set<String> r = new HashSet<>();
        for (String x : s) r.add(x.toLowerCase(Locale.ROOT));
        return r;
    }
    private void ui(Runnable r) { SwingUtilities.invokeLater(r); }
    private void log(String msg) {
        ui(() -> {
            logArea.append(msg + "\n");
            if (logArea.getLineCount() > 250) {  // 최근 200줄 유지
                try { int cut = logArea.getLineStartOffset(50); logArea.replaceRange("", 0, cut); }
                catch (Exception ignored) {}
            }
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // 원형 퍼센트 UI
    static class ProgressCircle extends JPanel {
        private double p = 0;
        void setProgress(double v) { p = Math.max(0, Math.min(1, v)); repaint(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 40;
            int x=(getWidth()-size)/2, y=(getHeight()-size)/2;
            g2.setColor(new Color(235,235,238)); g2.fill(new Ellipse2D.Double(x,y,size,size));
            g2.setColor(new Color(50,140,255)); g2.fill(new Arc2D.Double(x,y,size,size,90,-p*360,Arc2D.PIE));
            g2.setColor(getBackground()); g2.fill(new Ellipse2D.Double(x+22,y+22,size-44,size-44));
            g2.setColor(Color.DARK_GRAY); g2.setFont(new Font("맑은 고딕", Font.BOLD, 28));
            String t=(int)Math.round(p*100)+"%"; FontMetrics fm=g2.getFontMetrics();
            g2.drawString(t,(getWidth()-fm.stringWidth(t))/2,(getHeight()+fm.getAscent()/2)/2);
            g2.dispose();
        }
    }

    static class Quality {
        final boolean ok; final String reason;
        Quality(boolean ok, String reason){ this.ok=ok; this.reason=reason; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ICloudFinalApp_Full::new);
    }
}
