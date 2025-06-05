package com.anonymity.controller;

import com.anonymity.controller.dto.DifferentialPrivacyRequest;
import com.anonymity.service.DifferentialPrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/differential-privacy")
@Tag(name = "差分隱私", description = "差分隱私相關的 API")
public class DifferentialPrivacyController {

    private final DifferentialPrivacyService differentialPrivacyService;

    @Autowired
    public DifferentialPrivacyController(DifferentialPrivacyService differentialPrivacyService) {
        this.differentialPrivacyService = differentialPrivacyService;
    }

    @PostMapping("/apply")
    @Operation(summary = "應用差分隱私", description = "使用指定的隱私預算對數據應用差分隱私")
    public ResponseEntity<Map<String, Object>> applyDifferentialPrivacy(
            @Valid @RequestBody DifferentialPrivacyRequest request) {
        try {
            Map<String, Object> result = differentialPrivacyService.applyDifferentialPrivacy(
                request.getData(), 
                request.getEpsilon(),
                request.getDelta(),
                request.getIsDataIndependent()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 