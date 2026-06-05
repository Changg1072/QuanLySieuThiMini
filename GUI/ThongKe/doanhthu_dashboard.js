let salesChart = null; let paymentChart = null;
let calViewYear, calViewMonth, calSelStart, calSelEnd, calPickStep;

document.addEventListener("DOMContentLoaded", function () { 
    initializeCharts(); 
    initDatePickers(); 
});
function initializeCharts() {
    salesChart = new ApexCharts(document.querySelector("#salesLinearChart"), {
        // 🔥 ĐÃ THÊM 2 ĐƯỜNG DỮ LIỆU: DOANH THU & LỢI NHUẬN
        series: [
            { name: 'Doanh Thu', data: [0,0,0,0,0,0,0,0,0,0,0,0] },
            { name: 'Lợi Nhuận', data: [0,0,0,0,0,0,0,0,0,0,0,0] }
        ],
        chart: { type: 'area', height: 330, toolbar: { show: false }, fontFamily: 'Inter' },
        // Xanh dương cho Doanh thu, Xanh ngọc cho Lợi nhuận
        colors: ['#2563eb', '#10b981'], 
        fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.05, stops: [0, 100] } },
        dataLabels: { enabled: false },
        stroke: { curve: 'smooth', width: 2 },
        xaxis: { categories: ['Thg 1','Thg 2','Thg 3','Thg 4','Thg 5','Thg 6','Thg 7','Thg 8','Thg 9','Thg 10','Thg 11','Thg 12'] },
        yaxis: { labels: { formatter: val => (val / 1000000).toFixed(1) + "M" } },
        // Format tiền tệ khi hover chuột
        tooltip: { y: { formatter: function (val) { return new Intl.NumberFormat('vi-VN').format(val) + "đ" } } }
    });
    salesChart.render();

    paymentChart = new ApexCharts(document.querySelector("#paymentDistributionChart"), {
        series: [40, 40, 20], chart: { type: 'donut', height: 330, fontFamily: 'Inter' },
        labels: ['Tiền Mặt', 'Chuyển Khoản', 'Ví Điện Tử'], colors: ['#f59e0b', '#0891b2', '#be185d'],
        legend: { position: 'bottom' }
    });
    paymentChart.render();
}
function updateDashboard(data) {
    if (!data) return;

    // 1. CẬP NHẬT KPI CHÍNH
    if (document.getElementById("val-revenue")) document.getElementById("val-revenue").innerText = new Intl.NumberFormat('vi-VN').format(data.totalRevenue || 0) + "đ";
    if (document.getElementById("val-invoices")) document.getElementById("val-invoices").innerText = new Intl.NumberFormat('vi-VN').format(data.totalInvoices || 0);
    if (document.getElementById("val-peak-hour")) {
        document.getElementById("val-peak-hour").innerText = data.peakHour || "--:--";
        document.getElementById("val-peak-count").innerText = (data.peakOrderCount || 0) + " giao dịch đỉnh điểm";
    }
        
    // 2. CÁC CHỈ SỐ CHI PHÍ & LỢI NHUẬN
    if (document.getElementById("val-cogs")) document.getElementById("val-cogs").innerText = new Intl.NumberFormat('vi-VN').format(data.totalCOGS || 0) + "đ";
    if (document.getElementById("val-salary")) document.getElementById("val-salary").innerText = new Intl.NumberFormat('vi-VN').format(data.totalSalary || 0) + "đ";
    if (document.getElementById("val-loss")) document.getElementById("val-loss").innerText = new Intl.NumberFormat('vi-VN').format(data.totalLoss || 0) + "đ";
    if (document.getElementById("val-actual-profit")) document.getElementById("val-actual-profit").innerText = new Intl.NumberFormat('vi-VN').format(data.actualProfit || 0) + "đ";
    if (document.getElementById("val-import-cost")) document.getElementById("val-import-cost").innerText = new Intl.NumberFormat('vi-VN').format(data.totalImportCost || 0) + "đ";

    // =========================================================
    // 🔥 3. CẬP NHẬT BIỂU ĐỒ (CHARTS KÉP)
    // =========================================================
    if (salesChart) {
        if (data.chronologicalSales && data.chronologicalProfit) {
            salesChart.updateSeries([
                { name: 'Doanh Thu', data: data.chronologicalSales },
                { name: 'Lợi Nhuận', data: data.chronologicalProfit }
            ]);
        }
        if (data.chronologicalLabels) {
            salesChart.updateOptions({ xaxis: { categories: data.chronologicalLabels } });
        }
    }
    
    if (paymentChart && data.paymentMethods) {
        paymentChart.updateSeries([
            data.paymentMethods.cash || 0, 
            data.paymentMethods.bankTransfer || 0, 
            data.paymentMethods.eWallet || 0
        ]);
    }

    // 4. TOP SẢN PHẨM & KHÁCH HÀNG (Giữ nguyên)
    const prodCont = document.getElementById("top-products-list"); 
    if (prodCont) {
        prodCont.innerHTML = "";
        (data.topProducts || []).forEach((p, i) => {
            prodCont.innerHTML += `<div class="leader-row"><div style="display:flex;"><div class="avatar-mock">${i+1}</div><div><b>${p.name}</b><div style="font-size:11px;color:#64748b;">Đã bán: ${p.unitsSold}</div></div></div></div>`;
        });
    }

    const custCont = document.getElementById("top-customers-list"); 
    if (custCont) {
        custCont.innerHTML = "";
        (data.topCustomers || []).forEach(c => {
            custCont.innerHTML += `<div class="leader-row"><div style="display:flex;"><div class="avatar-mock" style="background:#ede9fe;color:#8b5cf6;">${c.name.charAt(0)}</div><div><b>${c.name}</b></div></div><div style="font-weight:600;color:#8b5cf6;">${new Intl.NumberFormat('vi-VN').format(c.totalSpent)}đ</div></div>`;
        });
    }

    // 5. NHẬT KÝ HÓA ĐƠN GẦN ĐÂY
    const tb = document.getElementById("invoice-rows-container"); 
    if (tb) {
        tb.innerHTML = "";
        (data.recentInvoices || []).forEach(inv => {
            tb.innerHTML += `
                <tr>
                    <td style="color:#2563eb;font-weight:600;">${inv.id}</td>
                    <td>${inv.customer}</td>
                    <td>${inv.cashier}</td>
                    <td style="color:#64748b;">${inv.timestamp}</td>
                    <td>${inv.method}</td>
                    <td style="font-weight:600;">${new Intl.NumberFormat('vi-VN').format(inv.amount)}đ</td>
                    <td style="text-align:center;">
                        <button class="btn-detail" onclick="alert('ACTION:VIEW_INVOICE|${inv.id}')">Chi tiết</button>
                    </td>
                </tr>
            `;
        });
    }
}

// ==========================================
// LOGIC LỊCH (ĐỒNG BỘ TỪ KHÁCH HÀNG)
// ==========================================
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
        const empty = document.createElement("div"); empty.className = "cal-day-cell empty"; grid.appendChild(empty);
    }
    for (let d = 1; d <= daysInMonth; d++) {
        const cellDate = new Date(calViewYear, calViewMonth, d);
        cellDate.setHours(0,0,0,0);
        const cell = document.createElement("div"); cell.className = "cal-day-cell"; cell.innerText = d;
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
    if (calPickStep === 1) { calSelStart = date; calSelEnd = null; calPickStep = 2; } 
    else {
        if (date < calSelStart) { calSelEnd = calSelStart; calSelStart = date; } else { calSelEnd = date; }
        calPickStep = 1;
    }
    renderCalendarGrid(); updateDateRangeLabel();
}

function updateDateRangeLabel() {
    const label = document.getElementById("dateRangeDisplayLabel");
    if (calSelStart && calSelEnd) {
        label.innerText = fmtDate(calSelStart) + "  →  " + fmtDate(calSelEnd);
    } else if (calSelStart) { label.innerText = fmtDate(calSelStart) + "  →  ..."; } 
}

function fmtDate(d) { return String(d.getDate()).padStart(2,'0') + '/' + String(d.getMonth()+1).padStart(2,'0') + '/' + d.getFullYear(); }
function fmtDateISO(d) { return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0'); }

// 🔥 BẤM ÁP DỤNG TRUYỀN NGÀY VỀ JAVA
function confirmDateRange() {
    if (!calSelStart || !calSelEnd) { alert("Vui lòng chọn đủ 2 ngày!"); return; }
    document.getElementById("inlineCalendarPanel").style.display = "none";
    alert(`ACTION:DATE_SYNC|${fmtDateISO(calSelStart)}|${fmtDateISO(calSelEnd)}`);
}

// Bấm ra ngoài đóng lịch
document.addEventListener("click", function(event) {
    const calendarPanel = document.getElementById("inlineCalendarPanel");
    const toggleBtn = document.getElementById("calendarToggleBtn");
    if (calendarPanel && calendarPanel.style.display === "block" && !calendarPanel.contains(event.target) && !toggleBtn.contains(event.target)) {
        calendarPanel.style.display = "none";
    }
});