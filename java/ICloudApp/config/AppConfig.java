package config;

public class AppConfig {

    // 최소 해상도
    public static final int MIN_WIDTH  = 256;
    public static final int MIN_HEIGHT = 256;

    // 단색 판단 기준(분산)
    public static final double MONO_VARIANCE = 2.0;

    // 파일 스캔 단위
    public static final int SCAN_PARALLELISM =
            Math.max(4, Runtime.getRuntime().availableProcessors());

    // 해시 앞부분 읽기 크기 (1MB)
    public static final int HASH_READ_SIZE = 1024 * 1024;

    // UI 업데이트 최소 간격 (초)
    public static final double UI_UPDATE_INTERVAL = 0.15;
}
