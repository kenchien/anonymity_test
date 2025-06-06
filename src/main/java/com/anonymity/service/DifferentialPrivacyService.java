package com.anonymity.service;

import com.anonymity.controller.dto.DifferentialPrivacyRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.deidentifier.arx.*;
import org.deidentifier.arx.criteria.EDDifferentialPrivacy;
import org.deidentifier.arx.criteria.EntropyLDiversity;
import org.deidentifier.arx.criteria.DistinctLDiversity;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.AttributeType.Hierarchy.DefaultHierarchy;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.deidentifier.arx.DataGeneralizationScheme.GeneralizationDegree;

@Service
public class DifferentialPrivacyService {
    
    private final ObjectMapper objectMapper;
    
    public DifferentialPrivacyService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    private void setupHierarchies(Data.DefaultData arxData, String[] attributes) {
        // System.out.println("\n=== 開始設定欄位類型與泛化層級 ===");
        // System.out.println("總欄位數：" + attributes.length);
        
        // 定義年齡層級
        DefaultHierarchy ageHierarchy = Hierarchy.create();
        for (int age = 0; age <= 99; age++) {
            int decade = (age / 10) * 10;
            int halfDecade = (age / 5) * 5;
            String ageStr = String.valueOf(age);
            String decadeRange = String.format("%d-%d", decade, decade + 9);
            String halfDecadeRange = String.format("%d-%d", halfDecade, halfDecade + 4);
            String ageGroup;
            
            if (age < 20) ageGroup = "0-19";
            else if (age < 40) ageGroup = "20-39";
            else if (age < 60) ageGroup = "40-59";
            else if (age < 80) ageGroup = "60-79";
            else ageGroup = "80-99";
            
            ageHierarchy.add(ageStr, halfDecadeRange, decadeRange, ageGroup,"*");
        }
        
        // 定義性別層級
        DefaultHierarchy genderHierarchy = Hierarchy.create();
        genderHierarchy.add("男","*");
        genderHierarchy.add("女","*");
        genderHierarchy.add("male","*");
        genderHierarchy.add("female","*");
        
        // 定義郵遞區號層級
        DefaultHierarchy zipHierarchy = Hierarchy.create();
        for (int zip = 100; zip <= 999; zip++) {
            String zipStr = String.format("%03d", zip);
            String hundredRange = String.format("%d00-%d99", zip/100, zip/100);
            zipHierarchy.add(zipStr, hundredRange,"*");
        }

        // 定義縣市層級
        DefaultHierarchy cityHierarchy = Hierarchy.create();
        // 北區
        cityHierarchy.add("台北市", "北區", "台灣", "*");
        cityHierarchy.add("新北市", "北區", "台灣", "*");
        cityHierarchy.add("桃園市", "北區", "台灣", "*");
        cityHierarchy.add("基隆市", "北區", "台灣", "*");
        cityHierarchy.add("新竹市", "北區", "台灣", "*");
        cityHierarchy.add("新竹縣", "北區", "台灣", "*");
        cityHierarchy.add("宜蘭縣", "北區", "台灣", "*");

        // 中區
        cityHierarchy.add("台中市", "中區", "台灣", "*");
        cityHierarchy.add("彰化縣", "中區", "台灣", "*");
        cityHierarchy.add("南投縣", "中區", "台灣", "*");
        cityHierarchy.add("苗栗縣", "中區", "台灣", "*");
        cityHierarchy.add("雲林縣", "中區", "台灣", "*");
        cityHierarchy.add("嘉義市", "中區", "台灣", "*");
        cityHierarchy.add("嘉義縣", "中區", "台灣", "*");

        // 南區
        cityHierarchy.add("高雄市", "南區", "台灣", "*");
        cityHierarchy.add("台南市", "南區", "台灣", "*");
        cityHierarchy.add("屏東縣", "南區", "台灣", "*");
        cityHierarchy.add("澎湖縣", "南區", "台灣", "*");

        // 東區
        cityHierarchy.add("花蓮縣", "東區", "台灣", "*");
        cityHierarchy.add("台東縣", "東區", "台灣", "*");

        // 離島
        cityHierarchy.add("金門縣", "離島", "台灣", "*");
        cityHierarchy.add("連江縣", "離島", "台灣", "*");

        // 定義是否確診層級
        DefaultHierarchy confirmHierarchy = Hierarchy.create();
        confirmHierarchy.add("0","*");
        confirmHierarchy.add("1","*");
        
        // 定義日期層級
        DefaultHierarchy dateHierarchy = Hierarchy.create();
        
        // 設定日期格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        Calendar cal = Calendar.getInstance();
        
        // 設定日期範圍（例如：2020-2025）
        int startYear = 2025;
        int endYear = 2025;
        
        // 為每一年建立層級
        for (int year = startYear; year <= endYear; year++) {
            for (int month = 1; month <= 12; month++) {
                // 取得當月的最後一天
                cal.set(year, month, 0);
                int lastDay = cal.get(Calendar.DAY_OF_MONTH);
                
                // 為當月的每一天建立層級
                for (int day = 1; day <= lastDay; day++) {
                    cal.set(year, month - 1, day);
                    String dateStr = sdf.format(cal.getTime());
                    
                    // 建立層級
                    String monthStr = String.format("%04d/%02d", year, month);
                    String quarterStr = String.format("%04d-Q%d", year, (month-1)/3 + 1);
                    String yearStr = String.format("%04d", year);
                    String decadeStr = String.format("%d0s", year/10);
                    
                    dateHierarchy.add(dateStr, monthStr, quarterStr, yearStr, decadeStr, "*");
                }
            }
        }

        // System.out.println("\n=== 欄位設定詳情 ===");
        // 根據欄位名稱設定屬性型態和層級
        for (String attribute : attributes) {
            //識別欄位:身分證/手機/姓名
            //準識別欄位:年齡/性別/縣市/通報日期/郵遞區號
            //敏感欄位:是否確診/疾病/檢驗結果
            //非敏感欄位:其他
            switch (attribute) {
                case "身分證":
                case "手機":
                case "姓名":
                    arxData.getDefinition().setAttributeType(attribute, AttributeType.IDENTIFYING_ATTRIBUTE);
                    arxData.getDefinition().setDataType(attribute, DataType.STRING);
                    // System.out.println("  - 類型：識別欄位");
                    // System.out.println("  - 資料型態：字串");
                    break;
                case "年齡":
                    arxData.getDefinition().setAttributeType(attribute, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
                    arxData.getDefinition().setDataType(attribute, DataType.INTEGER);
                    arxData.getDefinition().setAttributeType(attribute, ageHierarchy);
                    // System.out.println("  - 類型：準識別欄位");
                    // System.out.println("  - 資料型態：整數");
                    // System.out.println("  - 泛化層級：年齡層級（0-99歲，5年/10年/年齡組）");
                    break;
                case "性別":
                    arxData.getDefinition().setAttributeType(attribute, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
                    arxData.getDefinition().setDataType(attribute, DataType.STRING);
                    arxData.getDefinition().setAttributeType(attribute, genderHierarchy);
                    // System.out.println("  - 類型：準識別欄位");
                    // System.out.println("  - 資料型態：字串");
                    // System.out.println("  - 泛化層級：性別層級（男/女）");
                    break;
                case "縣市":
                    arxData.getDefinition().setAttributeType(attribute, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
                    arxData.getDefinition().setDataType(attribute, DataType.STRING);
                    arxData.getDefinition().setAttributeType(attribute, cityHierarchy);
                    // System.out.println("  - 類型：準識別欄位");
                    // System.out.println("  - 資料型態：字串");
                    // System.out.println("  - 泛化層級：縣市層級（縣市/區域/台灣）");
                    break;
                case "郵遞區號":
                    arxData.getDefinition().setAttributeType(attribute, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
                    arxData.getDefinition().setDataType(attribute, DataType.DECIMAL);
                    arxData.getDefinition().setAttributeType(attribute, zipHierarchy);
                    // System.out.println("  - 類型：準識別欄位");
                    // System.out.println("  - 資料型態：小數");
                    // System.out.println("  - 泛化層級：郵遞區號層級（3位數/百位數）");
                    break;
                case "通報日期":
                    arxData.getDefinition().setAttributeType(attribute, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
                    arxData.getDefinition().setDataType(attribute, DataType.DATE);
                    arxData.getDefinition().setAttributeType(attribute, dateHierarchy);
                    // System.out.println("  - 類型：準識別欄位");
                    // System.out.println("  - 資料型態：日期");
                    // System.out.println("  - 泛化層級：日期層級（日/月/季/年/十年）");
                    break;
                case "是否確診":
                    arxData.getDefinition().setAttributeType(attribute, AttributeType.SENSITIVE_ATTRIBUTE);
                    arxData.getDefinition().setDataType(attribute, DataType.INTEGER);
                    // System.out.println("  - 類型：敏感欄位");
                    // System.out.println("  - 資料型態：整數");
                    // System.out.println("  - 說明：確診狀態為敏感資訊");
                    break;
                case "疾病":
                    arxData.getDefinition().setAttributeType(attribute, AttributeType.SENSITIVE_ATTRIBUTE);
                    arxData.getDefinition().setDataType(attribute, DataType.STRING);
                    // System.out.println("  - 類型：敏感欄位");
                    // System.out.println("  - 資料型態：字串");
                    // System.out.println("  - 說明：疾病為敏感資訊");
                    break;
                case "檢驗結果":
                    arxData.getDefinition().setAttributeType(attribute, AttributeType.SENSITIVE_ATTRIBUTE);
                    arxData.getDefinition().setDataType(attribute, DataType.STRING);
                    // System.out.println("  - 類型：敏感欄位");
                    // System.out.println("  - 資料型態：字串");
                    // System.out.println("  - 說明：檢驗結果為敏感資訊");
                    break;
                default:
                    // 未定義泛化層級的欄位設為非敏感欄位
                    arxData.getDefinition().setAttributeType(attribute, AttributeType.INSENSITIVE_ATTRIBUTE);
                    arxData.getDefinition().setDataType(attribute, DataType.STRING);
                    // System.out.println("  - 類型：非敏感欄位（未定義泛化層級）");
                    // System.out.println("  - 資料型態：字串");
                    // System.out.println("  - 警告：此欄位未定義泛化層級，已設為非敏感欄位");
                    break;
            }
        }
        // System.out.println("\n=== 欄位設定完成 ===");
    }

    public Map<String, Object> applyDifferentialPrivacy(String data, double epsilon, double delta, boolean isDataIndependent) {
        try {
            // 解析輸入數據
            List<Map<String, String>> inputData = objectMapper.readValue(data, List.class);
            
            // 創建 ARX 數據集
            Data.DefaultData arxData = Data.create();
            
            // 添加屬性
            if (!inputData.isEmpty()) {
                Map<String, String> firstRow = inputData.get(0);
                String[] attributes = firstRow.keySet().toArray(new String[0]);
                arxData.add(attributes);
                
                // 設定泛化層級
                setupHierarchies(arxData, attributes);
            }
            
            // 添加數據
            for (Map<String, String> row : inputData) {
                String[] values = new String[row.size()];
                int i = 0;
                for (String value : row.values()) {
                    values[i++] = value;
                }
                arxData.add(values);
            }
            
            // 配置差分隱私
            ARXConfiguration config = ARXConfiguration.create();
            config.setSuppressionLimit(0.1d);
            config.setHeuristicSearchTimeLimit(60); // 設定 60 秒超時
            
            // 為直接標記為敏感屬性的欄位添加隱私模型
            for (String attribute : arxData.getDefinition().getSensitiveAttributes()) {
                //比較寬鬆的設定,適用於測試與開發
                config.addPrivacyModel(new DistinctLDiversity(attribute, 2));

                //照理說第二參數應該是在2~10之間,但是可以設定1/1.4這樣
                //config.addPrivacyModel(new EntropyLDiversity(attribute, 1.4));
            }
            
            if (isDataIndependent) {
                // 資料獨立差分隱私
                DataGeneralizationScheme scheme = DataGeneralizationScheme.create(arxData, GeneralizationDegree.MEDIUM);
                config.addPrivacyModel(new EDDifferentialPrivacy(epsilon, delta, scheme));
            } else {
                // 資料相依差分隱私
                config.addPrivacyModel(new EDDifferentialPrivacy(epsilon, delta));
                config.setDPSearchBudget(0.1d);  // double dpSearchBudget = Additional epsilon for search process
                config.setHeuristicSearchStepLimit(160);
            }
            
            // 執行匿名化
            ARXAnonymizer anonymizer = new ARXAnonymizer();
            ARXResult result = anonymizer.anonymize(arxData, config);
            
            // 處理結果
            List<List<String>> anonymizedResult = new ArrayList<>();
            DataHandle handle = result.getOutput();
            for (int i = 0; i < handle.getNumRows(); i++) {
                List<String> row = new ArrayList<>();
                for (int j = 0; j < handle.getNumColumns(); j++) {
                    row.add(handle.getValue(i, j));
                }
                anonymizedResult.add(row);
            }
            
            return Map.of(
                "epsilon", epsilon,
                "delta", delta,
                "isDataIndependent", isDataIndependent,
                "result", anonymizedResult
            );
            
        } catch (Exception e) {
            // 只輸出錯誤訊息，不包含資料內容
            throw new RuntimeException(String.format("差分隱私處理失敗 (epsilon=%.2f, delta=%.5f): %s", 
                epsilon, delta, e.getMessage()));
        }
    }
} 