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
    
    public static List<Map<String, String>> generateTestData(int count) {
        List<Map<String, String>> data = new ArrayList<>();
        LocalDate startDate = LocalDate.now().minusDays(30);
        
        for (int i = 0; i < count; i++) {
            Map<String, String> row = new HashMap<>();
            
            // 生成年齡
            int age = 5 + random.nextInt(41);
            row.put("年齡", String.valueOf(age));
            
            // 生成性別
            row.put("性別", GENDERS[random.nextInt(GENDERS.length)]);
            
            // 生成郵遞區號 (100-116)
            int zipcode = 166 + random.nextInt(88);
            //row.put("郵遞區號", String.valueOf(zipcode));
            
            // 生成縣市
            //row.put("縣市", CITIES[random.nextInt(CITIES.length)]);
            

            // 生成通報日期 (過去30天內)
            LocalDate reportDate = startDate.plusDays(random.nextInt(30));
            row.put("通報日期", reportDate.format(dateFormatter));
           
            // 生成是否確診
            boolean isConfirmed = random.nextBoolean();
            row.put("是否確診", isConfirmed ? "1" : "0");

            // 生成疾病
            row.put("疾病", DISEASES[random.nextInt(DISEASES.length)]);
            
             
            // 生成檢驗結果
            //row.put("檢驗結果", TEST_RESULTS[random.nextInt(TEST_RESULTS.length)]);
            

            
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