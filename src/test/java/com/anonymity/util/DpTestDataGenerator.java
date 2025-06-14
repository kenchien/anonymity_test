package com.anonymity.util;

import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DpTestDataGenerator {
    
    private static final String[] GENDERS = {"男", "女"};
    
    private static final String[] CITIES = {"台北市", 
    "新北市", 
    "桃園市", 
    "台中市", 
    "台南市", 
    "高雄市", 
    "基隆市", 
    "新竹市", 
    "嘉義市",
    "宜蘭縣",
    "花蓮縣",
    "台東縣",
    "屏東縣",
    "彰化縣",
    "南投縣"};
    
    
    private static final String[] DISEASES = {"新冠肺炎", "流感", "登革熱", "腸病毒", "結核病"};
    private static final String[] TEST_RESULTS = {"陽性", "陰性", "待確認"};
    private static final Random random = new Random();
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    
    // 身分證字號生成相關常數
    private static final String[] AREA_CODES = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    private static final int[] AREA_WEIGHTS = {10, 11, 12, 13, 14, 15, 16, 17, 34, 18, 19, 20, 21, 22, 35, 23, 24, 25, 26, 27, 28, 29, 32, 30, 31, 33};
    
    // 生成有效的台灣身分證字號
    private static String generateTaiwanID() {
        // 隨機選擇地區代碼
        int areaIndex = random.nextInt(AREA_CODES.length);
        String areaCode = AREA_CODES[areaIndex];
        int areaWeight = AREA_WEIGHTS[areaIndex];
        
        // 生成8位數字
        StringBuilder numbers = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            numbers.append(random.nextInt(10));
        }
        
        // 計算檢查碼
        int sum = (areaWeight / 10) + (areaWeight % 10 * 9);
        for (int i = 0; i < 8; i++) {
            sum += (numbers.charAt(i) - '0') * (8 - i);
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        
        return areaCode + numbers + checkDigit;
    }
    
    public static List<Map<String, String>> generateTestData(int count) {
        List<Map<String, String>> data = new ArrayList<>();
        LocalDate startDate = LocalDate.now().minusDays(30);
        
        for (int i = 0; i < count; i++) {
            Map<String, String> row = new LinkedHashMap<>();  // 使用 LinkedHashMap 來保持插入順序
            
            // 生成身分證字號
            row.put("身分證", generateTaiwanID());
            
            // 生成年齡
            int age = 5 + random.nextInt(41);
            row.put("年齡", String.valueOf(age));
            
            // 生成性別
            row.put("性別", GENDERS[random.nextInt(GENDERS.length)]);
            
            // 生成縣市
            row.put("縣市", CITIES[random.nextInt(5)]);
            
            // 生成通報日期 (過去30天內)
            LocalDate reportDate = startDate.plusDays(random.nextInt(30));
            row.put("通報日期", reportDate.format(dateFormatter));
           
            // 生成是否確診
            boolean isConfirmed = random.nextBoolean();
            row.put("是否確診", isConfirmed ? "1" : "0");

            // 生成疾病
            row.put("疾病", DISEASES[random.nextInt(DISEASES.length)]);
            
            // 生成檢驗結果
            row.put("檢驗結果", TEST_RESULTS[random.nextInt(TEST_RESULTS.length)]);
            
            data.add(row);
        }
        
        return data;
    }
    
    public static Map<String, Object> generateTestRequest(int count, int k, int l) {
        Map<String, Object> request = new HashMap<>();
        
        request.put("data", generateTestData(count));
        request.put("quasiIdentifiers", Arrays.asList("年齡", "性別", "郵遞區號", "縣市"));
        request.put("sensitiveAttributes", Arrays.asList("疾病", "檢驗結果", "是否確診"));
        request.put("k", k);
        request.put("l", l);
        
        return request;
    }
} 