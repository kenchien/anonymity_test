package com.anonymity.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class DifferentialPrivacyRequest {
    @NotNull(message = "隱私預算不能為空")
    @Positive(message = "隱私預算必須大於0")
    private Double epsilon;
    
    @NotNull(message = "delta 值不能為空")
    @Positive(message = "delta 值必須大於0")
    private Double delta;
    
    @NotNull(message = "數據不能為空")
    private String data;
    
    private Boolean isDataIndependent = false; // 預設使用資料相依差分隱私

    public Double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(Double epsilon) {
        this.epsilon = epsilon;
    }

    public Double getDelta() {
        return delta;
    }

    public void setDelta(Double delta) {
        this.delta = delta;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Boolean getIsDataIndependent() {
        return isDataIndependent;
    }

    public void setIsDataIndependent(Boolean isDataIndependent) {
        this.isDataIndependent = isDataIndependent;
    }
} 