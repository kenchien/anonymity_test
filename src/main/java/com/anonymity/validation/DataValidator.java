package com.anonymity.validation;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class DataValidator {
    
    public void validateRequest(List<Map<String, String>> data,
                              List<String> quasiIdentifiers,
                              List<String> sensitiveAttributes,
                              int k,
                              double l) throws IllegalArgumentException {
        
        // 驗證數據不為空
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("數據不能為空");
        }
        
        // 驗證準識別符
        if (quasiIdentifiers == null || quasiIdentifiers.isEmpty()) {
            throw new IllegalArgumentException("準識別符不能為空");
        }
        
        // 驗證敏感屬性
        if (sensitiveAttributes == null || sensitiveAttributes.isEmpty()) {
            throw new IllegalArgumentException("敏感屬性不能為空");
        }
        
        // 驗證k和l值
        if (k < 2) {
            throw new IllegalArgumentException("k值必須大於等於2");
        }
        if (l < 1.0) {
            throw new IllegalArgumentException("l值必須大於等於1.0");
        }
        
        // 驗證數據格式
        Map<String, String> firstRow = data.get(0);
        for (String identifier : quasiIdentifiers) {
            if (!firstRow.containsKey(identifier)) {
                throw new IllegalArgumentException("準識別符 '" + identifier + "' 在數據中不存在");
            }
        }
        
        for (String attribute : sensitiveAttributes) {
            if (!firstRow.containsKey(attribute)) {
                throw new IllegalArgumentException("敏感屬性 '" + attribute + "' 在數據中不存在");
            }
        }
        
        // 驗證數據量是否足夠
        if (data.size() < k) {
            throw new IllegalArgumentException("數據量必須大於等於k值");
        }
        
        // 驗證敏感屬性的唯一值數量
        for (String attribute : sensitiveAttributes) {
            long uniqueValues = data.stream()
                .map(row -> row.get(attribute))
                .distinct()
                .count();
            if (uniqueValues < l) {
                throw new IllegalArgumentException("敏感屬性 '" + attribute + "' 的唯一值數量必須大於等於l值");
            }
        }
    }
} 