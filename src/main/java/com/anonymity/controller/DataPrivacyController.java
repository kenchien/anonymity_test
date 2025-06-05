package com.anonymity.controller;

import com.anonymity.service.ExcelService;
import com.anonymity.service.AnonymityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/privacy")
@Tag(name = "資料隱私保護", description = "提供資料匿名化和隱私保護功能，支援 k-匿名化和 l-多樣性處理")
public class DataPrivacyController {

    private static final Logger logger = LoggerFactory.getLogger(DataPrivacyController.class);
    private static final List<String> DEFAULT_QUASI_IDENTIFIERS = Arrays.asList("年齡", "性別", "郵遞區號", "縣市");
    private static final List<String> DEFAULT_SENSITIVE_ATTRIBUTES = Arrays.asList("疾病", "檢驗結果", "是否確診");

    @Autowired
    private ExcelService excelService;

    @Autowired
    private AnonymityService anonymityService;

    @PostMapping(value = "/anonymize", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "匿名化資料",
        description = "將輸入的資料進行 k-anonymity 和 l-diversity 匿名化處理。\n\n" +
                     "k-anonymity 確保每個等價類中至少有 k 筆相同的記錄。\n" +
                     "l-diversity 確保每個等價類中至少有 l 個不同的敏感屬性值。"
    )
    @ApiResponse(
        responseCode = "200",
        description = "匿名化成功",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                implementation = Map.class,
                description = "匿名化處理結果"
            )
        )
    )
    @ApiResponse(responseCode = "400", description = "請求參數錯誤")
    @ApiResponse(responseCode = "500", description = "伺服器內部錯誤")
    public ResponseEntity<?> anonymizeData(
            @RequestBody Map<String, Object> request) {
        try {
            logger.info("收到匿名化請求");
            
            // 從請求中獲取資料
            @SuppressWarnings("unchecked")
            List<Map<String, String>> data = (List<Map<String, String>>) request.get("data");
            
            // 使用預設值或請求中的值
            List<String> quasiIdentifiers = request.get("quasiIdentifiers") != null ? 
                (List<String>) request.get("quasiIdentifiers") : 
                DEFAULT_QUASI_IDENTIFIERS;
                
            List<String> sensitiveAttributes = request.get("sensitiveAttributes") != null ? 
                (List<String>) request.get("sensitiveAttributes") : 
                DEFAULT_SENSITIVE_ATTRIBUTES;
            
            // 從請求中獲取 k 和 l 值
            int k = ((Number) request.get("k")).intValue();
            double l = ((Number) request.get("l")).doubleValue();
            
            logger.info("匿名化參數: k={}, l={}", k, l);
            
            // 驗證必要參數
            if (data == null || data.isEmpty()) {
                return ResponseEntity.badRequest().body("缺少必要參數：data");
            }
            
            // 呼叫匿名化服務
            Map<String, Object> result = anonymityService.anonymizeData(
                data, 
                quasiIdentifiers, 
                sensitiveAttributes, 
                k, 
                l
            );
            
            // 構建回應
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "匿名化處理成功");
            response.put("data", result.get("data"));  // 匿名化後的資料
            response.put("statistics", result.get("statistics"));  // 統計資訊
            
            // 創建參數映射
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("k", k);
            parameters.put("l", l);
            parameters.put("quasiIdentifiers", quasiIdentifiers);
            parameters.put("sensitiveAttributes", sensitiveAttributes);
            response.put("parameters", parameters);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("參數驗證失敗", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("匿名化處理失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("匿名化處理失敗：" + e.getMessage());
        }
    }
} 