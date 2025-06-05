package com.anonymity.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.anonymity.service.AnonymityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/anonymity")
@Tag(name = "數據匿名化", description = "提供k-Anonymity和l-Diversity匿名化功能")
public class AnonymityController {

    private static final Logger logger = LoggerFactory.getLogger(AnonymityController.class);

    @Autowired
    private AnonymityService anonymityService;

    @Operation(summary = "匿名化數據", description = "使用k-Anonymity和l-Diversity算法對數據進行匿名化處理")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "匿名化成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "請求參數無效",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "500", description = "處理請求時發生錯誤",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/anonymize")
    public ResponseEntity<?> anonymizeData(
            @Parameter(description = "匿名化請求參數", required = true)
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> data = (List<Map<String, String>>) request.get("data");
            @SuppressWarnings("unchecked")
            List<String> quasiIdentifiers = (List<String>) request.get("quasiIdentifiers");
            @SuppressWarnings("unchecked")
            List<String> sensitiveAttributes = (List<String>) request.get("sensitiveAttributes");
            int k = ((Number) request.get("k")).intValue();
            double l = ((Number) request.get("l")).doubleValue();
            logger.info("parameter: k={}, l={}", k, l);

            Map<String, Object> result = anonymityService.anonymizeData(
                data, quasiIdentifiers, sensitiveAttributes, k, l);
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.error("請求參數無效", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "請求參數無效",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("處理請求時發生錯誤", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "處理請求時發生錯誤",
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "生成並匿名化測試資料", description = "生成指定數量的測試資料並進行k-Anonymity和l-Diversity匿名化處理")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "處理成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "請求參數無效",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "500", description = "處理請求時發生錯誤",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/anonymizeTest")
    public ResponseEntity<?> anonymizeTest(
            @Parameter(description = "資料筆數", required = true)
            @RequestParam int dataSize,
            @Parameter(description = "k值 (k-Anonymity)", required = true)
            @RequestParam int k,
            @Parameter(description = "l值 (l-Diversity)", required = true)
            @RequestParam int l) {
        try {
            if (dataSize < k) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "請求參數無效",
                    "message", "資料筆數必須大於等於k值"
                ));
            }
            
            Map<String, Object> result = anonymityService.generateAndAnonymizeTestData(dataSize, k, l);
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.error("請求參數無效", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "請求參數無效",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("處理請求時發生錯誤", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "處理請求時發生錯誤",
                "message", e.getMessage()
            ));
        }
    }
} 