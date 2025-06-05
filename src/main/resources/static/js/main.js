// 初始化 AG Grid
let gridApi;
const columnDefs = [
    { field: '年齡', sortable: true, filter: true },
    { field: '性別', sortable: true, filter: true },
    { field: '縣市', sortable: true, filter: true },
    { field: '疾病', sortable: true, filter: true },
    { field: '檢驗結果', sortable: true, filter: true },
    { field: '是否確診', sortable: true, filter: true }
];

const gridOptions = {
    columnDefs: columnDefs,
    defaultColDef: {
        flex: 1,
        minWidth: 100,
        resizable: true
    },
    pagination: true,
    paginationPageSize: 10,
    domLayout: 'autoHeight'
};

// 初始化網格
document.addEventListener('DOMContentLoaded', function() {
    const gridDiv = document.querySelector('#myGrid');
    new agGrid.Grid(gridDiv, gridOptions);
    gridApi = gridOptions.api;
});

// 處理表單提交
document.getElementById('anonymizeForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    // 獲取表單值
    const k = parseInt(document.getElementById('kValue').value);
    const l = parseFloat(document.getElementById('lValue').value);
    
    // 獲取選中的準識別符
    const quasiIdentifiers = [];
    if (document.getElementById('qiAge').checked) quasiIdentifiers.push('年齡');
    if (document.getElementById('qiGender').checked) quasiIdentifiers.push('性別');
    if (document.getElementById('qiCity').checked) quasiIdentifiers.push('縣市');
    
    // 準備請求資料
    const requestData = {
        data: window.testData || [], // 假設我們有測試資料
        k: k,
        l: l,
        quasiIdentifiers: quasiIdentifiers,
        sensitiveAttributes: ['疾病', '檢驗結果', '是否確診']
    };
    
    try {
        // 發送請求
        const response = await fetch('/api/privacy/anonymize', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        
        if (result.success) {
            // 更新統計資訊
            updateStatistics(result.statistics);
            
            // 更新網格資料
            gridApi.setRowData(result.data);
            
            // 顯示成功訊息
            alert('資料處理成功！');
        } else {
            throw new Error(result.message || '處理失敗');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('處理失敗：' + error.message);
    }
});

// 更新統計資訊
function updateStatistics(stats) {
    document.getElementById('statsK').textContent = stats.k;
    document.getElementById('statsL').textContent = stats.l;
    document.getElementById('statsRows').textContent = stats.rows;
    document.getElementById('statsLoss').textContent = (stats.informationLoss * 100).toFixed(2) + '%';
}

// 生成測試資料
function generateTestData(size = 100) {
    const data = [];
    const cities = ['台北市', '新北市', '桃園市', '台中市', '台南市', '高雄市'];
    const diseases = ['流感', '糖尿病', '高血壓', '氣喘', '癌症'];
    const testResults = ['陽性', '陰性', '待確認'];
    const diagnosisStatus = ['確診', '未確診', '待確認'];
    
    for (let i = 0; i < size; i++) {
        data.push({
            '年齡': String(20 + Math.floor(Math.random() * 60)),
            '性別': Math.random() > 0.5 ? '男' : '女',
            '縣市': cities[Math.floor(Math.random() * cities.length)],
            '疾病': diseases[Math.floor(Math.random() * diseases.length)],
            '檢驗結果': testResults[Math.floor(Math.random() * testResults.length)],
            '是否確診': diagnosisStatus[Math.floor(Math.random() * diagnosisStatus.length)]
        });
    }
    
    return data;
}

// 初始化測試資料
window.testData = generateTestData(2000); 