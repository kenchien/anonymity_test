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

// 圖表實例
let privacyUtilityChart;
let anonymizationDistributionChart;

// 初始化網格和圖表
document.addEventListener('DOMContentLoaded', function() {
    // 初始化 AG Grid
    const gridDiv = document.querySelector('#myGrid');
    new agGrid.Grid(gridDiv, gridOptions);
    gridApi = gridOptions.api;

    // 初始化圖表
    initializeCharts();

    // 綁定表單提交事件
    document.getElementById('anonymizeForm').addEventListener('submit', handleFormSubmit);
});

// 初始化圖表
function initializeCharts() {
    // 隱私保障 vs 資料可用性圖表
    const privacyUtilityCtx = document.getElementById('privacyUtilityChart').getContext('2d');
    privacyUtilityChart = new Chart(privacyUtilityCtx, {
        type: 'scatter',
        data: {
            datasets: [{
                label: '資料點',
                data: [],
                backgroundColor: 'rgba(54, 162, 235, 0.5)',
                borderColor: 'rgba(54, 162, 235, 1)',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    title: {
                        display: true,
                        text: '隱私保障'
                    }
                },
                y: {
                    title: {
                        display: true,
                        text: '資料可用性'
                    }
                }
            }
        }
    });

    // 匿名化分布圖表
    const anonymizationDistributionCtx = document.getElementById('anonymizationDistributionChart').getContext('2d');
    anonymizationDistributionChart = new Chart(anonymizationDistributionCtx, {
        type: 'pie',
        data: {
            labels: ['完全匿名', '部分匿名', '未匿名'],
            datasets: [{
                data: [0, 0, 0],
                backgroundColor: [
                    'rgba(255, 99, 132, 0.5)',
                    'rgba(54, 162, 235, 0.5)',
                    'rgba(255, 206, 86, 0.5)'
                ],
                borderColor: [
                    'rgba(255, 99, 132, 1)',
                    'rgba(54, 162, 235, 1)',
                    'rgba(255, 206, 86, 1)'
                ],
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false
        }
    });
}

// 處理表單提交
async function handleFormSubmit(event) {
    event.preventDefault();
    
    const formData = {
        k: parseInt(document.getElementById('kValue').value),
        l: parseFloat(document.getElementById('lValue').value),
        epsilon: parseFloat(document.getElementById('epsilon').value),
        delta: parseFloat(document.getElementById('delta').value)
    };

    try {
        const response = await fetch('/api/differential-privacy/apply', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        if (!response.ok) {
            throw new Error('API 請求失敗');
        }

        const result = await response.json();
        updateDashboard(result);
    } catch (error) {
        console.error('處理請求時發生錯誤:', error);
        alert('處理請求時發生錯誤: ' + error.message);
    }
}

// 更新儀表板
function updateDashboard(data) {
    // 更新關鍵指標
    document.getElementById('privacyScore').textContent = (data.privacyScore * 100).toFixed(1) + '%';
    document.getElementById('utilityScore').textContent = (data.utilityScore * 100).toFixed(1) + '%';
    document.getElementById('anonymizationRate').textContent = (data.anonymizationRate * 100).toFixed(1) + '%';
    document.getElementById('processingTime').textContent = data.processingTime.toFixed(2);

    // 更新隱私保障 vs 資料可用性圖表
    privacyUtilityChart.data.datasets[0].data = data.privacyUtilityData.map(point => ({
        x: point.privacy,
        y: point.utility
    }));
    privacyUtilityChart.update();

    // 更新匿名化分布圖表
    anonymizationDistributionChart.data.datasets[0].data = [
        data.fullyAnonymized,
        data.partiallyAnonymized,
        data.notAnonymized
    ];
    anonymizationDistributionChart.update();

    // 更新資料表格
    gridApi.setRowData(data.result);
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