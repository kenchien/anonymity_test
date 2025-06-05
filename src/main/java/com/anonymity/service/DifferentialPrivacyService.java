package com.anonymity.service;

import com.anonymity.controller.dto.DifferentialPrivacyRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.deidentifier.arx.*;
import org.deidentifier.arx.criteria.EDDifferentialPrivacy;
import org.deidentifier.arx.criteria.EntropyLDiversity;
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

            // 設定屬性型態和層級
            //年齡/性別/郵遞區號/縣市/通報日期/疾病/檢驗結果/是否確診
            arxData.getDefinition().setAttributeType("年齡", AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
            arxData.getDefinition().setAttributeType("性別", AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
            //arxData.getDefinition().setAttributeType("縣市", AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
            //arxData.getDefinition().setAttributeType("郵遞區號", AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
            arxData.getDefinition().setAttributeType("是否確診", AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
            arxData.getDefinition().setAttributeType("通報日期", AttributeType.INSENSITIVE_ATTRIBUTE);
            
            arxData.getDefinition().setDataType("年齡", DataType.INTEGER);
            arxData.getDefinition().setDataType("性別", DataType.STRING);
            //arxData.getDefinition().setDataType("縣市", DataType.STRING);
            //arxData.getDefinition().setDataType("郵遞區號", DataType.DECIMAL);              
            arxData.getDefinition().setDataType("是否確診", DataType.INTEGER);
            arxData.getDefinition().setDataType("通報日期", DataType.DATE);
            
            arxData.getDefinition().setAttributeType("年齡", ageHierarchy);
            arxData.getDefinition().setAttributeType("性別", genderHierarchy);
            //arxData.getDefinition().setAttributeType("縣市", cityHierarchy);
            //arxData.getDefinition().setAttributeType("郵遞區號", zipHierarchy);
            arxData.getDefinition().setAttributeType("是否確診", confirmHierarchy);
            arxData.getDefinition().setAttributeType("通報日期", dateHierarchy);

            
            // 配置差分隱私
            ARXConfiguration config = ARXConfiguration.create();
            config.setSuppressionLimit(0.2d);
            config.setHeuristicSearchTimeLimit(60); // 設定 60 秒超時
            
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