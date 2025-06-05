package com.anonymity;

import com.anonymity.controller.dto.DifferentialPrivacyRequest;
import com.anonymity.util.DpTestDataGenerator;
import com.anonymity.util.PrivacyMetricsCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import java.io.FileWriter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;

@SpringBootTest
@AutoConfigureMockMvc
public class DpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @Test
    public void testDpDependent() throws Exception {
        // 生成測試數據
        List<Map<String, String>> testData = DpTestDataGenerator.generateTestData(2000);
        
        // The parameter epsilon
        double epsilon = 5d;
        double delta = 0.1d;

        // 創建請求
        DifferentialPrivacyRequest request = new DifferentialPrivacyRequest();
        request.setEpsilon(epsilon);
        request.setDelta(delta);
        request.setIsDataIndependent(false);
        request.setData(objectMapper.writeValueAsString(testData));

        // 記錄開始時間
        Instant startTime = Instant.now();

        // 發送請求
        MvcResult result = mockMvc.perform(post("/api/differential-privacy/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // 計算執行時間
        Duration executionTime = Duration.between(startTime, Instant.now());

        // 解析響應
        String responseContent = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseContent, Map.class);

        // 驗證響應
        assertNotNull(response);
        assertTrue(response.containsKey("epsilon"));
        assertTrue(response.containsKey("delta"));
        assertTrue(response.containsKey("isDataIndependent"));
        assertTrue(response.containsKey("result"));
        
        
        // 計算並顯示隱私指標
        @SuppressWarnings("unchecked")
        List<List<String>> anonymizedData = (List<List<String>>) response.get("result");
        Map<String, Double> metrics = PrivacyMetricsCalculator.calculateMetrics(testData, anonymizedData);
        
        //其他參數參考
//config.setSuppressionLimit(0.2d);
//config.setDPSearchBudget(0.1d);  // double dpSearchBudget = Additional epsilon for search process
//config.setHeuristicSearchStepLimit(160);
        System.out.printf("epsilon=%.2f,", epsilon);
        System.out.printf("delta=%.2f,dependent,", delta);
        System.out.printf("%d 筆,", testData.size());
        System.out.printf("%d 毫秒,%n", executionTime.toMillis());
        System.out.printf("信息損失: %.4f%n", metrics.get("informationLoss"));
        System.out.printf("效用損失: %.4f%n", metrics.get("utilityLoss"));
        System.out.printf("隱私保障: %.4f%n", metrics.get("privacyGuarantee"));

        // 儲存原始資料和匿名化結果
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String originalFilePath = String.format("C:\\Ken\\ori_dp_%.1f_%.3f_%d.xlsx", epsilon, delta, testData.size());
        String anonymizedFilePath = String.format("C:\\Ken\\dp_%.1f_%.3f_%d.xlsx", epsilon, delta, testData.size());
        
        // 確保目錄存在
        Files.createDirectories(Paths.get("C:\\Ken"));
        
        // 儲存檔案
        saveToExcel(testData, originalFilePath);
        saveAnonymizedToExcel(anonymizedData, anonymizedFilePath);
        
        System.out.println("\n=== 資料儲存完成 ===");
        System.out.println("原始資料: " + originalFilePath);
        System.out.println("匿名化結果: " + anonymizedFilePath);
    }

    @Test
    public void testDpIndependent() throws Exception {
        // 生成測試數據
        List<Map<String, String>> testData = DpTestDataGenerator.generateTestData(2000);
        
        // 創建請求
        DifferentialPrivacyRequest request = new DifferentialPrivacyRequest();
        request.setEpsilon(2d);
        request.setDelta(0.001d);
        request.setIsDataIndependent(false);
        request.setData(objectMapper.writeValueAsString(testData));

        // 記錄開始時間
        Instant startTime = Instant.now();

        // 發送請求
        MvcResult result = mockMvc.perform(post("/api/differential-privacy/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // 計算執行時間
        Duration executionTime = Duration.between(startTime, Instant.now());

        // 解析響應
        String responseContent = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseContent, Map.class);

        // 驗證響應
        assertNotNull(response);
        assertTrue(response.containsKey("epsilon"));
        assertTrue(response.containsKey("delta"));
        assertTrue(response.containsKey("isDataIndependent"));
        assertTrue(response.containsKey("result"));

        
        // 計算並顯示隱私指標
        @SuppressWarnings("unchecked")
        List<List<String>> anonymizedData = (List<List<String>>) response.get("result");
        Map<String, Double> metrics = PrivacyMetricsCalculator.calculateMetrics(testData, anonymizedData);
        
        System.out.println("\n=== 資料獨立差分隱私評估結果 ===");
        System.out.printf("原始資料筆數: %d%n", testData.size());
        System.out.printf("處理後筆數: %d%n", anonymizedData.size());
        System.out.printf("執行時間: %d 毫秒%n", executionTime.toMillis());
        System.out.printf("信息損失: %.4f%n", metrics.get("informationLoss"));
        System.out.printf("效用損失: %.4f%n", metrics.get("utilityLoss"));
        System.out.printf("隱私保障: %.4f%n", metrics.get("privacyGuarantee"));

        // 儲存原始資料和匿名化結果
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String originalFilePath = String.format("C:\\Ken\\original_data_independent_%.1f_%.3f_%d.xlsx", 2d, 0.001d, testData.size());
        String anonymizedFilePath = String.format("C:\\Ken\\anonymized_data_independent_%.1f_%.3f_%d.xlsx", 2d, 0.001d, testData.size());
        
        // 確保目錄存在
        Files.createDirectories(Paths.get("C:\\Ken"));
        
        // 儲存檔案
        saveToExcel(testData, originalFilePath);
        saveAnonymizedToExcel(anonymizedData, anonymizedFilePath);
        
        System.out.println("\n=== 資料儲存完成 ===");
        System.out.println("原始資料: " + originalFilePath);
        System.out.println("匿名化結果: " + anonymizedFilePath);
    }

    @Test
    public void testInvalidParameters() throws Exception {
        // 生成測試數據
        List<Map<String, String>> testData = DpTestDataGenerator.generateTestData(2000);
        
        // 創建請求（使用無效的參數）
        DifferentialPrivacyRequest request = new DifferentialPrivacyRequest();
        request.setEpsilon(-1.0);
        request.setDelta(0.1);
        request.setIsDataIndependent(false);
        request.setData(objectMapper.writeValueAsString(testData));

        // 發送請求並驗證錯誤響應
        mockMvc.perform(post("/api/differential-privacy/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testEmptyData() throws Exception {
        // 創建請求（空數據）
        DifferentialPrivacyRequest request = new DifferentialPrivacyRequest();
        request.setEpsilon(2.0);
        request.setDelta(0.1);
        request.setIsDataIndependent(false);
        request.setData("");

        // 發送請求並驗證錯誤響應
        mockMvc.perform(post("/api/differential-privacy/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDpDependentLoop() throws Exception {
        // 定義要測試的 epsilon 和 delta 值
        //double[] epsilons = {6.0,5.0,4.0,3.0, 2.0, 1.0};
        //double[] deltas = {0.99, 0.9, 0.7, 0.5, 0.3, 0.1};
        double[] epsilons = {4.0};
        double[] deltas = { 0.9, 0.7, 0.5};

        // 生成測試數據
        List<Map<String, String>> testData = DpTestDataGenerator.generateTestData(200);
        
        // 儲存原始資料（只儲存一次）
        String originalFilePath = String.format("C:\\Ken\\ori_dp_%d.xlsx", testData.size());
        Files.createDirectories(Paths.get("C:\\Ken"));
        saveToExcel(testData, originalFilePath);
        System.out.println("\n=== 原始資料已儲存至: " + originalFilePath);
        
        System.out.println("\n=== 開始執行差分隱私參數測試 ===");
        System.out.println("總測試組合數: " + (epsilons.length * deltas.length));
        
        // 建立結果收集列表
        List<Map<String, Object>> testResults = new ArrayList<>();
        
        int testCount = 0;
        for (double epsilon : epsilons) {
            for (double delta : deltas) {
                testCount++;
                System.out.printf("\n執行第 %d/35 次測試 (epsilon=%.2f, delta=%.5f)%n", 
                    testCount, epsilon, delta);
                
                if(epsilon==6.0 && delta==0.1){
                    continue;
                }

                // 設置 ObjectMapper 使用 UTF-8
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                
                // 創建請求
                DifferentialPrivacyRequest request = new DifferentialPrivacyRequest();
                request.setEpsilon(epsilon);
                request.setDelta(delta);
                request.setIsDataIndependent(false);

                // 檢查 testData 的編碼
                String testDataJson = objectMapper.writeValueAsString(testData);
                System.out.println("\n=== 測試資料編碼檢查 ===");
                System.out.println("testData JSON 編碼：" + StandardCharsets.UTF_8.name());
                System.out.println("testData JSON 內容：" + testDataJson.substring(0, Math.min(100, testDataJson.length())));

                request.setData(testDataJson);

                // 記錄開始時間
                Instant startTime = Instant.now();

                try {
                    // 執行請求
                    MvcResult result = mockMvc.perform(post("/api/differential-privacy/apply")
                            .contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                            .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isOk())
                            .andReturn();

                    // 計算執行時間
                    Duration executionTime = Duration.between(startTime, Instant.now());
                    
                    // 獲取響應內容
                    String responseContent = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                    System.out.println("\n=== 響應內容編碼檢查 ===");
                    System.out.println("響應內容編碼：" + StandardCharsets.UTF_8.name());
                    System.out.println("響應內容：" + responseContent.substring(0, Math.min(100, responseContent.length())));

                    // 解析響應
                    Map<String, Object> response = objectMapper.readValue(responseContent, Map.class);
                    @SuppressWarnings("unchecked")
                    List<List<String>> anonymizedData = (List<List<String>>) response.get("result");

                    // 檢查匿名化資料的編碼
                    System.out.println("\n=== 匿名化資料編碼檢查 ===");
                    if (!anonymizedData.isEmpty() && !anonymizedData.get(0).isEmpty()) {
                        String sampleData = anonymizedData.get(0).get(0);
                        System.out.println("匿名化資料編碼：" + StandardCharsets.UTF_8.name());
                        System.out.println("匿名化資料範例：" + sampleData);
                    }
                    
                    // 計算並顯示隱私指標
                    Map<String, Double> metrics = PrivacyMetricsCalculator.calculateMetrics(testData, anonymizedData);
                    
                    // 計算完全匿名化的筆數
                    int fullyAnonymizedCount = 0;
                    int ageAnonymizedCount = 0;
                    int genderAnonymizedCount = 0;
                    int dateAnonymizedCount = 0;
                    int confirmAnonymizedCount = 0;
                    
                    // 取得欄位索引
                    List<String> headers = anonymizedData.get(0);
                    System.out.println("匿名化資料欄位: " + String.join(", ", headers));
                    
                    // 檢查欄位數量
                    if (headers.size() < 4) {
                        System.out.println("警告：匿名化資料欄位數量不足，跳過此測試案例");
                        continue;
                    }
                    
                    // 假設欄位順序為：年齡、性別、通報日期、是否確診
                    int ageIndex = 0;        // 年齡
                    int genderIndex = 1;     // 性別
                    int dateIndex = 2;       // 通報日期
                    int confirmIndex = 3;    // 是否確診
                    
                    // 檢查是否所有欄位都被匿名化
                    boolean allColumnsAnonymized = true;
                    for (String header : headers) {
                        if (!header.equals("*")) {
                            allColumnsAnonymized = false;
                            break;
                        }
                    }
                    
                    if (allColumnsAnonymized) {
                        // 如果所有欄位都被匿名化，則所有筆數都計入各欄位的匿名化筆數
                        ageAnonymizedCount = anonymizedData.size();  // 減去標題行
                        genderAnonymizedCount = anonymizedData.size();
                        dateAnonymizedCount = anonymizedData.size();
                        confirmAnonymizedCount = anonymizedData.size();
                        fullyAnonymizedCount = anonymizedData.size();
                    } else {
                        // 正常處理每筆資料
                        for (int i = 1; i < anonymizedData.size(); i++) {
                            List<String> row = anonymizedData.get(i);
                            
                            // 檢查是否完全匿名化
                            boolean isFullyAnonymized = true;
                            for (String value : row) {
                                if (!value.equals("*")) {
                                    isFullyAnonymized = false;
                                    break;
                                }
                            }
                            if (isFullyAnonymized) {
                                fullyAnonymizedCount++;
                            }
                            
                            // 檢查各欄位匿名化情況
                            if (row.get(ageIndex).equals("*")) ageAnonymizedCount++;
                            if (row.get(genderIndex).equals("*")) genderAnonymizedCount++;
                            if (row.get(dateIndex).equals("*")) dateAnonymizedCount++;
                            if (row.get(confirmIndex).equals("*")) confirmAnonymizedCount++;
                        }
                    }
                    
                    // 判斷是否可用
                    int isUsable = 1;
                    double threshold = testData.size() * 0.2; // 計算20%的閾值
                    if (ageAnonymizedCount == testData.size() ||
                        genderAnonymizedCount == testData.size() ||
                        dateAnonymizedCount == testData.size() ||
                        confirmAnonymizedCount == testData.size() ||
                        fullyAnonymizedCount == testData.size() ||
                        ageAnonymizedCount > threshold ||
                        genderAnonymizedCount > threshold ||
                        dateAnonymizedCount > threshold ||
                        confirmAnonymizedCount > threshold ||
                        fullyAnonymizedCount > threshold) {
                        isUsable = 0;
                    }
                    
                    // 收集測試結果
                    Map<String, Object> testResult = new HashMap<>();
                    testResult.put("epsilon", epsilon);
                    testResult.put("delta", delta);
                    testResult.put("資料筆數", testData.size());
                    testResult.put("執行時間(毫秒)", executionTime.toMillis());
                    testResult.put("是否可用", isUsable);
                    testResult.put("完全匿名化筆數", fullyAnonymizedCount);
                    testResult.put("年齡匿名筆數", ageAnonymizedCount);
                    testResult.put("性別匿名筆數", genderAnonymizedCount);
                    testResult.put("通報日期匿名筆數", dateAnonymizedCount);
                    testResult.put("是否確診匿名筆數", confirmAnonymizedCount);
                    testResult.put("信息損失", metrics.get("informationLoss"));
                    testResult.put("效用損失", metrics.get("utilityLoss"));
                    testResult.put("隱私保障", metrics.get("privacyGuarantee"));
                    testResult.put("平均值差異", metrics.get("meanDifference"));
                    testResult.put("分類準確率", metrics.get("classificationAccuracy"));
                    testResults.add(testResult);
                    
                    System.out.printf("%d 毫秒,", executionTime.toMillis());
                    System.out.printf("完全匿名化筆數: %d,", fullyAnonymizedCount);
                    System.out.printf("年齡匿名: %d,", ageAnonymizedCount);
                    System.out.printf("性別匿名: %d,", genderAnonymizedCount);
                    System.out.printf("通報日期匿名: %d,", dateAnonymizedCount);
                    System.out.printf("是否確診匿名: %d,", confirmAnonymizedCount);
                    System.out.printf("信息損失: %.2f,", metrics.get("informationLoss"));
                    System.out.printf("效用損失: %.2f,", metrics.get("utilityLoss"));
                    System.out.printf("隱私保障: %.2f%n", metrics.get("privacyGuarantee"));
                    
                    // 儲存匿名化結果
                    String anonymizedFilePath = String.format("C:\\Ken\\dp_%.1f_%.2f_%d.xlsx", epsilon, delta, testData.size());
                    saveAnonymizedToExcel(anonymizedData, anonymizedFilePath);
                } catch (Exception e) {
                    System.out.printf("測試執行時發生錯誤 (epsilon=%.2f, delta=%.5f): %s%n", 
                        epsilon, delta, e.getMessage());
                    continue;
                }
            }
        }
        
        // 儲存所有測試結果到一個 Excel 檔案
        String summaryFilePath = "C:\\Ken\\dp_test_summary.xlsx";
        saveTestResultsToExcel(testResults, summaryFilePath);
        System.out.println("\n=== 測試結果摘要已儲存至: " + summaryFilePath);
        
        // 同時儲存為 TXT 檔案
        String txtFilePath = "C:\\Ken\\dp_test_summary.txt";
        saveTestResultsToTxt(testResults, txtFilePath);
        System.out.println("=== 測試結果摘要已儲存至: " + txtFilePath);
        
        System.out.println("\n=== 所有測試完成 ===");
    }
    
    private void saveTestResultsToExcel(List<Map<String, Object>> results, String filePath) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            XSSFSheet sheet = workbook.createSheet("測試結果");
            
            // 寫入標題行
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "epsilon", 
                "delta", 
                "資料筆數", 
                "執行時間(毫秒)", 
                "完全匿名化筆數",
                "年齡匿名筆數",
                "性別匿名筆數",
                "通報日期匿名筆數",
                "是否確診匿名筆數",
                "信息損失", 
                "效用損失", 
                "隱私保障", 
                "平均值差異",
                "分類準確率",
                "是否可用"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            // 寫入資料行
            for (int i = 0; i < results.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> result = results.get(i);
                
                int col = 0;
                row.createCell(col++).setCellValue(String.format("%.2f", (Double) result.get("epsilon")));
                row.createCell(col++).setCellValue(String.format("%.2f", (Double) result.get("delta")));
                row.createCell(col++).setCellValue((Integer) result.get("資料筆數"));
                row.createCell(col++).setCellValue((Long) result.get("執行時間(毫秒)"));
                row.createCell(col++).setCellValue((Integer) result.get("完全匿名化筆數"));
                row.createCell(col++).setCellValue((Integer) result.get("年齡匿名筆數"));
                row.createCell(col++).setCellValue((Integer) result.get("性別匿名筆數"));
                row.createCell(col++).setCellValue((Integer) result.get("通報日期匿名筆數"));
                row.createCell(col++).setCellValue((Integer) result.get("是否確診匿名筆數"));
                row.createCell(col++).setCellValue(String.format("%.2f", (Double) result.get("信息損失")));
                row.createCell(col++).setCellValue(String.format("%.2f", (Double) result.get("效用損失")));
                row.createCell(col++).setCellValue(String.format("%.2f", (Double) result.get("隱私保障")));
                
                // 處理可能為 null 的值
                Object meanDiff = result.get("平均值差異");
                Object classAcc = result.get("分類準確率");
                
                row.createCell(col++).setCellValue(meanDiff != null ? String.format("%.2f", (Double) meanDiff) : "0.00");
                row.createCell(col++).setCellValue(classAcc != null ? String.format("%.2f", (Double) classAcc) : "0.00");
                
                // 是否可用
                row.createCell(col++).setCellValue((Integer) result.get("是否可用"));
            }
            
            // 自動調整欄寬
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 寫入檔案
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }catch(Exception e){
                System.out.println("寫入檔案時發生錯誤: " + e.getMessage());
            }
        } finally {
            //workbook=null;
        }
    }

    private void saveTestResultsToTxt(List<Map<String, Object>> results, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            
            // 寫入標題行
            String[] headers = {
                "epsilon", 
                "delta", 
                "資料筆數", 
                "執行時間(毫秒)", 
                "完全匿名化筆數",
                "年齡匿名筆數",
                "性別匿名筆數",
                "通報日期匿名筆數",
                "是否確診匿名筆數",
                "信息損失", 
                "效用損失", 
                "隱私保障", 
                "平均值差異",
                "分類準確率",
                "是否可用"
            };
            writer.write(String.join(",", headers));
            writer.newLine();
            
            // 寫入資料行
            for (Map<String, Object> result : results) {
                List<String> row = new ArrayList<>();
                row.add(String.format("%.2f", (Double) result.get("epsilon")));
                row.add(String.format("%.2f", (Double) result.get("delta")));
                row.add(String.valueOf(result.get("資料筆數")));
                row.add(String.valueOf(result.get("執行時間(毫秒)")));
                row.add(String.valueOf(result.get("完全匿名化筆數")));
                row.add(String.valueOf(result.get("年齡匿名筆數")));
                row.add(String.valueOf(result.get("性別匿名筆數")));
                row.add(String.valueOf(result.get("通報日期匿名筆數")));
                row.add(String.valueOf(result.get("是否確診匿名筆數")));
                row.add(String.format("%.2f", (Double) result.get("信息損失")));
                row.add(String.format("%.2f", (Double) result.get("效用損失")));
                row.add(String.format("%.2f", (Double) result.get("隱私保障")));
                
                // 處理可能為 null 的值
                Object meanDiff = result.get("平均值差異");
                Object classAcc = result.get("分類準確率");
                
                row.add(meanDiff != null ? String.format("%.2f", (Double) meanDiff) : "0.00");
                row.add(classAcc != null ? String.format("%.2f", (Double) classAcc) : "0.00");
                
                // 是否可用
                row.add(String.valueOf(result.get("是否可用")));
                
                writer.write(String.join(",", row));
                writer.newLine();
            }
        } catch (Exception e) {
            System.out.println("寫入 TXT 檔案時發生錯誤: " + e.getMessage());
        }
    }

    private void saveToExcel(List<Map<String, String>> data, String filePath) throws IOException {
        if (data.isEmpty()) return;
        
        // 檢查檔案是否存在
        if (Files.exists(Paths.get(filePath))) {
            System.out.println("檔案已存在，將進行覆蓋: " + filePath);
        }
        
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            XSSFSheet sheet = workbook.createSheet("資料");
            
            // 寫入標題行
            String[] headers = data.get(0).keySet().toArray(new String[0]);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            // 寫入資料行
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, String> rowData = data.get(i);
                for (int j = 0; j < headers.length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(rowData.get(headers[j]));
                }
            }
            
            // 自動調整欄寬
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 寫入檔案
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        } finally {
            // 不需要手動關閉 workbook，讓它被垃圾回收
        }
    }

    private void saveAnonymizedToExcel(List<List<String>> data, String filePath) throws IOException {
        if (data.isEmpty()) return;
        
        // 檢查檔案是否存在
        if (Files.exists(Paths.get(filePath))) {
            System.out.println("檔案已存在，將進行覆蓋: " + filePath);
        }

        // 檢查資料編碼
        System.out.println("\n=== Excel 儲存編碼檢查 ===");
        if (!data.isEmpty() && !data.get(0).isEmpty()) {
            String sampleData = data.get(0).get(0);
            System.out.println("Excel 儲存編碼：" + StandardCharsets.UTF_8.name());
            System.out.println("儲存資料範例：" + sampleData);
        }
        
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            XSSFSheet sheet = workbook.createSheet("匿名化資料");
            
            // 寫入資料行
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i);
                List<String> rowData = data.get(i);
                if(i<3){
                    System.out.println("寫入第 " + (i+1) + " 行資料：" + rowData);
                }
                for (int j = 0; j < rowData.size(); j++) {
                    Cell cell = row.createCell(j);
                    String value = rowData.get(j);
                    if (value != null) {
                        // 確保使用 UTF-8 編碼
                        cell.setCellValue(new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
                    } else {
                        cell.setCellValue("");
                    }
                }
            }
            
            // 自動調整欄寬
            for (int i = 0; i < data.get(0).size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 寫入檔案
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }catch(Exception e){
                System.out.println("寫入檔案時發生錯誤: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("寫入檔案時發生錯誤: " + e.getMessage());
        }
    }

    // 輔助方法：將位元組陣列轉換為十六進制字串
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    // 輔助方法：檢查是否為 UTF-8 編碼
    private boolean isUTF8(byte[] bytes) {
        try {
            String str = new String(bytes, StandardCharsets.UTF_8);
            System.out.println(str); 
            byte[] newBytes = str.getBytes(StandardCharsets.UTF_8);
            return java.util.Arrays.equals(bytes, newBytes);
        } catch (Exception e) {
            return false;
        }
    }

    // 輔助方法：檢查是否為 Big5 編碼
    private boolean isBig5(byte[] bytes) {
        try {
            String str = new String(bytes, "Big5");
            System.out.println(str);
            byte[] newBytes = str.getBytes("Big5");
            return java.util.Arrays.equals(bytes, newBytes);
        } catch (Exception e) {
            return false;
        }
    }

    // 輔助方法：檢查是否為 Latin-1 編碼
    private boolean isLatin1(byte[] bytes) {
        try {
            String str = new String(bytes, StandardCharsets.ISO_8859_1);
            System.out.println(str);
            byte[] newBytes = str.getBytes(StandardCharsets.ISO_8859_1);
            return java.util.Arrays.equals(bytes, newBytes);
        } catch (Exception e) {
            return false;
        }
    }

} 