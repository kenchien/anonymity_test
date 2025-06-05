package com.anonymity.service;

import com.anonymity.validation.DataValidator;
import org.deidentifier.arx.*;
import org.deidentifier.arx.Data.DefaultData;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.EntropyLDiversity;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.AttributeType.Hierarchy.DefaultHierarchy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

@Service
public class AnonymityService {
    
    private static final Logger logger;
    
    static {
        // 設定 logger 的編碼為 UTF-8
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
        
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.setCharset(java.nio.charset.StandardCharsets.UTF_8);
        encoder.start();
        
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();
        
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("ROOT");
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(consoleAppender);
        
        logger = LoggerFactory.getLogger(AnonymityService.class);
    }
    
    @Autowired
    private DataValidator dataValidator;
    
    // 縣市列表
    private static final String[] CITIES = {
        "台北市", "新北市", "桃園市", "台中市", "台南市", "高雄市",
        "基隆市", "新竹市", "新竹縣", "苗栗縣", "彰化縣", "南投縣",
        "雲林縣", "嘉義市", "嘉義縣", "屏東縣", "宜蘭縣", "花蓮縣",
        "台東縣", "澎湖縣", "金門縣", "連江縣"
    };

    // 將 cityZipRanges 提升為類別層級變數
    private static final Map<String, int[]> cityZipRanges = new HashMap<>();
    static {
        /*
        cityZipRanges.put("台北市", new int[]{100, 116});
        cityZipRanges.put("新北市", new int[]{200, 251});
        cityZipRanges.put("桃園市", new int[]{320, 338});
        cityZipRanges.put("台中市", new int[]{400, 439});
        cityZipRanges.put("台南市", new int[]{700, 745});
        cityZipRanges.put("高雄市", new int[]{800, 852});
        cityZipRanges.put("基隆市", new int[]{200, 206});
        cityZipRanges.put("新竹市", new int[]{300, 308});
        cityZipRanges.put("新竹縣", new int[]{300, 315});
        cityZipRanges.put("苗栗縣", new int[]{350, 369});
        cityZipRanges.put("彰化縣", new int[]{500, 530});
        cityZipRanges.put("南投縣", new int[]{540, 558});
        cityZipRanges.put("雲林縣", new int[]{630, 655});
        cityZipRanges.put("嘉義市", new int[]{600, 600});
        cityZipRanges.put("嘉義縣", new int[]{600, 625});
        cityZipRanges.put("屏東縣", new int[]{900, 947});
        cityZipRanges.put("宜蘭縣", new int[]{260, 272});
        cityZipRanges.put("花蓮縣", new int[]{970, 983});
        cityZipRanges.put("台東縣", new int[]{950, 966});
        cityZipRanges.put("澎湖縣", new int[]{880, 885});
        cityZipRanges.put("金門縣", new int[]{890, 891});
        cityZipRanges.put("連江縣", new int[]{209, 212});
        */
    }
    
    public Map<String, Object> anonymizeData(List<Map<String, String>> data, 
                            List<String> quasiIdentifiers,
                            List<String> sensitiveAttributes,
                            int k,
                            double l) throws IOException {
        
        try {
            logger.info("start anonymizeData, count: {}, k: {}, l: {}", data.size(), k, l);
            
            // 驗證數據
            dataValidator.validateRequest(data, quasiIdentifiers, sensitiveAttributes, k, l);
            
            // 記錄前三筆資料
            logger.info("First 3 rows of data:");
            for (int i = 0; i < Math.min(3, data.size()); i++) {
                logger.info("Row {}: {}", i + 1, data.get(i));
            }
            
            // 創建數據集
            DefaultData dataSet = Data.create();
            
            // 添加屬性
            if (!data.isEmpty()) {
                Map<String, String> firstRow = data.get(0);
                String[] attributes = firstRow.keySet().toArray(new String[0]);
                dataSet.add(attributes);
                logger.debug("Added attributes: {}", Arrays.toString(attributes));
            }
            
            // 添加數據
            for (Map<String, String> row : data) {
                String[] values = new String[row.size()];
                int i = 0;
                for (String value : row.values()) {
                    values[i++] = value;
                }
                dataSet.add(values);
            }
            logger.debug("Data addition completed, total rows: {}", data.size());
            
            // 定義年齡層級
            DefaultHierarchy ageHierarchy = Hierarchy.create();
            for (int age = 0; age <= 99; age++) {
                int decade = (age / 10) * 10;
                int halfDecade = (age / 5) * 5;
                
                String ageStr = String.valueOf(age);
                String decadeRange = String.format("%d-%d", decade, decade + 9);
                String halfDecadeRange = String.format("%d-%d", halfDecade, halfDecade + 4);
                String ageGroup;
                
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
            /*
            DefaultHierarchy zipHierarchy = Hierarchy.create();
            
            // 為每個縣市建立郵遞區號層級
            for (String city : CITIES) {
                int[] zipRange = cityZipRanges.get(city);
                if (zipRange != null) {
                    for (int zip = zipRange[0]; zip <= zipRange[1]; zip++) {
                        String zipStr = String.format("%03d", zip);
                        // 保持郵遞區號為數字格式
                        String hundredRange = String.format("%d00-%d99", zip/100, zip/100);
                        zipHierarchy.add(zipStr, hundredRange, "*");
                    }
                }
            }
            */

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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            Calendar cal = Calendar.getInstance();
            
            int startYear = 2020;
            int endYear = 2025;
            
            for (int year = startYear; year <= endYear; year++) {
                for (int month = 1; month <= 12; month++) {
                    cal.set(year, month, 0);
                    int lastDay = cal.get(Calendar.DAY_OF_MONTH);
                    
                    for (int day = 1; day <= lastDay; day++) {
                        cal.set(year, month - 1, day);
                        String dateStr = sdf.format(cal.getTime());
                        
                        String monthStr = String.format("%04d/%02d", year, month);
                        String quarterStr = String.format("%04d-Q%d", year, (month-1)/3 + 1);
                        String yearStr = String.format("%04d", year);
                        String decadeStr = String.format("%d0s", year/10);
                        
                        dateHierarchy.add(dateStr, monthStr, quarterStr, yearStr, decadeStr, "*");
                    }
                }
            }

            // 設定資料型態
            dataSet.getDefinition().setDataType("年齡", DataType.INTEGER);
            //dataSet.getDefinition().setDataType("郵遞區號", DataType.DECIMAL);
            dataSet.getDefinition().setDataType("通報日期", DataType.DATE);

            // 設定屬性型態
            dataSet.getDefinition().setAttributeType("年齡", ageHierarchy);
            dataSet.getDefinition().setAttributeType("性別", genderHierarchy);
            //dataSet.getDefinition().setAttributeType("郵遞區號", zipHierarchy);
            dataSet.getDefinition().setAttributeType("縣市", cityHierarchy);
            dataSet.getDefinition().setAttributeType("通報日期", dateHierarchy);
            
            // 設置敏感屬性
            for (String attribute : sensitiveAttributes) {
                dataSet.getDefinition().setAttributeType(attribute, AttributeType.SENSITIVE_ATTRIBUTE);
                logger.debug("Set sensitive attribute: {}", attribute);
            }
            
            // 創建ARX配置
            ARXConfiguration config = ARXConfiguration.create();
            config.addPrivacyModel(new KAnonymity(k));
            
            // 為每個敏感屬性添加 l-Diversity 隱私模型
            for (String sensitiveAttribute : sensitiveAttributes) {
                config.addPrivacyModel(new EntropyLDiversity(sensitiveAttribute, l));
            }
            
            // 設置隱私模型參數
            config.setSuppressionLimit(1.0d);
            config.setQualityModel(Metric.createLossMetric(0.5d));
            
            // 設定日期泛化層級
            config.setMaxOutliers(0.0d);
            
            logger.info("ARX configuration completed: k={}, l={}, suppression limit={}", k, l, config.getSuppressionLimit());
            
            // 執行匿名化
            ARXAnonymizer anonymizer = new ARXAnonymizer();
            ARXResult result = anonymizer.anonymize(dataSet, config);
            
            // 檢查結果
            if (result == null || result.getOutput() == null) {
                logger.error("Anonymization failed: Unable to find solution satisfying k={}, l={}", k, l);
                throw new IOException("Unable to find anonymization solution satisfying k=" + k + " and l=" + l + ", please try adjusting parameters or increasing data volume");
            }
            
            // 轉換結果為易於理解的格式
            DataHandle output = result.getOutput();
            List<Map<String, String>> anonymizedData = new ArrayList<>();
            
            // 獲取所有欄位名稱
            String[] header = dataSet.getDefinition().getQuasiIdentifyingAttributes().toArray(new String[0]);
            String[] sensitiveHeader = dataSet.getDefinition().getSensitiveAttributes().toArray(new String[0]);
            
            for (int i = 0; i < output.getNumRows(); i++) {
                Map<String, String> row = new HashMap<>();
                // 添加準識別符
                for (int j = 0; j < header.length; j++) {
                    row.put(header[j], output.getValue(i, j));
                }
                // 添加敏感屬性
                for (int j = 0; j < sensitiveHeader.length; j++) {
                    row.put(sensitiveHeader[j], output.getValue(i, header.length + j));
                }
                anonymizedData.add(row);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", anonymizedData);
            response.put("statistics", new HashMap<String, Object>() {{
                put("k", k);
                put("l", l);
                put("rows", output.getNumRows());
                put("columns", header.length);
                put("informationLoss", calculateInformationLoss(output, header));
            }});
            
            logger.info("Anonymization completed, total rows: {}", output.getNumRows());
            
            return response;
            
        } catch (IllegalArgumentException e) {
            logger.error("Data validation failed", e);
            throw e;
        } catch (Exception e) {
            logger.error("Anonymization process failed", e);
            throw new IOException("Anonymization process failed: " + e.getMessage(), e);
        }
    }

    private double calculateInformationLoss(DataHandle output, String[] header) {
        // 計算資料損失率（基於泛化層級）
        int totalGeneralizations = 0;
        int maxPossibleGeneralizations = 0;
        
        // 計算每個準識別屬性的泛化程度
        for (String attr : header) {
            int currentLevel = 0;
            
            // 根據屬性名稱判斷泛化層級
            if (attr.equals("年齡")) {
                currentLevel = 1;  // 假設年齡被泛化到五年區間
            } else if (attr.equals("郵遞區號")) {
                currentLevel = 1;  // 假設郵遞區號被泛化到百位數區間
            } else if (attr.equals("通報日期")) {
                currentLevel = 1;  // 將日期泛化層級設為 1，表示保留到月份層級
            }
            
            totalGeneralizations += currentLevel;
            maxPossibleGeneralizations += 3;  // 假設每個屬性最多可以泛化到 3 層
        }
        
        return (double)totalGeneralizations / maxPossibleGeneralizations;
    }

    public Map<String, Object> generateAndAnonymizeTestData(int dataSize, int k, double l) throws IOException {
        // 生成測試資料
        List<Map<String, String>> testData = generateTestData(dataSize);
        
        // 定義準識別符和敏感屬性
        List<String> quasiIdentifiers = Arrays.asList("年齡", "性別", "縣市", "通報日期");
        List<String> sensitiveAttributes = Arrays.asList("疾病", "檢驗結果", "是否確診");
        
        logger.info("Starting test data generation and anonymization, data size: {}, k: {}, l: {}", dataSize, k, l);
        
        // 進行匿名化處理
        Map<String, Object> result = anonymizeData(testData, quasiIdentifiers, sensitiveAttributes, k, l);
        
        return result;
    }
    
    private List<Map<String, String>> generateTestData(int size) {
        Random random = new Random();
        String[] genders = {"男", "女"};
        String[] diseases = {"流感", "糖尿病", "高血壓", "氣喘", "癌症"};
        String[] testResults = {"陽性", "陰性", "待確認"};
        String[] diagnosisStatus = {"確診", "未確診", "待確認"};
        
        return IntStream.range(0, size)
            .mapToObj(i -> {
                Map<String, String> record = new HashMap<>();
                
                // 年齡 (20-79歲)
                record.put("年齡", String.valueOf(20 + random.nextInt(60)));
                
                // 性別
                record.put("性別", genders[random.nextInt(genders.length)]);
                
                // 縣市和郵遞區號
                String city = CITIES[random.nextInt(CITIES.length)];
                record.put("縣市", city);
                
                /*
                // 根據縣市生成對應的郵遞區號
                int[] zipRange = cityZipRanges.get(city);
                if (zipRange != null) {
                    int zipCode = zipRange[0] + random.nextInt(zipRange[1] - zipRange[0] + 1);
                    record.put("郵遞區號", String.format("%03d", zipCode));
                } else {
                    // 若查不到範圍，使用預設 100~115
                    int zipCode = 100 + random.nextInt(16);
                    record.put("郵遞區號", String.format("%03d", zipCode));
                }
                */
                
                // 通報日期 (2024年)
                int year = 2024;
                int month = 1 + random.nextInt(12);
                int day = 1 + random.nextInt(28); // 簡化處理，統一使用28天
                record.put("通報日期", String.format("%04d/%02d/%02d", year, month, day));
                
                // 疾病
                record.put("疾病", diseases[random.nextInt(diseases.length)]);
                
                // 檢驗結果
                record.put("檢驗結果", testResults[random.nextInt(testResults.length)]);
                
                // 是否確診
                record.put("是否確診", diagnosisStatus[random.nextInt(diagnosisStatus.length)]);
                
                return record;
            })
            .collect(Collectors.toList());
    }
} 