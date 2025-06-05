# 差分隱私測試專案

## 專案說明
本專案實現了基於差分隱私（Differential Privacy）的資料匿名化功能，並提供了完整的測試框架來評估匿名化效果。專案使用 ARX 框架實現差分隱私保護，並提供了多種評估指標來衡量匿名化效果。

## 主要功能
1. 差分隱私匿名化
   - 支援資料相依（Data Dependent）和資料獨立（Data Independent）兩種模式
   - 可調整 epsilon 和 delta 參數
   - 自動處理超時情況（60秒）

2. 測試框架
   - 自動生成測試資料
   - 多參數組合測試
   - 結果自動儲存（Excel 和 TXT 格式）

## 評估指標
專案提供以下評估指標來衡量匿名化效果：

1. 資訊損失（Information Loss）
   - 評估資料匿名化後資訊的保留程度
   - 值域範圍：0（無損失）到 1（完全損失）
   - 計算方式：基於每個欄位的唯一值數量變化

2. 效用損失（Utility Loss）
   - 評估資料匿名化後對統計分析的影響
   - 值域範圍：0（無損失）到 1（完全損失）
   - 計算方式：基於每個欄位的統計特性變化

3. 隱私保障（Privacy Guarantee）
   - 評估資料匿名化後的隱私保護程度
   - 值域範圍：0（無保護）到 1（完全保護）
   - 計算方式：基於資訊熵的變化

4. 平均值差異（Mean Difference）
   - 評估數值型資料的統計特性保持程度
   - 值域範圍：0（無差異）到 1（最大差異）
   - 計算方式：比較匿名化前後的平均值變化

5. 分類準確率（Classification Accuracy）
   - 評估分類型資料的分布相似度
   - 值域範圍：0（完全不一致）到 1（完全一致）
   - 計算方式：比較匿名化前後的類別分布

## 測試結果輸出
測試結果會以兩種格式儲存：

1. Excel 檔案（dp_test_summary.xlsx）
   - 包含所有測試參數組合的結果
   - 自動調整欄寬
   - 方便視覺化查看和分析

2. TXT 檔案（dp_test_summary.txt）
   - CSV 格式，使用逗號分隔
   - UTF-8 編碼
   - 適合程式讀取和資料分析

## 使用方式
1. 執行測試
   ```java
   @Test
   public void testDpDependentLoop()
   ```

2. 查看結果
   - 檢查控制台輸出的執行狀態
   - 查看生成的 Excel 和 TXT 檔案
   - 分析各項評估指標

## 注意事項
1. 測試資料會自動生成，預設為 2000 筆
2. 匿名化過程有 60 秒的超時限制
3. 結果檔案會儲存在 C:\Ken 目錄下
4. 建議使用較新版本的 Java 運行環境

## 資料隱私保護服務

這是一個基於 Spring Boot 的資料隱私保護服務，提供資料匿名化和隱私保護功能。

## 功能特點

- 支援 k-匿名化 (k-Anonymity)
- 支援 l-多樣性 (l-Diversity)
- 提供 RESTful API 介面
- 支援 JSON 格式的資料輸入和輸出
- 提供 Swagger UI 文檔

## 技術架構

- Java 17
- Spring Boot 3.2.3
- ARX Library 3.9.1
- Apache POI 5.2.5
- Swagger UI

## API 文檔

### 資料匿名化處理

將輸入的 JSON 資料進行 k-匿名化和 l-多樣性處理。

**請求方式：** POST  
**端點：** `/api/privacy/anonymize`  
**Content-Type：** application/json

#### 請求參數

```json
{
    "data": [
        {
            "年齡": "25",
            "性別": "男",
            "縣市": "台北市",
            "疾病": "感冒"
        }
    ],
    "k": 3,                    // 可選，預設值為 3
    "l": 2.5,                  // 可選，預設值為 2.0
    "quasiIdentifiers": [      // 可選，預設值如下
        "年齡",
        "性別",
        "郵遞區號",
        "縣市"
    ],
    "sensitiveAttributes": [   // 可選，預設值如下
        "疾病",
        "檢驗結果",
        "是否確診"
    ]
}
```

#### 回應格式

```json
{
    "data": [
        {
            "年齡": "20-30",
            "性別": "男",
            "縣市": "台北市",
            "疾病": "感冒"
        }
    ],
    "statistics": {
        "k": 3,
        "l": 2.5,
        "informationLoss": 0.15
    }
}
```

## 開發環境設置

1. 確保已安裝 Java 17 或更高版本
2. 克隆專案
3. 使用 Maven 編譯專案：
   ```bash
   mvn clean install
   ```
4. 運行應用程式：
   ```bash
   mvn spring-boot:run
   ```

## Swagger UI

啟動應用程式後，可以通過以下 URL 訪問 Swagger UI：
```
http://localhost:8089/swagger-ui.html
```

## 授權

本專案採用 MIT 授權條款。 