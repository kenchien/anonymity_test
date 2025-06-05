package com.anonymity;

import com.anonymity.util.KTestDataGenerator;
import org.deidentifier.arx.*;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.AttributeType.Hierarchy.DefaultHierarchy;
import org.deidentifier.arx.AttributeType.MicroAggregationFunction;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.criteria.EntropyLDiversity;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class KTest {

    @Test
    public void testKAnonymity() {
        try {
            // 匿名化參數設定
            final int K_ANONYMITY = 3;                    // K-Anonymity 的 k 值
            final double L_DIVERSITY = 1.5;               // Entropy L-Diversity 的 l 值
            final double SUPPRESSION_LIMIT = 0.5;         // 抑制限制
            final int TEST_DATA_SIZE = 3000;              // 測試資料筆數
            final int DISPLAY_SAMPLE_SIZE = 30;           // 顯示範例筆數
            
            // 敏感屬性列表
            final String[] SENSITIVE_ATTRIBUTES = {
                "疾病",
                "檢驗結果",
                "是否確診"
            };
            
            // 準識別屬性列表
            final String[] QUASI_IDENTIFYING_ATTRIBUTES = {
                "年齡",
                "性別",
                "郵遞區號",
                "縣市",
                "通報日期"
            };

            // 產生測試資料
            List<Map<String, String>> data = KTestDataGenerator.generateTestData(TEST_DATA_SIZE);
            System.out.println("產生測試資料筆數: " + data.size());

            // 顯示前 N 筆測試資料
            System.out.println("\n前 " + DISPLAY_SAMPLE_SIZE + " 筆測試資料:");
            System.out.println("年齡\t性別\t郵遞區號\t縣市\t通報日期\t疾病\t檢驗結果\t是否確診");
            System.out.println("--------------------------------------------------------------------------------------------------------");
            for (int i = 0; i < Math.min(DISPLAY_SAMPLE_SIZE, data.size()); i++) {
                Map<String, String> row = data.get(i);
                System.out.printf("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s%n",
                    row.get("年齡"),
                    row.get("性別"),
                    row.get("郵遞區號"),
                    row.get("縣市"),
                    row.get("通報日期"),
                    row.get("疾病"),
                    row.get("檢驗結果"),
                    row.get("是否確診")
                );
            }
            System.out.println("--------------------------------------------------------------------------------------------------------\n");

            // 建立 ARX Data 物件
            Data.DefaultData dataset = Data.create();
            dataset.add("年齡", "性別", "郵遞區號", "縣市", "通報日期", "疾病", "檢驗結果", "是否確診");
            for (Map<String, String> row : data) {
                dataset.add(
                    row.get("年齡"),
                    row.get("性別"),
                    row.get("郵遞區號"),
                    row.get("縣市"),
                    row.get("通報日期"),
                    row.get("疾病"),
                    row.get("檢驗結果"),
                    row.get("是否確診")
                );
            }
            System.out.println("資料集建立完成，總筆數: " + dataset.getHandle().getNumRows());

            // 定義年齡層級
            DefaultHierarchy ageHierarchy = Hierarchy.create();
            
            // 設定年齡範圍（0-99歲）
            for (int age = 0; age <= 99; age++) {
                // 計算年齡區間
                int decade = (age / 10) * 10;
                int halfDecade = (age / 5) * 5;
                
                // 建立層級
                String ageStr = String.valueOf(age);
                String decadeRange = String.format("%d-%d", decade, decade + 9);
                String halfDecadeRange = String.format("%d-%d", halfDecade, halfDecade + 4);
                String ageGroup;
                
                // 根據年齡分組
                if (age < 20) {
                    ageGroup = "0-19";
                } else if (age < 40) {
                    ageGroup = "20-39";
                } else if (age < 60) {
                    ageGroup = "40-59";
                } else if (age < 80) {
                    ageGroup = "60-79";
                } else {
                    ageGroup = "80-99";
                }
                
                ageHierarchy.add(ageStr, halfDecadeRange, decadeRange, ageGroup, "*");
            }

            // 定義性別層級
            DefaultHierarchy genderHierarchy = Hierarchy.create();
            genderHierarchy.add("男", "*");
            genderHierarchy.add("女", "*");

            // 定義郵遞區號層級
            DefaultHierarchy zipHierarchy = Hierarchy.create();
            
            // 定義各縣市的郵遞區號範圍
            Map<String, int[]> cityZipRanges = new HashMap<>();
            cityZipRanges.put("台北市", new int[]{100, 116});
            cityZipRanges.put("新北市", new int[]{200, 251});
            cityZipRanges.put("桃園市", new int[]{320, 338});
            cityZipRanges.put("台中市", new int[]{400, 439});
            cityZipRanges.put("台南市", new int[]{700, 745});
            cityZipRanges.put("高雄市", new int[]{800, 852});
            
            // 為每個縣市建立郵遞區號層級
            for (Map.Entry<String, int[]> entry : cityZipRanges.entrySet()) {
                String city = entry.getKey();
                int startZip = entry.getValue()[0];
                int endZip = entry.getValue()[1];
                
                for (int zip = startZip; zip <= endZip; zip++) {
                    String zipStr = String.format("%03d", zip);
                    String hundredRange = String.format("%d00-%d99", zip/100, zip/100);
                    String cityRange = String.format("%s區", city);
                    
                    zipHierarchy.add(zipStr, hundredRange, cityRange, "*");
                }
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

            // 定義日期層級
            DefaultHierarchy dateHierarchy = Hierarchy.create();
            
            // 設定日期格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            Calendar cal = Calendar.getInstance();
            
            // 設定日期範圍（例如：2020-2025）
            int startYear = 2020;
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

            // 設定日期層級
            dataset.getDefinition().setAttributeType("通報日期", dateHierarchy);

            // 設定資料型態
            dataset.getDefinition().setDataType("年齡", DataType.INTEGER);
            dataset.getDefinition().setDataType("郵遞區號", DataType.DECIMAL);
            dataset.getDefinition().setDataType("通報日期", DataType.DATE);

            // 設定屬性型態
            dataset.getDefinition().setAttributeType("年齡", ageHierarchy);
            dataset.getDefinition().setAttributeType("性別", genderHierarchy);
            dataset.getDefinition().setAttributeType("郵遞區號", zipHierarchy);
            dataset.getDefinition().setAttributeType("縣市", cityHierarchy);
            dataset.getDefinition().setAttributeType("通報日期", dateHierarchy);
            dataset.getDefinition().setAttributeType("疾病", AttributeType.SENSITIVE_ATTRIBUTE);
            dataset.getDefinition().setAttributeType("檢驗結果", AttributeType.SENSITIVE_ATTRIBUTE);
            dataset.getDefinition().setAttributeType("是否確診", AttributeType.SENSITIVE_ATTRIBUTE);
            System.out.println("屬性型態設定完成");

            // 匿名化設定
            ARXConfiguration config = ARXConfiguration.create();
            config.addPrivacyModel(new KAnonymity(K_ANONYMITY));
            for (String attr : SENSITIVE_ATTRIBUTES) {
                config.addPrivacyModel(new EntropyLDiversity(attr, L_DIVERSITY));
            }
            config.setSuppressionLimit(SUPPRESSION_LIMIT);
            config.setQualityModel(Metric.createLossMetric());

            System.out.println("\n匿名化參數設定：");
            System.out.println("1. K-Anonymity: k = " + K_ANONYMITY);
            System.out.println("2. Entropy L-Diversity: l = " + L_DIVERSITY);
            System.out.println("3. 抑制限制: " + (SUPPRESSION_LIMIT * 100) + "%");
            System.out.println("4. 品質度量: 損失度量");
            System.out.println("5. 敏感屬性: " + String.join("、", SENSITIVE_ATTRIBUTES));
            System.out.println("6. 準識別屬性: " + String.join("、", QUASI_IDENTIFYING_ATTRIBUTES));

            // 執行匿名化
            ARXAnonymizer anonymizer = new ARXAnonymizer();
            ARXResult result = anonymizer.anonymize(dataset, config);
            System.out.println("\n匿名化執行完成");

            if (result == null) {
                System.err.println("\n匿名化結果為空，可能原因：");
                System.err.println("1. 資料集太小，無法滿足 k=" + K_ANONYMITY + " 的要求");
                System.err.println("2. 敏感屬性的多樣性不足，無法滿足 l=" + L_DIVERSITY + " 的要求");
                System.err.println("3. 抑制限制（" + (SUPPRESSION_LIMIT * 100) + "%）過低，導致過多資料被抑制");
                System.err.println("\n建議調整：");
                System.err.println("1. 降低 k 值（例如：k=" + (K_ANONYMITY - 1) + "）");
                System.err.println("2. 降低 l 值（例如：l=" + (L_DIVERSITY - 0.3) + "）");
                System.err.println("3. 提高抑制限制（例如：" + ((SUPPRESSION_LIMIT + 0.2) * 100) + "%）");
                System.err.println("4. 增加資料量（目前：" + TEST_DATA_SIZE + " 筆）");
                System.err.println("5. 調整泛化層級");
                return;
            }

            DataHandle output = result.getOutput();
            if (output == null) {
                System.err.println("\n輸出數據為空，可能原因：");
                System.err.println("1. 所有資料都被抑制");
                System.err.println("2. 資料集結構不符合要求");
                System.err.println("\n建議調整：");
                System.err.println("1. 提高抑制限制");
                System.err.println("2. 檢查資料集結構");
                System.err.println("3. 調整泛化層級");
                return;
            }

            System.out.println("\n匿名化結果統計：");
            System.out.println("1. 原始資料筆數: " + dataset.getHandle().getNumRows());
            System.out.println("2. 匿名化後資料筆數: " + output.getNumRows());
            System.out.println("3. 資料欄位數: " + output.getNumColumns());
            
            // 計算抑制率（被刪除的資料比例）
            double suppressionRate = (double)(dataset.getHandle().getNumRows() - output.getNumRows()) / dataset.getHandle().getNumRows();
            System.out.println("4. 抑制率: " + String.format("%.2f%%", suppressionRate * 100));
            
            // 計算資料損失率（基於泛化層級）
            int totalGeneralizations = 0;
            int maxPossibleGeneralizations = 0;
            
            // 計算每個準識別屬性的泛化程度
            for (String attr : QUASI_IDENTIFYING_ATTRIBUTES) {
                int originalLevel = 0;  // 原始層級
                int currentLevel = 0;   // 當前層級
                
                // 計算當前層級（這裡需要根據實際的泛化結果來計算）
                // 注意：這只是一個示例，實際的計算方式需要根據 ARX 的泛化結果來調整
                if (attr.equals("年齡")) {
                    currentLevel = 1;  // 假設年齡被泛化到五年區間
                } else if (attr.equals("郵遞區號")) {
                    currentLevel = 1;  // 假設郵遞區號被泛化到百位數區間
                }
                
                totalGeneralizations += currentLevel;
                maxPossibleGeneralizations += 3;  // 假設每個屬性最多可以泛化到 3 層
            }
            
            double informationLoss = (double)totalGeneralizations / maxPossibleGeneralizations;
            System.out.println("5. 資料損失率: " + String.format("%.2f%%", informationLoss * 100));
            
            // 輸出前 N 筆資料作為範例
            System.out.println("\n匿名化後的資料範例（前 " + DISPLAY_SAMPLE_SIZE + " 筆）:");
            System.out.println("年齡\t性別\t郵遞區號\t縣市\t通報日期\t疾病\t檢驗結果\t是否確診");
            System.out.println("--------------------------------------------------------------------------------------------------------");
            for (int i = 0; i < Math.min(DISPLAY_SAMPLE_SIZE, output.getNumRows()); i++) {
                for (int j = 0; j < output.getNumColumns(); j++) {
                    System.out.print(output.getValue(i, j) + "\t");
                }
                System.out.println();
            }
            System.out.println("--------------------------------------------------------------------------------------------------------\n");

        } catch (Exception e) {
            System.err.println("\n測試執行時發生錯誤: " + e.getMessage());
            System.err.println("\n錯誤詳情：");
            e.printStackTrace();
            System.err.println("\n建議檢查：");
            System.err.println("1. 資料格式是否正確");
            System.err.println("2. 層級結構是否完整");
            System.err.println("3. 參數設定是否合理");
            System.err.println("4. 記憶體使用量是否足夠");
        }
    }
} 