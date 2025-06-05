package com.anonymity.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

@Service
public class ExcelService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);
    
    public Map<String, Object> processExcelFile(MultipartFile file, int k, int l) throws IOException {
        Workbook workbook = null;
        try {
            workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);
            
            // 讀取標題行
            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }
            
            // 讀取數據
            List<Map<String, String>> data = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    String value = getCellValueAsString(cell);
                    rowData.put(headers.get(j), value);
                }
                data.add(rowData);
            }
            
            // 準備匿名化請求
            Map<String, Object> request = new HashMap<>();
            request.put("data", data);
            request.put("quasiIdentifiers", Arrays.asList("年齡", "性別", "縣市", "通報日期"));
            request.put("sensitiveAttributes", Arrays.asList("疾病", "檢驗結果", "是否確診"));
            request.put("k", k);
            request.put("l", l);
            
            return request;
        } finally {
            
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        try {
            return cell.getStringCellValue();
        } catch (Exception e) {
            logger.error("轉換單元格值時發生錯誤", e);
            return "";
        }
    }
} 