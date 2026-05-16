let donutChart;
let fullData = [];

function initCharts() {
    const options = {
        chart: { type: 'donut', height: 250 },
        series: [],
        labels: [],
        colors: ['#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'],
        legend: { position: 'bottom' }
    };
    donutChart = new ApexCharts(document.querySelector("#donutChart"), options);
    donutChart.render();
}

function updateDashboard(data) {
    document.getElementById('totalStock').innerText = data.totalStock.toLocaleString();
    document.getElementById('warehouseValue').innerText = new Intl.NumberFormat('vi-VN').format(data.warehouseValue) + 'đ';
    document.getElementById('totalLoss').innerText = new Intl.NumberFormat('vi-VN').format(data.totalLoss) + 'đ';
    document.getElementById('lowStockCount').innerText = data.lowStockCount;

    // Biểu đồ
    const labels = Object.keys(data.categoryDistribution);
    const series = Object.values(data.categoryDistribution);
    donutChart.updateSeries(series);
    donutChart.updateOptions({ labels: labels });

    // 🔥 Render Cảnh báo thực tế từ Java Logic
    const feed = document.getElementById('alertsFeed');
    feed.innerHTML = "";
    if (data.alertsFeed && data.alertsFeed.length > 0) {
        data.alertsFeed.forEach(msg => {
            let borderColor = "var(--warning)";
            let bgColor = "#fffbeb";
            let textColor = "#9a3412";
            let type = "Cảnh báo hệ thống";

            if (msg.includes("NGHIÊM TRỌNG") || msg.includes("BÁO ĐỘNG ĐỎ")) {
                borderColor = "var(--danger)"; bgColor = "#fee2e2"; textColor = "#991b1b"; type = "Rủi ro thất thoát";
            }

            feed.innerHTML += `
                <div style="
                    padding: 12px 15px; 
                    border-left: 4px solid ${borderColor}; 
                    background: ${bgColor}; 
                    border-radius: 8px; 
                    margin-bottom: 10px;
                    word-break: break-word;
                ">
                    <small style="color:${textColor}; font-weight:600; display:block; margin-bottom:4px;">
                        ${type}
                    </small>
                    <p style="margin:0; font-size:13px; color:#1e293b; line-height:1.5;">
                        ${msg}
                    </p>
                </div>
            `;
        });
    } else {
        feed.innerHTML = "<div style='color:#64748b; font-size:13px; padding:10px;'>Trạng thái kho hàng ổn định. Không có cảnh báo.</div>";
    }

    // Bảng dữ liệu
    fullData = data.inventoryTable;
    // Gọi ngay bộ lọc khi nạp xong dữ liệu
    filterTable(); 
}
function loaiBoDauTiengViet(str) {
    if (!str) return "";
    return str.toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/đ/g, "d");
}
function renderTable(items) {
    const tbody = document.getElementById('tableBody');
    if (items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding:30px; color:#64748b;">Không tìm thấy sản phẩm nào!</td></tr>`;
        return;
    }

    tbody.innerHTML = items.map(item => `
        <tr>
            <td style="color:var(--primary); font-weight:600;">${item.id}</td>
            <td style="font-weight:600;">${item.name}</td>
            <td><span style="font-size:12px; background:#f1f5f9; padding:4px 8px; border-radius:6px;">${item.category}</span></td>
            <td style="font-weight:bold; color:${item.stock <= 10 ? 'var(--danger)' : '#1e293b'}">${item.stock}</td>
            <td>
                <span class="badge ${item.stock > 10 ? 'badge-success' : 'badge-warning'}" 
                      style="${item.stock === 0 ? 'background:#fee2e2; color:#991b1b;' : ''}">
                    ${item.stock > 10 ? 'Ổn định' : (item.stock === 0 ? 'Hết hàng' : 'Thấp')}
                </span>
            </td>
            <td><button onclick="alert('ACTION:VIEW_${item.id}')" style="border:1px solid #e2e8f0; border-radius:6px; padding:6px 12px; background:#fff; color:var(--primary); cursor:pointer; font-weight:500;">Chi tiết</button></td>
        </tr>
    `).join('');
}

function generateAlerts(items) {
    const lowStock = items.filter(i => i.stock < 10);
    document.getElementById('lowStockCount').innerText = lowStock.length;
    
    const feed = document.getElementById('alertsFeed');
    feed.innerHTML = lowStock.map(i => `
        <div style="padding:12px; border-left:4px solid var(--warning); background:#fffbeb; border-radius:8px; margin-bottom:10px;">
            <small style="color:#9a3412">Hết hàng sắp xảy ra</small>
            <p style="margin:5px 0 0; font-size:13px;"><b>${i.name}</b> chỉ còn ${i.stock} sản phẩm trong kho.</p>
        </div>
    `).join('');
}

function filterTable() {
    const searchRaw = document.getElementById('search').value;
    const query = loaiBoDauTiengViet(searchRaw.trim());
    
    let filtered = fullData.filter(i => {
        const tenSP = loaiBoDauTiengViet(i.name);
        const maSP = loaiBoDauTiengViet(i.id);
        return tenSP.includes(query) || maSP.includes(query);
    });

    // Sắp xếp: Hàng Tồn kho thấp (<= 10) bị đẩy LÊN ĐẦU để quản lý chú ý!
    filtered.sort((a, b) => {
        let scoreA = a.stock <= 10 ? 0 : 1;
        let scoreB = b.stock <= 10 ? 0 : 1;
        if (scoreA !== scoreB) return scoreA - scoreB;
        return a.stock - b.stock; // Sắp xếp tăng dần theo số lượng
    });

    renderTable(filtered);
}

document.addEventListener('DOMContentLoaded', initCharts);