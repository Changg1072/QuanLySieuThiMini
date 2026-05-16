let salesChart = null; let paymentChart = null;
document.addEventListener("DOMContentLoaded", function () { initializeCharts(); });

function initializeCharts() {
    salesChart = new ApexCharts(document.querySelector("#salesLinearChart"), {
        series: [{ name: 'Doanh Thu Luỹ Tiến', data: [0,0,0,0,0,0,0,0,0,0,0,0] }],
        chart: { type: 'area', height: 330, toolbar: { show: false }, fontFamily: 'Inter' },
        colors: ['#2563eb'], fill: { type: 'gradient' },
        xaxis: { categories: ['Thg 1','Thg 2','Thg 3','Thg 4','Thg 5','Thg 6','Thg 7','Thg 8','Thg 9','Thg 10','Thg 11','Thg 12'] },
        yaxis: { labels: { formatter: val => (val / 1000000).toFixed(1) + "M" } }
    });
    salesChart.render();

    paymentChart = new ApexCharts(document.querySelector("#paymentDistributionChart"), {
        series: [40, 40, 20], chart: { type: 'donut', height: 330, fontFamily: 'Inter' },
        labels: ['Tiền Mặt', 'Chuyển Khoản', 'Ví Điện Tử'], colors: ['#f59e0b', '#0891b2', '#be185d']
    });
    paymentChart.render();
}

function updateDashboard(data) {
    if (!data) return;

    // 1. CẬP NHẬT CÁC CON SỐ TỔNG (KPI CHÍNH)
    document.getElementById("val-revenue").innerText = new Intl.NumberFormat('vi-VN').format(data.totalRevenue || 0) + "đ";
    document.getElementById("val-invoices").innerText = new Intl.NumberFormat('vi-VN').format(data.totalInvoices || 0);
    document.getElementById("val-profit").innerText = new Intl.NumberFormat('vi-VN').format(data.totalProfit || 0) + "đ";
    if(document.getElementById("val-loss")) {
        document.getElementById("val-loss").innerText = "-" + new Intl.NumberFormat('vi-VN').format(data.totalLoss || 0) + "đ";
    }
    // =========================================================================
    // 🔥 2. CẬP NHẬT CÁC CHỈ SỐ PHỤ MỚI THÊM (TRUNG BÌNH, GIỜ CAO ĐIỂM, MARGIN)
    // =========================================================================
    
    // Cập nhật Tiền trung bình / đơn
    document.getElementById("val-avg-ticket").innerText = "Trung bình: " + new Intl.NumberFormat('vi-VN').format(data.averageTicket || 0) + "đ / đơn";
    
    // Cập nhật Giờ cao điểm
    document.getElementById("val-peak-hour").innerText = data.peakHour || "--:--";
    document.getElementById("val-peak-count").innerText = (data.peakOrderCount || 0) + " giao dịch đỉnh điểm";

    // Cập nhật Margin (Biên độ lợi nhuận) và tự động đổi màu
    let marginEl = document.getElementById("val-margin");
    let marginVal = data.profitMargin || 0;
    marginEl.innerText = "Margin: " + marginVal + "%";
    
    if (marginVal > 0) {
        marginEl.className = "trend-badge trend-up";
        marginEl.style.color = "#10b981";       // Màu xanh ngọc (Lãi)
        marginEl.style.background = "#d1fae5";
    } else if (marginVal < 0) {
        marginEl.className = "trend-badge trend-down";
        marginEl.style.color = "#ef4444";       // Màu đỏ (Lỗ)
        marginEl.style.background = "#fee2e2";
    } else {
        marginEl.className = "trend-badge trend-neutral";
        marginEl.style.color = "#64748b";       // Màu xám (Hòa vốn hoặc 0)
        marginEl.style.background = "#f1f5f9";
    }
    // =========================================================================

    // 3. CẬP NHẬT BIỂU ĐỒ (CHARTS)
    if (data.chronologicalSales) salesChart.updateSeries([{ data: data.chronologicalSales }]);
    if (data.paymentMethods) paymentChart.updateSeries([data.paymentMethods.cash, data.paymentMethods.bankTransfer, data.paymentMethods.eWallet]);

    // 4. CẬP NHẬT BẢNG TOP SẢN PHẨM BÁN CHẠY
    const prodCont = document.getElementById("top-products-list"); prodCont.innerHTML = "";
    (data.topProducts || []).forEach((p, i) => {
        prodCont.innerHTML += `<div class="leader-row"><div style="display:flex;"><div class="avatar-mock">${i+1}</div><div><b>${p.name}</b><div style="font-size:11px;color:#64748b;">Đã bán: ${p.unitsSold}</div></div></div></div>`;
    });

    // 5. CẬP NHẬT BẢNG TOP KHÁCH HÀNG VIP
    const custCont = document.getElementById("top-customers-list"); custCont.innerHTML = "";
    (data.topCustomers || []).forEach(c => {
        custCont.innerHTML += `<div class="leader-row"><div style="display:flex;"><div class="avatar-mock" style="background:#ede9fe;color:#8b5cf6;">${c.name.charAt(0)}</div><div><b>${c.name}</b></div></div><div style="font-weight:600;color:#8b5cf6;">${new Intl.NumberFormat('vi-VN').format(c.totalSpent)}đ</div></div>`;
    });

    // 6. CẬP NHẬT BẢNG NHẬT KÝ HÓA ĐƠN GẦN ĐÂY
    const tb = document.getElementById("invoice-rows-container"); tb.innerHTML = "";
    (data.recentInvoices || []).forEach(inv => {
        tb.innerHTML += `<tr><td style="color:#2563eb;font-weight:600;">${inv.id}</td><td>${inv.customer}</td><td>${inv.cashier}</td><td style="color:#64748b;">${inv.timestamp}</td><td>${inv.method}</td><td style="text-align:right;font-weight:600;">${new Intl.NumberFormat('vi-VN').format(inv.amount)}đ</td></tr>`;
    });
}