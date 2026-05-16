let chronologicalAnnualSalesChartInstance = null;

document.addEventListener("DOMContentLoaded", function () {
    preInitializeLinearMetricsGraph();
});

function preInitializeLinearMetricsGraph() {
    chronologicalAnnualSalesChartInstance = new ApexCharts(document.querySelector("#overviewTrendLinearChart"), {
        series: [{ name: 'Doanh thu tiến độ', data: [0,0,0,0,0,0,0,0,0,0,0,0] }],
        chart: { type: 'area', height: 280, toolbar: { show: false }, fontFamily: 'Inter' },
        colors: ['#2563eb'],
        stroke: { width: 3, curve: 'smooth' },
        fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.3, opacityTo: 0.02 } },
        xaxis: { categories: ['Thg 1','Thg 2','Thg 3','Thg 4','Thg 5','Thg 6','Thg 7','Thg 8','Thg 9','Thg 10','Thg 11','Thg 12'] },
        yaxis: { labels: { formatter: val => (val / 1000000).toFixed(1) + "M" } },
        dataLabels: { enabled: false }
    });
    chronologicalAnnualSalesChartInstance.render();
}

function updateGlobalControlCenter(metaPackage) {
    if (!metaPackage) return;

    const targetTabId = metaPackage.currentTab || "OVERVIEW";
    
    document.querySelectorAll(".navigation-menu-item").forEach(node => node.classList.remove("active"));
    const activeMenuNode = document.getElementById("menu-" + targetTabId);
    if (activeMenuNode) activeMenuNode.classList.add("active");

    const overviewPanel = document.getElementById("panel-OVERVIEW");
    const fallbackPanel = document.getElementById("panel-FALLBACK_CONTAINER");
    const breadcrumbLabel = document.getElementById("activeBreadcrumbLabel");

    if (targetTabId === "OVERVIEW") {
        overviewPanel.className = "sub-dashboard-view-panel visible-view-state";
        fallbackPanel.className = "sub-dashboard-view-panel hidden-view-state";
        breadcrumbLabel.innerText = "Tổng Quan Hệ Thống";
    } else {
        overviewPanel.className = "sub-dashboard-view-panel hidden-view-state";
        fallbackPanel.className = "sub-dashboard-view-panel visible-view-state";
        
        let labelTranslation = "Trí Tuệ Khách Hàng";
        if (targetTabId === "REVENUE") labelTranslation = "Phân Khúc Chỉ Số Doanh Thu";
        else if (targetTabId === "WAREHOUSE") labelTranslation = "Quản Trị Kiểm Kê Kho Hàng";
        else if (targetTabId === "STAFF") labelTranslation = "Phân Tích Ca Kíp & Nhân Sự";
        else if (targetTabId === "PRODUCT") labelTranslation = "Hiệu Suất Phân Phối Sản Phẩm";
        breadcrumbLabel.innerText = labelTranslation;
    }

    document.getElementById("filterMonthBox").value = metaPackage.filterMonth;
    document.getElementById("filterYearBox").value = metaPackage.filterYear;
    document.getElementById("filterCategoryBox").value = metaPackage.filterCategory;

    document.getElementById("lbl-total-revenue").innerText = new Intl.NumberFormat('vi-VN').format(metaPackage.totalRevenue || 0) + "đ";
    document.getElementById("lbl-net-profits").innerText = new Intl.NumberFormat('vi-VN').format(metaPackage.netProfits || 0) + "đ";
    document.getElementById("lbl-total-invoices").innerText = new Intl.NumberFormat('vi-VN').format(metaPackage.totalInvoices || 0);
    document.getElementById("lbl-total-losses").innerText = new Intl.NumberFormat('vi-VN').format(metaPackage.totalLosses || 0) + "đ";
    document.getElementById("lbl-low-stock-count").innerText = `⚠️ Có ${metaPackage.lowStockAlertsCount || 0} mặt hàng chạm ngưỡng tồn an toàn`;

    if (metaPackage.annualRevenueTrend) {
        chronologicalAnnualSalesChartInstance.updateSeries([{ name: 'Doanh Thu Tháng', data: metaPackage.annualRevenueTrend }]);
    }

    const productsBox = document.getElementById("targetTopProductsList");
    productsBox.innerHTML = "";
    if (metaPackage.topMovingProducts && metaPackage.topMovingProducts.length > 0) {
        metaPackage.topMovingProducts.forEach((item, idx) => {
            productsBox.innerHTML += `
                <div class="ranking-item-row">
                    <div class="rank-cell" style="width:12%; font-weight:700; color:var(--text-slate-muted);">#${idx+1}</div>
                    <div class="rank-cell" style="width:63%; font-weight:600;">${item.name}</div>
                    <div class="rank-cell" style="width:25%; text-align:right; font-weight:700; color:var(--primary-blue);">${item.value} đơn vị</div>
                </div>
            `;
        });
    } else {
        productsBox.innerHTML = '<div style="color:var(--text-slate-muted); font-size:12.5px; padding:15px; text-align:center;">Chưa phát sinh giao dịch xuất kho mặt hàng nào.</div>';
    }

    const customersBox = document.getElementById("targetTopLoyalCustomersList");
    customersBox.innerHTML = "";
    if (metaPackage.topLoyalCustomers && metaPackage.topLoyalCustomers.length > 0) {
        metaPackage.topLoyalCustomers.forEach((item, idx) => {
            customersBox.innerHTML += `
                <div class="ranking-item-row">
                    <div class="rank-cell" style="width:12%; font-weight:700; color:var(--text-slate-muted);">#${idx+1}</div>
                    <div class="rank-cell" style="width:53%; font-weight:600;">${item.name}</div>
                    <div class="rank-cell" style="width:35%; text-align:right; font-weight:700; color:var(--emerald-success);">${new Intl.NumberFormat('vi-VN').format(item.spent)}đ</div>
                </div>
            `;
        });
    } else {
        customersBox.innerHTML = '<div style="color:var(--text-slate-muted); font-size:12.5px; padding:15px; text-align:center;">Chưa phát sinh doanh thu thành viên VIP kỳ này.</div>';
    }

    const aiContainer = document.getElementById("floatingAiInsightsContainer");
    aiContainer.innerHTML = "";
    if (metaPackage.intelligenceInsights && metaPackage.intelligenceInsights.length > 0) {
        metaPackage.intelligenceInsights.forEach(txt => {
            aiContainer.innerHTML += `<p style="margin:0 0 6px 0; font-size:12.5px; line-height:1.4; color:#334155;">💡 ${txt}</p>`;
        });
    } else {
        let message = "Hệ thống vận hành ổn định. Lợi nhuận gộp chi nhánh đang giữ đà tăng trưởng dương.";
        if (metaPackage.lowStockAlertsCount > 5) {
            message = `Phát hiện ${metaPackage.lowStockAlertsCount} cảnh báo cạn kho. Ban quản trị nên xem xét mở lệnh nhập hàng siêu tốc lập tức.`;
        }
        aiContainer.innerHTML = `<p style="margin:0; font-size:12.5px; line-height:1.4; color:#334155;">🚀 ${message}</p>`;
    }
}

function switchControlViewTab(tabKeyName) {
    document.querySelectorAll(".navigation-menu-item").forEach(node => node.classList.remove("active"));
    const activeMenuNode = document.getElementById("menu-" + tabKeyName);
    if (activeMenuNode) activeMenuNode.classList.add("active");
    
    const overviewPanel = document.getElementById("panel-OVERVIEW");
    const fallbackPanel = document.getElementById("panel-FALLBACK_CONTAINER");
    
    if (tabKeyName === "OVERVIEW") {
        overviewPanel.className = "sub-dashboard-view-panel visible-view-state";
        fallbackPanel.className = "sub-dashboard-view-panel hidden-view-state";
    } else {
        overviewPanel.className = "sub-dashboard-view-panel hidden-view-state";
        fallbackPanel.className = "sub-dashboard-view-panel visible-view-state";
    }

    alert("TAB_SWITCH:" + tabKeyName);
}

function propagateFilterChanges() {
    const m = document.getElementById("filterMonthBox").value;
    const y = document.getElementById("filterYearBox").value;
    const c = document.getElementById("filterCategoryBox").value;
    alert(`FILTER_CHANGE:month=${m}&year=${y}&category=${c}`);
}