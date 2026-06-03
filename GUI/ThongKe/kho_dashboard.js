let donutChart;
let fullData = [];

let calViewYear  = new Date().getFullYear();
let calViewMonth = new Date().getMonth(); 
let calSelStart  = null; 
let calSelEnd    = null; 
let calPickStep  = 1;

let lastRawData = null;

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
    lastRawData = data;
    document.getElementById('totalStock').innerText = data.totalStock.toLocaleString();
    document.getElementById('warehouseValue').innerText = new Intl.NumberFormat('vi-VN').format(data.warehouseValue) + 'đ';
    
    // =========================================================================
    // 🔥 1. TÍNH TOÁN VÀ HIỂN THỊ TREND SO SÁNH (ĐẦU KỲ vs CUỐI KỲ)
    // =========================================================================
    const renderTrend = (startVal, endVal) => {
        // Chống lỗi nếu data chưa truyền xuống đủ
        if (startVal === undefined || endVal === undefined) return '';
        
        const diff = endVal - startVal;
        
        if (diff === 0) {
            return `<span style="font-size:11px; font-weight:600; color:#64748b; background:#f1f5f9; padding:2px 6px; border-radius:4px;">- Không đổi</span>`;
        }
        
        let percent = startVal === 0 ? 100 : Math.abs((diff / startVal) * 100);

        // Quy định màu sắc: Xanh (Tăng) / Đỏ (Giảm)
        const isUp = diff > 0;
        const color = isUp ? "#10b981" : "#ef4444";
        const bg = isUp ? "#ecfdf5" : "#fef2f2";
        const icon = isUp ? "▲" : "▼";
        const sign = isUp ? "+" : "-";

        return `<span style="font-size:11px; font-weight:700; color:${color}; background:${bg}; padding:3px 6px; border-radius:4px; display:inline-flex; align-items:center; gap:2px;">
                    <span style="font-size:9px;">${icon}</span> ${sign}${Math.abs(diff).toLocaleString()} (${percent.toFixed(1)}%)
                </span>`;
    };

    // Đổ kết quả so sánh ra HTML (kiểm tra phần tử tồn tại để tránh lỗi JS)
    const stockTrendEl = document.getElementById('stockTrend');
    if (stockTrendEl) stockTrendEl.innerHTML = renderTrend(data.totalStockStart, data.totalStock);
    
    const valTrendEl = document.getElementById('valTrend');
    if (valTrendEl) valTrendEl.innerHTML = renderTrend(data.warehouseValueStart, data.warehouseValue);


    // =========================================================================
    // 2. CẬP NHẬT CÁC CHỈ SỐ KHÁC VÀ BIỂU ĐỒ
    // =========================================================================
    document.getElementById('destructionCost').innerText = new Intl.NumberFormat('vi-VN').format(data.destructionCost) + 'đ';
    document.getElementById('destructionQty').innerText = data.destructionQty.toLocaleString() + ' sản phẩm đã hủy';
    document.getElementById('lowStockCount').innerText = data.lowStockCount;

    // Cập nhật Biểu đồ Donut
    const labels = Object.keys(data.categoryDistribution);
    const series = Object.values(data.categoryDistribution);
    donutChart.updateSeries(series);
    donutChart.updateOptions({ labels: labels });


    // =========================================================================
    // 3. RENDER CẢNH BÁO THỰC TẾ TỪ JAVA LOGIC
    // =========================================================================
    const feed = document.getElementById('alertsFeed');
    feed.innerHTML = "";
    if (data.alertsFeed && data.alertsFeed.length > 0) {
        data.alertsFeed.forEach(msg => {
            // Mặc định là thông báo Tốt (Màu xanh)
            let borderColor = "var(--success)";
            let bgColor = "#ecfdf5";
            let textColor = "#047857";
            let type = "Thông tin hệ thống";
            let icon = "✅";

            // Nhận diện Cảnh báo (Màu cam)
            let iconHtml = "";
            if (msg.includes("BAO DONG") || msg.includes("NGHIEM TRONG") || 
                msg.includes("BÁO ĐỘNG") || msg.includes("NGHIÊM TRỌNG")) {
                borderColor = "var(--danger)"; 
                bgColor = "#fee2e2"; 
                textColor = "#991b1b"; 
                type = "Rủi ro thất thoát";
                // SVG tam giác cảnh báo
                iconHtml = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#991b1b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:middle;margin-right:5px;"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M12 9v4"/><path d="M10.363 3.591l-8.106 13.534a1.914 1.914 0 0 0 1.636 2.871h16.214a1.914 1.914 0 0 0 1.636-2.871l-8.106-13.534a1.914 1.914 0 0 0-3.274 0z"/><path d="M12 16h.01"/></svg>`;
            } else if (msg.includes("CANH BAO") || msg.includes("CẢNH BÁO")) {
                borderColor = "var(--warning)"; 
                bgColor = "#fffbeb"; 
                textColor = "#9a3412"; 
                type = "Cảnh báo hệ thống";
                // SVG vòng tròn cảnh báo
                iconHtml = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#9a3412" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:middle;margin-right:5px;"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><circle cx="12" cy="12" r="9"/><path d="M12 8v4"/><path d="M12 16h.01"/></svg>`;
            } else if (msg.includes("DA XU LY") || msg.includes("ĐÃ XỬ LÝ")) {
                borderColor = "var(--success)"; 
                bgColor = "#ecfdf5"; 
                textColor = "#047857"; 
                type = "Kiểm kê đã giải quyết";
                // SVG check
                iconHtml = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#047857" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:middle;margin-right:5px;"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M5 12l5 5l10 -10"/></svg>`;
            } else {
                // SVG info
                iconHtml = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:middle;margin-right:5px;"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><circle cx="12" cy="12" r="9"/><path d="M12 8h.01"/><path d="M11 12h1v4h1"/></svg>`;
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
                        <span style="font-size:16px; margin-right:6px; vertical-align:middle;">${iconHtml}</span>
                        <span style="vertical-align:middle;">${msg}</span>
                    </p>
                </div>
            `;
        });
    } else {
        feed.innerHTML = "<div style='color:#64748b; font-size:13px; padding:10px;'>Trạng thái kho hàng ổn định. Không có cảnh báo.</div>";
    }
    // =========================================================================
    // 4. RENDER BẢNG DỮ LIỆU
    // =========================================================================
    fullData = data.inventoryTable;
    // Gọi ngay bộ lọc để vẽ bảng khi nạp xong dữ liệu
    filterTable(); 

    alert("ACTION:LOAD_HISTORY_LIST");
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
            <td>
                <button class="btn-detail" onclick="alert('ACTION:VIEW_${item.id}')">Chi tiết</button>
            </td>
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

document.addEventListener("DOMContentLoaded", function () {
    if (typeof initCharts === "function") initCharts();
    if (typeof initDatePickers === "function") initDatePickers();
});

function initDatePickers() {
    const today = new Date();
    calViewYear  = today.getFullYear();
    calViewMonth = today.getMonth();
    // Mặc định: Đầu tháng đến hôm nay
    calSelStart = new Date(today.getFullYear(), today.getMonth(), 1);
    calSelEnd   = new Date(today);
    calPickStep = 1;
    updateDateRangeLabel();
    renderCalendarGrid();
}

function toggleCalendarPanel() {
    const panel = document.getElementById("inlineCalendarPanel");
    panel.style.display = (panel.style.display === "none") ? "block" : "none";
    if (panel.style.display === "block") renderCalendarGrid();
}

function shiftCalendarMonth(dir) {
    calViewMonth += dir;
    if (calViewMonth > 11) { calViewMonth = 0; calViewYear++; }
    if (calViewMonth < 0)  { calViewMonth = 11; calViewYear--; }
    renderCalendarGrid();
}

function renderCalendarGrid() {
    const monthNames = ["Tháng 1","Tháng 2","Tháng 3","Tháng 4","Tháng 5","Tháng 6","Tháng 7","Tháng 8","Tháng 9","Tháng 10","Tháng 11","Tháng 12"];
    document.getElementById("calMonthYearLabel").innerText = monthNames[calViewMonth] + " " + calViewYear;
    const grid = document.getElementById("calDaysGrid");
    grid.innerHTML = "";

    const today = new Date(); today.setHours(0,0,0,0);
    const firstDayOfMonth = new Date(calViewYear, calViewMonth, 1);
    let startOffset = firstDayOfMonth.getDay() - 1;
    if (startOffset < 0) startOffset = 6;
    const daysInMonth = new Date(calViewYear, calViewMonth + 1, 0).getDate();

    for (let i = 0; i < startOffset; i++) {
        const empty = document.createElement("div");
        empty.className = "cal-day-cell empty";
        grid.appendChild(empty);
    }
    for (let d = 1; d <= daysInMonth; d++) {
        const cellDate = new Date(calViewYear, calViewMonth, d);
        cellDate.setHours(0,0,0,0);
        const cell = document.createElement("div");
        cell.className = "cal-day-cell";
        cell.innerText = d;
        if (cellDate > today) {
            cell.classList.add("disabled");
        } else {
            if (calSelStart && calSelEnd) {
                if (cellDate >= calSelStart && cellDate <= calSelEnd) cell.classList.add("in-range");
            }
            if (calSelStart && cellDate.getTime() === calSelStart.getTime()) cell.classList.add("sel-start");
            if (calSelEnd   && cellDate.getTime() === calSelEnd.getTime())   cell.classList.add("sel-end");
            cell.onclick = () => onCalDayClick(new Date(cellDate));
        }
        grid.appendChild(cell);
    }
    document.getElementById("calSelectionHint").innerText = (calPickStep === 1) ? "Chọn ngày bắt đầu" : "Chọn ngày kết thúc";
}

function onCalDayClick(date) {
    if (calPickStep === 1) {
        calSelStart = date; calSelEnd = null; calPickStep = 2;
    } else {
        if (date < calSelStart) { calSelEnd = calSelStart; calSelStart = date; } 
        else { calSelEnd = date; }
        calPickStep = 1;
    }
    renderCalendarGrid(); updateDateRangeLabel();
}

function updateDateRangeLabel() {
    const label = document.getElementById("dateRangeDisplayLabel");
    if (calSelStart && calSelEnd) {
        label.innerText = fmtDate(calSelStart) + "  →  " + fmtDate(calSelEnd);
    } else if (calSelStart) { label.innerText = fmtDate(calSelStart) + "  →  ..."; } 
    else { label.innerText = "Chọn khoảng thời gian..."; }
}

function fmtDate(d) { return String(d.getDate()).padStart(2,'0') + '/' + String(d.getMonth()+1).padStart(2,'0') + '/' + d.getFullYear(); }
function fmtDateISO(d) { return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0'); }

function confirmDateRange() {
    if (!calSelStart || !calSelEnd) { alert("Vui lòng chọn đủ 2 ngày!"); return; }
    document.getElementById("inlineCalendarPanel").style.display = "none";
    // Bắn ngày qua Java
    alert(`ACTION:DATE_SYNC|${fmtDateISO(calSelStart)}|${fmtDateISO(calSelEnd)}`);
}

document.addEventListener("click", function(event) {
    const calendarPanel = document.getElementById("inlineCalendarPanel");
    const toggleBtn = document.getElementById("calendarToggleBtn");
    if (calendarPanel && calendarPanel.style.display === "block") {
        if (!calendarPanel.contains(event.target) && !toggleBtn.contains(event.target)) {
            calendarPanel.style.display = "none";
        }
    }
});

function exportData() {
    if (!lastRawData) {
        alert("Chưa có dữ liệu để xuất!");
        return;
    }
    // Gửi lệnh kèm theo JSON Data sang Java
    alert("ACTION:EXPORT_KHO|" + JSON.stringify(lastRawData));
}

// Java gọi hàm này để thả danh sách file vào thẻ Dropdown
function populateHistoryDropdown(filesData) {
    const dropdown = document.getElementById("exportHistoryDropdown");
    dropdown.innerHTML = '<option value="">⏳ Lịch sử xuất...</option>';
    
    try {
        // 🔥 GIẢI MÃ: Java giờ truyền xuống 1 Chuỗi an toàn. Ta phải ép nó lại thành Mảng (Array)
        let files = typeof filesData === 'string' ? JSON.parse(filesData) : filesData;
        
        if (files && files.length > 0) {
            dropdown.innerHTML += '<option value="EXIT">🌟 Về thực tại (Live)</option>';
            files.forEach(f => {
                dropdown.innerHTML += `<option value="${f.path}">${f.name}</option>`;
            });
        } else {
            dropdown.innerHTML += '<option value="" disabled>(Chưa có bản lưu nào)</option>';
        }
    } catch (error) {
        console.error("Lỗi parse danh sách file:", error);
        dropdown.innerHTML += '<option value="" disabled>Lỗi đọc dữ liệu</option>';
    }
    
    dropdown.style.display = "inline-block"; 
}
// Khi người dùng chọn 1 file từ Dropdown
function handleHistoryFileSelect(val) {
    if (!val) return; 
    
    if (val === "EXIT") {
        alert("ACTION:EXIT_HISTORY");
        
        // 1. RESET LỊCH VỀ MẶC ĐỊNH
        const today = new Date();
        calSelStart = new Date(today.getFullYear(), today.getMonth(), 1);
        calSelEnd   = new Date(today);
        calViewYear  = today.getFullYear();
        calViewMonth = today.getMonth();
        calPickStep  = 1;
        updateDateRangeLabel(); 
        
        // 2. Trả tiêu đề về bình thường (Xóa chữ Đỏ)
        const caption = document.querySelector(".title-area p");
        if (caption) {
            caption.innerHTML = "Hệ thống phân tích và quản lý kho hàng Real-time";
        }
        document.getElementById("exportHistoryDropdown").selectedIndex = 0;
    } else {
        alert("ACTION:LOAD_HISTORY_FILE|" + val);
    }
}
// Java trả về mã Base64 của file JSON, tiến hành giải mã và vẽ lại Dashboard
function applyHistoricalStateBase64(base64Data, fileName) {
    try {
        // 🔥 ĐỔI TEXT CẢNH BÁO NGAY LẬP TỨC: Giúp sếp thấy ngay trạng thái trước khi dữ liệu kịp vẽ xong
        const caption = document.querySelector(".title-area p");
        if (caption) {
            // Lấy ngày từ tên file
            const match = fileName.match(/ThongKe_(\d{2})-(\d{2})-(\d{4})_den_(\d{2})-(\d{2})-(\d{4})/);
            let dateDisplay = fileName;
            
            if (match) {
                dateDisplay = `${match[1]}/${match[2]}/${match[3]} → ${match[4]}/${match[5]}/${match[6]}`;
                
                calSelStart = new Date(parseInt(match[3]), parseInt(match[2]) - 1, parseInt(match[1]));
                calSelEnd   = new Date(parseInt(match[6]), parseInt(match[5]) - 1, parseInt(match[4]));
                calViewYear  = calSelStart.getFullYear();
                calViewMonth = calSelStart.getMonth();
                updateDateRangeLabel(); 
            }
            
            // 🔥 ĐÃ SỬA LỖI ICON: Dùng icon <i class="ti ti-history"></i> của thư viện thay vì dùng Emoji
            caption.innerHTML = `
                <span style="color: #ef4444; background: #fee2e2; padding: 4px 10px; border-radius: 6px; font-weight: bold; margin-right: 8px; display: inline-flex; align-items: center; gap: 4px;">
                    <i class="ti ti-history" style="font-size: 16px;"></i> CHẾ ĐỘ XEM LỊCH SỬ
                </span> 
                <span style="color: #991b1b; font-weight: 500; vertical-align: middle;">Đang xem dữ liệu: ${dateDisplay}</span>
            `;
        }

        // Giải mã Base64 và Render dữ liệu bảng
        const binaryString = window.atob(base64Data);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }
        const decodedStr = new TextDecoder('utf-8').decode(bytes);
        const data = JSON.parse(decodedStr);
        
        updateDashboard(data); // Vẽ lại Dashboard
        
    } catch (e) {
        console.error("Lỗi parse dữ liệu lịch sử:", e);
        alert("File dữ liệu bị hỏng hoặc không đúng định dạng!");
    }
}