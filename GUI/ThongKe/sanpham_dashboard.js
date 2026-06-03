let masterProductDataset = [];
let filteredProductDataset = [];
let topSellingChartInstance = null;
let categoryDonutChartInstance = null;

let currentTablePageOffset = 0;
const tablePageSizeLimit = 5;

let calViewYear  = new Date().getFullYear();
let calViewMonth = new Date().getMonth(); 
let calSelStart  = null; 
let calSelEnd    = null; 
let calPickStep  = 1;

document.addEventListener("DOMContentLoaded", function () {
    initializeSkeletonViewport();
    initDatePickers();
});

function initDatePickers() {
    const today = new Date();
    calViewYear  = today.getFullYear();
    calViewMonth = today.getMonth();
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

function initializeSkeletonViewport() {
    topSellingChartInstance = new ApexCharts(document.querySelector("#chartTopSellingItems"), {
        series: [{ name: 'Đơn vị giao dịch', data: [0, 0, 0, 0, 0] }],
        chart: { type: 'bar', height: 280, toolbar: { show: false }, fontFamily: 'Inter' },
        plotOptions: { bar: { horizontal: true, barHeight: '50%', borderRadius: 6 } },
        colors: ['#2563eb'],
        xaxis: { categories: ['Đang tải...', 'Đang tải...', 'Đang tải...', 'Đang tải...', 'Đang tải...'] }
    });
    topSellingChartInstance.render();

    categoryDonutChartInstance = new ApexCharts(document.querySelector("#chartCategoryDonut"), {
        series: [1],
        chart: { type: 'donut', height: 280, fontFamily: 'Inter' },
        labels: ['Đang phân tích...'],
        colors: ['#cbd5e1'],
        legend: { position: 'bottom' }
    });
    categoryDonutChartInstance.render();
}

function updateProductDashboard(payload) {
    if (!payload) return;

    document.getElementById("txt-total-products").innerText = payload.totalProducts || 0;
    document.getElementById("txt-total-categories").innerText = (payload.totalCategories || 0) + " danh mục hàng hóa";
    document.getElementById("txt-low-stock-count").innerText = payload.lowStockAlerts || 0;
    document.getElementById("txt-valuation-display").innerText = new Intl.NumberFormat('vi-VN').format(payload.inventoryValuation || 0) + "đ";

    if (payload.topSellingProducts && payload.topSellingProducts.length > 0) {
        document.getElementById("txt-top-product-name").innerText = payload.topSellingProducts[0].name;
        document.getElementById("txt-top-product-name").title = payload.topSellingProducts[0].name;
        document.getElementById("txt-top-product-qty").innerText = (payload.topSellingProducts[0].quantity || 0) + " đơn vị đã chốt";
    }

    if (payload.topSellingProducts) {
        const barNames = payload.topSellingProducts.map(p => p.name);
        const barQuantities = payload.topSellingProducts.map(p => p.quantity);
        topSellingChartInstance.updateOptions({ xaxis: { categories: barNames } });
        topSellingChartInstance.updateSeries([{ name: 'Đơn vị bán lẻ', data: barQuantities }]);
    }

    if (payload.categoryDistribution) {
        const donutLabels = Object.keys(payload.categoryDistribution);
        const donutSeries = Object.values(payload.categoryDistribution);
        categoryDonutChartInstance.updateOptions({
            labels: donutLabels,
            colors: ['#2563eb', '#10b981', '#f59e0b', '#84cc16', '#a855f7', '#ec4899']
        });
        categoryDonutChartInstance.updateSeries(donutSeries);
    }

    const insightContainer = document.getElementById("containerInsightFeed");
    insightContainer.innerHTML = "";
    if (payload.operationalInsights && payload.operationalInsights.length > 0) {
        payload.operationalInsights.forEach(insightStr => {
            insightContainer.innerHTML += `<div>${insightStr}</div>`;
        });
    } else {
        insightContainer.innerHTML = "<div>✨ Trạng thái vận hành ổn định. Chưa ghi nhận biến động bất thường nào.</div>";
    }

    if (payload.productGridMatrix) {
        masterProductDataset = payload.productGridMatrix;
        const catSelector = document.getElementById("categorySelector");
        const detectedCategories = [...new Set(masterProductDataset.map(p => p.category))];
        catSelector.innerHTML = '<option value="ALL">Tất Cả Danh Mục</option>';
        detectedCategories.forEach(catName => {
            catSelector.innerHTML += `<option value="${catName}">${catName}</option>`;
        });
        dispatchFilterCoordinates();
        renderLowStockDetails();
    }

    // Reset caption về live sau khi nhận dữ liệu thật
    var caption = document.querySelector(".header-sub-caption");
    if (caption && !caption.dataset.isHistorical) {
        caption.innerText = "Hệ thống phân tích tồn kho, doanh số và định giá tài sản thời gian thực";
        caption.style.color = "";
        caption.style.fontWeight = "";
    }
}

function dispatchFilterCoordinates() {
    const searchRaw = document.getElementById("internalSearchBox").value;
    const searchVal = loaiBoDauTiengViet(searchRaw);
    const catVal = document.getElementById("categorySelector").value;
    const statusVal = document.getElementById("statusFilterSelector").value;

    filteredProductDataset = masterProductDataset.filter(product => {
        const normalizedName = loaiBoDauTiengViet(product.name);
        const normalizedId = loaiBoDauTiengViet(product.id);
        const matchesSearch = normalizedId.includes(searchVal) || normalizedName.includes(searchVal);
        const matchesCategory = (catVal === "ALL" || product.category === catVal);
        const matchesStatus = (statusVal === "ALL" || product.intelligenceTag === statusVal);
        return matchesSearch && matchesCategory && matchesStatus;
    });

    filteredProductDataset.sort((a, b) => {
        let scoreA = a.stock > 0 ? 0 : 1;
        let scoreB = b.stock > 0 ? 0 : 1;
        return scoreA - scoreB;
    });

    currentTablePageOffset = 0;
    compileVisualCatalogGrid();
    compileVisualInventoryTable();
    if (typeof renderLowStockDetails === "function") renderLowStockDetails();
    updateFilteredKPIs();
    updateFilteredCharts(catVal, statusVal);
}

function compileVisualCatalogGrid() {
    const container = document.getElementById("catalogCardsContainer");
    container.innerHTML = "";
    const maxVisibleGridItems = 8;
    const targets = filteredProductDataset.slice(0, maxVisibleGridItems);

    if (targets.length === 0) {
        container.innerHTML = `<div style="grid-column: 1/-1; padding:40px; text-align:center; color:#64748b; font-size:14px;">
            Không tìm thấy sản phẩm nào phù hợp với bộ lọc hiện tại.
        </div>`;
        return;
    }

    targets.forEach(p => {
        let tagHtml = "";
        if (p.intelligenceTag === "BEST_SELLER") {
            tagHtml = `<span class="card-stock-badge bg-success-light"><span class="dot-icon dot-green"></span>Top Bán Chạy</span>`;
        } else if (p.intelligenceTag === "LOW_STOCK") {
            tagHtml = `<span class="card-stock-badge bg-danger-light"><span class="dot-icon dot-red"></span>Tồn Kho Ít</span>`;
        } else if (p.intelligenceTag === "SLOW_MOVING") {
            tagHtml = `<span class="card-stock-badge bg-warning-light"><span class="dot-icon dot-amber"></span>Bán Chậm</span>`;
        } else {
            tagHtml = `<span class="card-stock-badge bg-success-light"><span class="dot-icon dot-green"></span>Ổn Định</span>`;
        }

        let imageRender = "";
        let frameStyle = "";
        if (p.image && p.image !== "") {
            imageRender = `<img src="${p.image}" style="width:100%; height:100%; object-fit:contain;" alt="${p.name}">`;
            frameStyle = "background: transparent; padding: 32px;";
        } else {
            const displayChar = p.name ? p.name.charAt(0).toUpperCase() : "📦";
            imageRender = displayChar;
        }

        container.innerHTML += `
            <div class="saas-catalog-card">
                <div class="card-image-placeholder-frame" style="${frameStyle}">
                    ${imageRender}
                </div>
                <div class="card-body-content">
                    <div class="card-category-lbl">${p.category}</div>
                    <h4 class="card-product-title" title="${p.name}">${p.name}</h4>
                    <div style="font-size:12px; color:#64748b; margin-bottom:10px;">Mã số: ${p.id}</div>
                    <div class="card-pricing-row">
                        <span class="card-price-display">${new Intl.NumberFormat('vi-VN').format(p.price)}đ</span>
                        ${tagHtml}
                    </div>
                </div>
            </div>
        `;
    });
}

function compileVisualInventoryTable() {
    const tbody = document.getElementById("inventoryTableRowsTarget");
    tbody.innerHTML = "";

    const startIndex = currentTablePageOffset * tablePageSizeLimit;
    const endIndex = startIndex + tablePageSizeLimit;
    const viewPartition = filteredProductDataset.slice(startIndex, endIndex);

    document.getElementById("txtPaginationDisplay").innerText =
        `Hiển thị ${filteredProductDataset.length > 0 ? startIndex + 1 : 0} - ${Math.min(endIndex, filteredProductDataset.length)} của ${filteredProductDataset.length} sản phẩm`;

    if (viewPartition.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#64748b; padding:30px;">Không tìm thấy dữ liệu đối soát phù hợp.</td></tr>`;
        return;
    }

    viewPartition.forEach(p => {
        let labelClass = "bg-success-light";
        let labelText = "Ổn định";
        if (p.intelligenceTag === "LOW_STOCK") { labelClass = "bg-danger-light"; labelText = "Tồn Thấp"; }
        else if (p.intelligenceTag === "BEST_SELLER") { labelClass = "bg-success-light"; labelText = "Best Seller"; }
        else if (p.intelligenceTag === "SLOW_MOVING") { labelClass = "bg-warning-light"; labelText = "Bán Chậm"; }

        tbody.innerHTML += `
            <tr>
                <td style="font-weight:600; color:#2563eb;">${p.id}</td>
                <td>
                    <div style="font-weight:600;">${p.name}</div>
                    <div style="font-size:11px; color:#64748b;">Đơn vị tính: ${p.unit}</div>
                </td>
                <td><span style="font-size:12.5px; font-weight:500;">${p.category}</span></td>
                <td class="align-right" style="font-weight:600;">${new Intl.NumberFormat('vi-VN').format(p.price)}đ</td>
                <td class="align-right" style="font-weight:600; ${p.stock <= 10 ? 'color:#ef4444;' : ''}">${p.stock}</td>
                <td class="align-center">
                    <span class="table-micro-tag ${labelClass}">${labelText}</span>
                </td>
                <td class="align-right">
                    <button class="micro-action-trigger" onclick="handleQuickActionClick('${p.id}', 'VIEW')">Chi tiết</button>
                </td>
            </tr>
        `;
    });
}

function adjustPageOffset(direction) {
    const maxPageCount = Math.ceil(filteredProductDataset.length / tablePageSizeLimit);
    const potentialNextPage = currentTablePageOffset + direction;
    if (potentialNextPage >= 0 && potentialNextPage < maxPageCount) {
        currentTablePageOffset = potentialNextPage;
        compileVisualInventoryTable();
    }
}

function handleQuickActionClick(productId, eventType) {
    if (eventType === 'VIEW') {
        if (window.javaConnector) {
            window.javaConnector.openProductDetail(productId);
        } else {
            alert(`[Chế độ Web] Yêu cầu mở Form Chi Tiết cho mã: ${productId}`);
        }
    }
}

function applyClientSideFilters(category, keyword) {
    document.getElementById("categorySelector").value = category;
    document.getElementById("internalSearchBox").value = keyword;
    dispatchFilterCoordinates();
}

function loaiBoDauTiengViet(str) {
    if (!str) return "";
    str = str.toLowerCase();
    str = str.replace(/à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ/g, "a");
    str = str.replace(/è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ/g, "e");
    str = str.replace(/ì|í|ị|ỉ|ĩ/g, "i");
    str = str.replace(/ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ/g, "o");
    str = str.replace(/ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ/g, "u");
    str = str.replace(/ỳ|ý|ỵ|ỷ|ỹ/g, "y");
    str = str.replace(/đ/g, "d");
    return str.normalize("NFD").replace(/[\u0300-\u036f]/g, "").trim();
}

function renderLowStockDetails() {
    const container = document.getElementById("lowStockDetailsContainer");
    const listTarget = document.getElementById("lowStockListTarget");
    const lowStockItems = masterProductDataset.filter(p => p.intelligenceTag === "LOW_STOCK");

    if (lowStockItems.length > 0) {
        container.style.display = "block";
        listTarget.innerHTML = "";
        lowStockItems.forEach(item => {
            listTarget.innerHTML += `
                <div class="low-stock-item">
                    <div class="low-stock-item-info">
                        <span class="low-stock-item-name" title="${item.name}">${item.name}</span>
                        <span class="low-stock-item-id">${item.id} - ${item.category}</span>
                    </div>
                    <span class="low-stock-item-qty">Tồn: ${item.stock}</span>
                </div>
            `;
        });
    } else {
        container.style.display = "none";
    }
}

function updateFilteredKPIs() {
    document.getElementById("txt-total-products").innerText = filteredProductDataset.length;
    let uniqueCats = new Set(filteredProductDataset.map(p => p.category)).size;
    document.getElementById("txt-total-categories").innerText = uniqueCats + " danh mục hàng hóa";

    let sortedBySales = [...filteredProductDataset].sort((a, b) => (b.unitsSold || 0) - (a.unitsSold || 0));
    if (sortedBySales.length > 0 && sortedBySales[0].unitsSold > 0) {
        document.getElementById("txt-top-product-name").innerText = sortedBySales[0].name;
        document.getElementById("txt-top-product-name").title = sortedBySales[0].name;
        document.getElementById("txt-top-product-qty").innerText = sortedBySales[0].unitsSold + " đơn vị đã chốt";
    } else {
        document.getElementById("txt-top-product-name").innerText = "--";
        document.getElementById("txt-top-product-qty").innerText = "0 đơn vị";
    }

    let lowStockCount = filteredProductDataset.filter(p => p.intelligenceTag === "LOW_STOCK").length;
    document.getElementById("txt-low-stock-count").innerText = lowStockCount;

    let estimatedValue = filteredProductDataset.reduce((sum, p) => sum + (p.price * p.stock), 0);
    document.getElementById("txt-valuation-display").innerText = new Intl.NumberFormat('vi-VN').format(estimatedValue) + "đ";
}

function updateFilteredCharts(selectedCategory, selectedStatus) {
    let sortedBySales = [...filteredProductDataset].sort((a, b) => (b.unitsSold || 0) - (a.unitsSold || 0));
    let top5Sales = sortedBySales.slice(0, 5);
    let barNames = top5Sales.length > 0 ? top5Sales.map(p => p.name) : ["Chưa có dữ liệu"];
    let barQuantities = top5Sales.length > 0 ? top5Sales.map(p => p.unitsSold || 0) : [0];
    topSellingChartInstance.updateOptions({ xaxis: { categories: barNames } });
    topSellingChartInstance.updateSeries([{ name: 'Đơn vị bán lẻ', data: barQuantities }]);

    const donutTitleObj = document.querySelector('.distribution-donut .chart-card-heading');

    if (filteredProductDataset.length === 0) {
        donutTitleObj.innerText = "Không có sản phẩm phù hợp";
        categoryDonutChartInstance.updateOptions({ labels: ['Trống'], colors: ['#f1f5f9'] });
        categoryDonutChartInstance.updateSeries([1]);
        return;
    }

    if (selectedCategory === "ALL" && selectedStatus === "ALL") {
        donutTitleObj.innerText = "Cơ Cấu Lưu Kho Theo Phân Loại";
        let catMap = {};
        filteredProductDataset.forEach(p => {
            catMap[p.category] = (catMap[p.category] || 0) + p.stock;
        });
        categoryDonutChartInstance.updateOptions({
            labels: Object.keys(catMap),
            colors: ['#2563eb', '#10b981', '#f59e0b', '#84cc16', '#a855f7', '#ec4899']
        });
        categoryDonutChartInstance.updateSeries(Object.values(catMap));
    } else {
        let titleParts = [];
        if (selectedCategory !== "ALL") titleParts.push(selectedCategory);
        if (selectedStatus !== "ALL") {
            const statusNames = { "BEST_SELLER": "🔥 Bán Chạy", "LOW_STOCK": "⚠️ Tồn Kho Thấp", "SLOW_MOVING": "🐢 Bán Chậm" };
            titleParts.push(statusNames[selectedStatus] || selectedStatus);
        }
        donutTitleObj.innerText = "Tỷ Trọng Kho: " + titleParts.join(" - ");

        let sortedByStock = [...filteredProductDataset].sort((a, b) => b.stock - a.stock);
        let topStock = sortedByStock.slice(0, 6);
        let others = sortedByStock.slice(6);
        let donutLabels = topStock.map(p => p.name);
        let donutSeries = topStock.map(p => p.stock);
        if (others.length > 0) {
            donutLabels.push("Các SP Khác");
            donutSeries.push(others.reduce((sum, p) => sum + p.stock, 0));
        }
        categoryDonutChartInstance.updateOptions({
            labels: donutLabels,
            colors: ['#2563eb', '#10b981', '#f59e0b', '#84cc16', '#a855f7', '#ec4899', '#cbd5e1']
        });
        categoryDonutChartInstance.updateSeries(donutSeries);
    }
}

// =========================================================================
// XUẤT FILE & LỊCH SỬ THỐNG KÊ
// =========================================================================

function triggerSystemExport() {
    if (typeof window.javaConnector !== 'undefined' && window.javaConnector !== null) {
        try {
            let fileName = "Thong_Ke_San_Pham";
            if (calSelStart && calSelEnd) {
                const s = fmtDate(calSelStart).replace(/\//g, "_");
                const e = fmtDate(calSelEnd).replace(/\//g, "_");
                fileName = `Thong_Ke_San_Pham_Tu_${s}_Den_${e}.json`;
            } else {
                fileName += "_" + new Date().getTime() + ".json";
            }
            window.javaConnector.exportDashboardData(fileName);
        } catch (err) { alert("⚠️ LỖI KHI GỌI JAVA:\n" + err.message); }
    } else { alert("⚠️ CHƯA KẾT NỐI VỚI JAVA BACKEND!"); }
}
function loadExportHistory() {
    if (typeof window.javaConnector === 'undefined' || window.javaConnector === null) return;
    try {
        let historyJson = window.javaConnector.getExportHistoryList();
        let files = JSON.parse(historyJson);
        let selector = document.getElementById("historySelector");
        selector.options.length = 0;

        let defaultOpt = document.createElement("option");
        defaultOpt.value = ""; defaultOpt.textContent = "Lịch sử xuất file";
        selector.appendChild(defaultOpt);

        var caption = document.querySelector(".header-sub-caption");
        if (caption && caption.dataset.isHistorical === "true") {
            let exitOpt = document.createElement("option");
            exitOpt.value = "__EXIT_HISTORY__"; exitOpt.textContent = "Thoát - Quay về hiện tại";
            selector.appendChild(exitOpt);
        }

        files.forEach(function(f) {
            let displayName = f;
            let match = f.match(/Tu_(\d{2})_(\d{2})_(\d{4})_Den_(\d{2})_(\d{2})_(\d{4})/);
            if (match) displayName = `[${match[1]}/${match[2]}/${match[3]} - ${match[4]}/${match[5]}/${match[6]}] Báo Cáo Sản Phẩm`;
            let opt = document.createElement("option");
            opt.value = f; opt.textContent = displayName;
            selector.appendChild(opt);
        });
    } catch(e) { console.error(e); }
}
function loadHistoricalData(fileName) {
    if (!fileName || fileName === "") return;
    if (fileName === "__EXIT_HISTORY__") { exitHistoryMode(); return; }
    if (window.javaConnector) window.javaConnector.readAndLoadExportFile(fileName);
}
function exitHistoryMode() {
    // 1. Reset lịch trên giao diện về mặc định (Đầu tháng -> Hôm nay)
    const today = new Date();
    calSelStart = new Date(today.getFullYear(), today.getMonth(), 1);
    calSelEnd   = new Date(today);
    calViewYear = today.getFullYear(); 
    calViewMonth = today.getMonth(); 
    calPickStep = 1;
    updateDateRangeLabel();

    // 2. Khôi phục lại tiêu đề mặc định
    var caption = document.querySelector(".header-sub-caption");
    if (caption) {
        delete caption.dataset.isHistorical;
        caption.innerText = "Quản lý nâng cao và phân tích trí tuệ hiệu suất sản phẩm hàng hóa";
        caption.style.color = ""; 
        caption.style.fontWeight = "";
    }
    
    // 3. Reset Dropdown
    let selector = document.getElementById("historySelector");
    if (selector) selector.value = "";
    loadExportHistory();

    // 🔥 4. SỬA TẠI ĐÂY: Gửi ngày mặc định xuống Java để ép lọc lại dữ liệu
    let startStr = fmtDateISO(calSelStart);
    let endStr = fmtDateISO(calSelEnd);
    alert(`ACTION:DATE_SYNC|${startStr}|${endStr}`);
}

function applyHistoricalStateBase64(base64Data, fileName) {
    try {
        let binaryStr = atob(base64Data);
        let bytes = new Uint8Array(binaryStr.length);
        for (let i = 0; i < binaryStr.length; i++) bytes[i] = binaryStr.charCodeAt(i);
        let decodedString = new TextDecoder('utf-8').decode(bytes);
        
        let historicalPayload = JSON.parse(decodedString);
        updateProductDashboard(historicalPayload);
        
        let dateDisplay = fileName;
        let match = fileName.match(/Tu_(\d{2})_(\d{2})_(\d{4})_Den_(\d{2})_(\d{2})_(\d{4})/);
        if (match) {
            calSelStart = new Date(parseInt(match[3]), parseInt(match[2]) - 1, parseInt(match[1]));
            calSelEnd   = new Date(parseInt(match[6]), parseInt(match[5]) - 1, parseInt(match[4]));
            calViewYear = calSelStart.getFullYear(); calViewMonth = calSelStart.getMonth(); calPickStep = 1;
            updateDateRangeLabel();
            dateDisplay = `${match[1]}/${match[2]}/${match[3]} → ${match[4]}/${match[5]}/${match[6]}`;
        }

        var caption = document.querySelector(".header-sub-caption");
        if (caption) {
            caption.dataset.isHistorical = "true";
            caption.textContent = "[CHẾ ĐỘ LỊCH SỬ] Báo cáo: " + dateDisplay + " -- Chọn 'Thoát' để về hiện tại";
            caption.style.color = "#ef4444"; caption.style.fontWeight = "bold";
        }
        loadExportHistory();
    } catch(e) { alert("❌ Lỗi giải mã file lịch sử!"); }
}