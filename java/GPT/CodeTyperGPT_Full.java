import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.text.Font;

import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.*;

public class CodeTyperGPT_Full extends Application {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    private static final String SAVE_FILE = "results.json";

    private Label snippetLabel;
    private TextArea inputArea;
    private Label resultLabel;
    private ComboBox<String> langBox;
    private ComboBox<String> levelBox;
    private Button statsBtn;
    private Button feedbackBtn;

    private String currentSnippet;
    private long startTime;

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void start(Stage stage) {
        stage.setTitle("💻 GPT 프로그래밍 타자 연습 (Full Edition)");

        // 상단 메뉴
        langBox = new ComboBox<>();
        langBox.getItems().addAll("Java", "Python", "C", "JavaScript");
        langBox.setValue("Java");

        levelBox = new ComboBox<>();
        levelBox.getItems().addAll("beginner", "intermediate", "advanced");
        levelBox.setValue("beginner");

        Button loadBtn = new Button("코드 가져오기");
        loadBtn.setOnAction(e -> {
            try { loadSnippet(); } 
            catch (Exception ex) { resultLabel.setText("❌ 오류: " + ex.getMessage()); }
        });

        statsBtn = new Button("📊 통계 보기");
        statsBtn.setOnAction(e -> showStats());

        feedbackBtn = new Button("💡 AI 피드백");
        feedbackBtn.setOnAction(e -> showFeedback());

        HBox topBox = new HBox(10, new Label("언어:"), langBox, new Label("난이도:"), levelBox, loadBtn, statsBtn, feedbackBtn);
        topBox.setPadding(new Insets(10));

        snippetLabel = new Label("GPT가 생성한 코드가 여기에 표시됩니다.");
        snippetLabel.setWrapText(true);
        snippetLabel.setFont(Font.font("Consolas", 14));
        snippetLabel.setStyle("-fx-text-fill: blue;");

        inputArea = new TextArea();
        inputArea.setPromptText("여기에 코드를 입력하세요...");
        inputArea.setWrapText(true);
        inputArea.setStyle("-fx-font-family: Consolas; -fx-font-size: 14;");
        inputArea.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> {
                    e.consume();
                    checkInput();
                }
            }
        });

        resultLabel = new Label("");
        resultLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");

        VBox layout = new VBox(10, topBox, snippetLabel, inputArea, resultLabel);
        layout.setPadding(new Insets(15));

        Scene scene = new Scene(layout, 800, 480);
        stage.setScene(scene);
        stage.show();
    }

    // GPT에서 코드 생성
    private void loadSnippet() throws Exception {
        String lang = langBox.getValue();
        String level = levelBox.getValue();
        snippetLabel.setText("⌛ GPT가 코드를 생성 중입니다...");
        resultLabel.setText("");
        inputArea.clear();

        new Thread(() -> {
            try {
                String snippet = getGPTSnippet(lang, level);
                currentSnippet = snippet;
                startTime = System.currentTimeMillis();

                javafx.application.Platform.runLater(() -> {
                    snippetLabel.setText(snippet);
                    resultLabel.setText("✅ 코드를 입력 후 Enter를 누르세요.");
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        snippetLabel.setText("❌ 오류: " + e.getMessage()));
            }
        }).start();
    }

    // 입력 검사
    private void checkInput() {
        if (currentSnippet == null || currentSnippet.isEmpty()) {
            resultLabel.setText("⚠️ 먼저 코드를 불러오세요.");
            return;
        }

        String userInput = inputArea.getText().trim();
        long end = System.currentTimeMillis();
        double seconds = (end - startTime) / 1000.0;

        int errors = countErrors(currentSnippet, userInput);
        double accuracy = ((double)(currentSnippet.length() - errors) / currentSnippet.length()) * 100;

        resultLabel.setText(String.format("⏱ %.2f초 | ✅ 정확도: %.1f%% | 오류: %d자", seconds, accuracy, errors));

        saveResult(langBox.getValue(), levelBox.getValue(), accuracy, seconds, errors, currentSnippet, userInput);
    }

    // JSON 저장
    private void saveResult(String lang, String level, double accuracy, double seconds, int errors, String code, String input) {
        try {
            List<SessionResult> results = loadResults();
            results.add(new SessionResult(lang, level, accuracy, seconds, errors, code, input, new Date()));
            try (Writer writer = new FileWriter(SAVE_FILE)) {
                gson.toJson(results, writer);
            }
        } catch (Exception e) {
            resultLabel.setText("❌ 저장 오류: " + e.getMessage());
        }
    }

    // JSON 로드
    private List<SessionResult> loadResults() {
        try {
            if (!new File(SAVE_FILE).exists()) return new ArrayList<>();
            try (Reader reader = new FileReader(SAVE_FILE)) {
                SessionResult[] arr = gson.fromJson(reader, SessionResult[].class);
                return new ArrayList<>(Arrays.asList(arr));
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // 통계 시각화 (LineChart)
    private void showStats() {
        List<SessionResult> results = loadResults();
        if (results.isEmpty()) {
            resultLabel.setText("📭 저장된 기록이 없습니다.");
            return;
        }

        Stage statsStage = new Stage();
        statsStage.setTitle("📊 학습 통계 그래프");

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        xAxis.setLabel("세션 번호");
        yAxis.setLabel("정확도 (%)");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("정확도 변화 추이");

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("정확도");

        for (int i = 0; i < results.size(); i++) {
            series.getData().add(new XYChart.Data<>(i + 1, results.get(i).accuracy));
        }

        lineChart.getData().add(series);

        VBox chartBox = new VBox(lineChart);
        Scene scene = new Scene(chartBox, 600, 400);
        statsStage.setScene(scene);
        statsStage.show();
    }

    // GPT 오타 피드백 생성
    private void showFeedback() {
        List<SessionResult> results = loadResults();
        if (results.isEmpty()) {
            resultLabel.setText("⚠️ 기록이 없습니다.");
            return;
        }

        SessionResult last = results.get(results.size() - 1);
        new Thread(() -> {
            try {
                String feedback = getGPTFeedback(last.language, last.originalCode, last.userInput);
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("💡 AI 피드백");
                    alert.setHeaderText("최근 입력에 대한 AI 분석");
                    alert.setContentText(feedback);
                    alert.showAndWait();
                });
            } catch (Exception e) {
                resultLabel.setText("❌ 피드백 오류: " + e.getMessage());
            }
        }).start();
    }

    // GPT 코드 피드백 호출
    private String getGPTFeedback(String language, String correct, String input) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null) throw new RuntimeException("OPENAI_API_KEY 환경 변수가 없습니다.");

        String prompt = String.format("""
            Compare these two %s codes:
            Correct code:
            %s
            User input:
            %s
            Explain briefly what mistakes or syntax issues the user made and how to fix them.
            """, language, correct, input);

        JsonObject req = new JsonObject();
        req.addProperty("model", MODEL);
        JsonArray msgs = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", prompt);
        msgs.add(msg);
        req.add("messages", msgs);
        req.addProperty("max_tokens", 150);

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(req.toString().getBytes("utf-8"));
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line.trim());

        JsonObject resp = JsonParser.parseString(sb.toString()).getAsJsonObject();
        String content = resp.getAsJsonArray("choices").get(0)
                .getAsJsonObject().getAsJsonObject("message").get("content").getAsString();

        return content.trim();
    }

    // GPT 코드 스니펫 생성
    private String getGPTSnippet(String language, String level) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null) throw new RuntimeException("OPENAI_API_KEY 환경 변수가 없습니다.");

        String prompt = String.format(
            "Generate one short %s code snippet (1-2 lines) that matches %s difficulty. Return only the code, no explanation.",
            language, level);

        JsonObject req = new JsonObject();
        req.addProperty("model", MODEL);
        JsonArray msgs = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", prompt);
        msgs.add(msg);
        req.add("messages", msgs);

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(req.toString().getBytes("utf-8"));
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line.trim());

        JsonObject resp = JsonParser.parseString(sb.toString()).getAsJsonObject();
        String content = resp.getAsJsonArray("choices").get(0)
                .getAsJsonObject().getAsJsonObject("message").get("content").getAsString();

        return content.trim();
    }

    private int countErrors(String target, String input) {
        int len = Math.min(target.length(), input.length());
        int errors = Math.abs(target.length() - input.length());
        for (int i = 0; i < len; i++) {
            if (target.charAt(i) != input.charAt(i)) errors++;
        }
        return errors;
    }

    public static void main(String[] args) {
        launch();
    }

    // 세션 데이터 구조
    static class SessionResult {
        String language;
        String level;
        double accuracy;
        double seconds;
        int errors;
        String originalCode;
        String userInput;
        Date date;

        SessionResult(String lang, String lvl, double acc, double sec, int err, String code, String input, Date d) {
            this.language = lang;
            this.level = lvl;
            this.accuracy = acc;
            this.seconds = sec;
            this.errors = err;
            this.originalCode = code;
            this.userInput = input;
            this.date = d;
        }
    }
}
