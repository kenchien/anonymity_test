package com.anonymity.util;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

/**
 * 隱私指標計算器
 * 用於評估資料匿名化後的各種指標，包括：
 * 1. 資訊損失：評估資料匿名化後資訊的保留程度
 * 2. 效用損失：評估資料匿名化後對統計分析的影響
 * 3. 隱私保障：評估資料匿名化後的隱私保護程度
 * 4. 平均值差異：評估數值型資料的統計特性保持程度
 * 5. 分類準確率：評估分類型資料的分布保持程度
 */
public class PrivacyMetricsCalculator {
    
    /**
     * 計算所有評估指標
     * @param originalData 原始資料，格式為 List<Map<String, String>>
     * @param anonymizedData 匿名化後的資料，格式為 List<List<String>>
     * @return 包含所有評估指標的 Map
     */
    public static Map<String, Double> calculateMetrics(List<Map<String, String>> originalData, 
                                                     List<List<String>> anonymizedData) {
        // 計算信息損失：評估資料匿名化後資訊的保留程度
        double informationLoss = calculateInformationLoss(originalData, anonymizedData);
        
        // 計算效用損失：評估資料匿名化後對統計分析的影響
        double utilityLoss = calculateUtilityLoss(originalData, anonymizedData);
        
        // 計算隱私保障：評估資料匿名化後的隱私保護程度
        double privacyGuarantee = calculatePrivacyGuarantee(originalData, anonymizedData);
        
        // 計算平均值差異：評估數值型資料的統計特性保持程度
        double meanDifference = calculateMeanDifference(originalData, anonymizedData);
        
        // 計算分類準確率：評估分類型資料的分布保持程度
        double classificationAccuracy = calculateClassificationAccuracy(originalData, anonymizedData);
        
        return Map.of(
            "informationLoss", informationLoss,
            "utilityLoss", utilityLoss,
            "privacyGuarantee", privacyGuarantee,
            "meanDifference", meanDifference,
            "classificationAccuracy", classificationAccuracy
        );
    }
    
    /**
     * 計算資訊損失
     * 基於每個欄位的唯一值數量變化來評估資訊損失
     * 值域範圍：0（無損失）到 1（完全損失）
     */
    private static double calculateInformationLoss(List<Map<String, String>> originalData, 
                                                 List<List<String>> anonymizedData) {
        if (originalData.isEmpty() || anonymizedData.isEmpty()) {
            return 1.0;
        }
        
        // 計算每個欄位的資訊損失
        Map<String, Double> columnLosses = new HashMap<>();
        Set<String> columns = originalData.get(0).keySet();
        
        for (String column : columns) {
            // 計算原始資料中該欄位的唯一值數量
            Set<String> originalUniqueValues = new HashSet<>();
            for (Map<String, String> row : originalData) {
                originalUniqueValues.add(row.get(column));
            }
            
            // 計算匿名化後該欄位的唯一值數量
            Set<String> anonymizedUniqueValues = new HashSet<>();
            int columnIndex = new ArrayList<>(columns).indexOf(column);
            for (List<String> row : anonymizedData) {
                anonymizedUniqueValues.add(row.get(columnIndex));
            }
            
            // 計算該欄位的資訊損失：1 - (匿名化後唯一值數量 / 原始唯一值數量)
            double originalCount = originalUniqueValues.size();
            double anonymizedCount = anonymizedUniqueValues.size();
            double loss = 1.0 - (anonymizedCount / originalCount);
            columnLosses.put(column, loss);
        }
        
        // 返回所有欄位的平均資訊損失
        return columnLosses.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(1.0);
    }
    
    /**
     * 計算效用損失
     * 基於每個欄位的統計特性變化來評估效用損失
     * 值域範圍：0（無損失）到 1（完全損失）
     */
    private static double calculateUtilityLoss(List<Map<String, String>> originalData, 
                                             List<List<String>> anonymizedData) {
        if (originalData.isEmpty() || anonymizedData.isEmpty()) {
            return 1.0;
        }
        
        // 計算每個欄位的統計特性
        Map<String, Map<String, Double>> originalStats = calculateMapColumnStatistics(originalData);
        Map<String, Map<String, Double>> anonymizedStats = calculateListColumnStatistics(anonymizedData);
        
        // 計算每個欄位的效用損失
        double totalLoss = 0.0;
        int columnCount = 0;
        
        // 獲取原始資料的欄位名稱列表
        List<String> columns = new ArrayList<>(originalData.get(0).keySet());
        
        for (int i = 0; i < columns.size(); i++) {
            String originalColumn = columns.get(i);
            String anonymizedColumn = "column" + i;
            
            Map<String, Double> originalColumnStats = originalStats.get(originalColumn);
            Map<String, Double> anonymizedColumnStats = anonymizedStats.get(anonymizedColumn);
            
            if (originalColumnStats != null && anonymizedColumnStats != null) {
                // 計算該欄位的統計特性差異
                double columnLoss = 0.0;
                for (String stat : originalColumnStats.keySet()) {
                    double originalValue = originalColumnStats.get(stat);
                    double anonymizedValue = anonymizedColumnStats.get(stat);
                    
                    // 避免除以零
                    if (originalValue != 0) {
                        columnLoss += Math.abs((anonymizedValue - originalValue) / originalValue);
                    }
                }
                
                totalLoss += columnLoss / originalColumnStats.size();
                columnCount++;
            }
        }
        
        return columnCount > 0 ? totalLoss / columnCount : 1.0;
    }
    
    /**
     * 計算隱私保障
     * 基於資訊熵來評估隱私保護程度
     * 值域範圍：0（無保護）到 1（完全保護）
     */
    private static double calculatePrivacyGuarantee(List<Map<String, String>> originalData, 
                                                  List<List<String>> anonymizedData) {
        if (originalData.isEmpty() || anonymizedData.isEmpty()) {
            return 0.0;
        }
        
        // 計算每個欄位的隱私保障
        Map<String, Double> columnPrivacy = new HashMap<>();
        Set<String> columns = originalData.get(0).keySet();
        
        for (String column : columns) {
            // 計算原始資料中該欄位的值分布
            Map<String, Integer> originalDistribution = new HashMap<>();
            for (Map<String, String> row : originalData) {
                String value = row.get(column);
                originalDistribution.merge(value, 1, Integer::sum);
            }
            
            // 計算匿名化後該欄位的值分布
            Map<String, Integer> anonymizedDistribution = new HashMap<>();
            int columnIndex = new ArrayList<>(columns).indexOf(column);
            for (List<String> row : anonymizedData) {
                String value = row.get(columnIndex);
                anonymizedDistribution.merge(value, 1, Integer::sum);
            }
            
            // 計算該欄位的隱私保障：匿名化後熵值 / 原始熵值
            double originalEntropy = calculateEntropy(originalDistribution, originalData.size());
            double anonymizedEntropy = calculateEntropy(anonymizedDistribution, anonymizedData.size());
            double privacy = anonymizedEntropy / originalEntropy;
            columnPrivacy.put(column, privacy);
        }
        
        // 返回所有欄位的平均隱私保障
        return columnPrivacy.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }
    
    /**
     * 計算 Map 格式資料的欄位統計特性
     * 包括：唯一值數量、值分布、最大頻率等
     */
    private static Map<String, Map<String, Double>> calculateMapColumnStatistics(List<Map<String, String>> data) {
        Map<String, Map<String, Double>> stats = new HashMap<>();
        Set<String> columns = data.get(0).keySet();
        
        for (String column : columns) {
            Map<String, Double> columnStats = new HashMap<>();
            
            // 計算唯一值數量
            Set<String> uniqueValues = new HashSet<>();
            for (Map<String, String> row : data) {
                uniqueValues.add(row.get(column));
            }
            columnStats.put("uniqueCount", (double) uniqueValues.size());
            
            // 計算值分布
            Map<String, Integer> valueCounts = new HashMap<>();
            for (Map<String, String> row : data) {
                String value = row.get(column);
                valueCounts.merge(value, 1, Integer::sum);
            }
            
            // 計算最大頻率
            double maxFrequency = valueCounts.values().stream()
                .mapToDouble(count -> (double) count / data.size())
                .max()
                .orElse(0.0);
            columnStats.put("maxFrequency", maxFrequency);
            
            stats.put(column, columnStats);
        }
        
        return stats;
    }
    
    /**
     * 計算 List 格式資料的欄位統計特性
     * 包括：唯一值數量、值分布、最大頻率等
     */
    private static Map<String, Map<String, Double>> calculateListColumnStatistics(List<List<String>> data) {
        Map<String, Map<String, Double>> stats = new HashMap<>();
        int columnCount = data.get(0).size();
        
        for (int i = 0; i < columnCount; i++) {
            Map<String, Double> columnStats = new HashMap<>();
            
            // 計算唯一值數量
            Set<String> uniqueValues = new HashSet<>();
            for (List<String> row : data) {
                uniqueValues.add(row.get(i));
            }
            columnStats.put("uniqueCount", (double) uniqueValues.size());
            
            // 計算值分布
            Map<String, Integer> valueCounts = new HashMap<>();
            for (List<String> row : data) {
                String value = row.get(i);
                valueCounts.merge(value, 1, Integer::sum);
            }
            
            // 計算最大頻率
            double maxFrequency = valueCounts.values().stream()
                .mapToDouble(count -> (double) count / data.size())
                .max()
                .orElse(0.0);
            columnStats.put("maxFrequency", maxFrequency);
            
            stats.put("column" + i, columnStats);
        }
        
        return stats;
    }
    
    /**
     * 計算資訊熵
     * 用於評估資料的隨機性和不確定性
     */
    private static double calculateEntropy(Map<String, Integer> distribution, int total) {
        return distribution.values().stream()
            .mapToDouble(count -> {
                double probability = (double) count / total;
                return -probability * Math.log(probability);
            })
            .sum();
    }
    
    /**
     * 計算平均值差異
     * 評估數值型欄位在匿名化前後的平均值變化
     * 值域範圍：0（無差異）到 1（最大差異）
     */
    private static double calculateMeanDifference(List<Map<String, String>> originalData, 
                                                List<List<String>> anonymizedData) {
        if (originalData.isEmpty() || anonymizedData.isEmpty()) {
            return 1.0;
        }
        
        // 計算每個數值型欄位的平均值差異
        Map<String, Double> columnDifferences = new HashMap<>();
        Set<String> columns = originalData.get(0).keySet();
        
        for (String column : columns) {
            try {
                // 檢查該欄位是否被完全匿名化
                boolean isFullyAnonymized = anonymizedData.stream()
                    .allMatch(row -> row.get(new ArrayList<>(columns).indexOf(column)).equals("*"));
                
                if (!isFullyAnonymized) {
                    double originalMean = calculateMean(originalData, column);
                    int columnIndex = new ArrayList<>(columns).indexOf(column);
                    double anonymizedMean = calculateMean(anonymizedData, columnIndex);
                    
                    // 避免除以零
                    if (originalMean != 0) {
                        // 計算相對差異
                        double difference = Math.abs((anonymizedMean - originalMean) / originalMean);
                        columnDifferences.put(column, difference);
                    }
                }
            } catch (NumberFormatException e) {
                // 如果欄位不是數值型，則跳過
                continue;
            }
        }
        
        // 如果沒有可計算的數值型欄位，返回 0
        if (columnDifferences.isEmpty()) {
            return 0.0;
        }
        
        // 返回所有數值型欄位的平均差異
        return columnDifferences.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }
    
    /**
     * 計算分類準確率
     * 評估分類型欄位在匿名化前後的分布相似度
     * 值域範圍：0（完全不一致）到 1（完全一致）
     */
    private static double calculateClassificationAccuracy(List<Map<String, String>> originalData, 
                                                       List<List<String>> anonymizedData) {
        if (originalData.isEmpty() || anonymizedData.isEmpty()) {
            return 0.0;
        }
        
        // 計算每個分類欄位的準確率
        Map<String, Double> columnAccuracies = new HashMap<>();
        Set<String> columns = originalData.get(0).keySet();
        
        for (String column : columns) {
            // 計算原始資料中該欄位的值分布
            Map<String, Integer> originalDistribution = new HashMap<>();
            for (Map<String, String> row : originalData) {
                String value = row.get(column);
                originalDistribution.merge(value, 1, Integer::sum);
            }
            
            // 計算匿名化後該欄位的值分布
            Map<String, Integer> anonymizedDistribution = new HashMap<>();
            int columnIndex = new ArrayList<>(columns).indexOf(column);
            for (List<String> row : anonymizedData) {
                String value = row.get(columnIndex);
                anonymizedDistribution.merge(value, 1, Integer::sum);
            }
            
            // 計算該欄位的分類準確率
            double accuracy = calculateDistributionSimilarity(originalDistribution, anonymizedDistribution, originalData.size());
            columnAccuracies.put(column, accuracy);
        }
        
        // 返回所有欄位的平均準確率
        return columnAccuracies.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }
    
    /**
     * 計算 Map 格式資料的欄位平均值
     */
    private static double calculateMean(List<Map<String, String>> data, String column) {
        return data.stream()
            .mapToDouble(row -> Double.parseDouble(row.get(column)))
            .average()
            .orElse(0.0);
    }
    
    /**
     * 計算 List 格式資料的欄位平均值
     */
    private static double calculateMean(List<List<String>> data, int columnIndex) {
        return data.stream()
            .filter(row -> !row.get(columnIndex).equals("*"))  // 排除被匿名化的資料
            .mapToDouble(row -> Double.parseDouble(row.get(columnIndex)))
            .average()
            .orElse(0.0);
    }
    
    /**
     * 計算兩個分布之間的相似度
     * 基於每個類別的比例差異來計算
     * 值域範圍：0（完全不一致）到 1（完全一致）
     */
    private static double calculateDistributionSimilarity(Map<String, Integer> original, 
                                                       Map<String, Integer> anonymized, 
                                                       int totalCount) {
        double similarity = 0.0;
        
        // 計算每個類別的分布差異
        for (String category : original.keySet()) {
            double originalRatio = (double) original.get(category) / totalCount;
            double anonymizedRatio = (double) anonymized.getOrDefault(category, 0) / totalCount;
            similarity += 1.0 - Math.abs(originalRatio - anonymizedRatio);
        }
        
        // 返回平均相似度
        return similarity / original.size();
    }
} 