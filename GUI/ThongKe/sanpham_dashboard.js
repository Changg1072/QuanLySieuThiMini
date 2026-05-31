let masterProductDataset = [];
let filteredProductDataset = [];
let topSellingChartInstance = null;
let categoryDonutChartInstance = null;

let currentTablePageOffset = 0;
const tablePageSizeLimit = 5;

document.addEventListener("DOMContentLoaded", function () {
    initializeSkeletonViewport();
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
            window.javaConnector.exportDashboardData();
        } catch (err) {
            alert("⚠️ Lỗi khi gọi Java:\n" + err.message);
        }
    } else {
        alert("⚠️ Chưa kết nối với Java backend!\nHãy đợi trang load xong rồi thử lại.");
    }
}

/**
 * Load danh sách file lịch sử vào dropdown.
 * Được Java gọi sau khi trang load và sau mỗi lần xuất file.
 */
function loadExportHistory() {
    if (typeof window.javaConnector === 'undefined' || window.javaConnector === null) return;

    try {
        let historyJson = window.javaConnector.getExportHistoryList();
        let files = JSON.parse(historyJson);

        let selector = document.getElementById("historySelector");
        // ✅ FIX ENCODING: Dùng createElement + textContent hoàn toàn,
        //    KHÔNG dùng innerHTML để tránh lỗi UTF-8 tiếng Việt trong JavaFX WebView
        selector.options.length = 0; // Xóa sạch tất cả option cũ

        let defaultOpt = document.createElement("option");
        defaultOpt.value = "";
        defaultOpt.textContent = "Lịch sử xuất file"; // ASCII thuần — tránh lỗi JavaFX
        selector.appendChild(defaultOpt);

        // Thêm nút thoát lịch sử — chỉ hiện khi đang ở chế độ xem lịch sử
        var caption = document.querySelector(".header-sub-caption");
        if (caption && caption.dataset.isHistorical === "true") {
            let exitOpt = document.createElement("option");
            exitOpt.value = "__EXIT_HISTORY__";
            exitOpt.textContent = "Thoát - Quay về dữ liệu thực";
            selector.appendChild(exitOpt);
        }

        files.forEach(function(f) {
            let displayName = f;
            let match = f.match(/Thongke_(\d{2})_(\d{2})_(\d{4})\.json/);
            if (match) {
                // ASCII thuần, không dấu tiếng Việt
                displayName = "[" + match[1] + "/" + match[2] + "/" + match[3] + "] Bao cao Kho";
            }
            let opt = document.createElement("option");
            opt.value = f;
            opt.textContent = displayName;
            selector.appendChild(opt);
        });
    } catch(e) {
        console.error("loadExportHistory lỗi:", e);
    }
}

/**
 * Khi chọn file từ dropdown — xử lý cả lệnh thoát lịch sử.
 */
function loadHistoricalData(fileName) {
    if (!fileName || fileName === "") return;

    // ✅ Xử lý thoát chế độ lịch sử
    if (fileName === "__EXIT_HISTORY__") {
        exitHistoryMode();
        return;
    }

    if (typeof window.javaConnector === 'undefined' || window.javaConnector === null) {
        alert("⚠️ Chưa kết nối với Java. Hãy đợi trang load xong.");
        return;
    }
    try {
        window.javaConnector.readAndLoadExportFile(fileName);
    } catch(e) {
        alert("⚠️ Lỗi tải file lịch sử: " + e.message);
    }
}

/**
 * Thoát chế độ xem lịch sử, yêu cầu Java đồng bộ lại dữ liệu thực.
 */
function exitHistoryMode() {
    // 1. Xóa flag lịch sử và reset caption
    var caption = document.querySelector(".header-sub-caption");
    if (caption) {
        delete caption.dataset.isHistorical;
        caption.textContent = "He thong phan tich ton kho, doanh so va dinh gia tai san thoi gian thuc";
        caption.style.color = "";
        caption.style.fontWeight = "";
    }

    // 2. Reset dropdown về mặc định, rebuild (nút thoát tự biến mất)
    var selector = document.getElementById("historySelector");
    if (selector) selector.value = "";
    loadExportHistory();

    // 3. Kích hoạt đồng bộ dữ liệu thực từ Java
    alert("ACTION:SYNC");
}

/**
 * Java gọi hàm này sau khi đọc file xong, truyền nội dung dạng base64.
 * Dùng TextDecoder để giải mã UTF-8 tiếng Việt chuẩn xác.
 */
function applyHistoricalStateBase64(base64Data, fileName) {
    try {
        // ✅ Giải mã UTF-8 đúng chuẩn — hỗ trợ đầy đủ tiếng Việt
        let binaryStr = atob(base64Data);
        let bytes = new Uint8Array(binaryStr.length);
        for (let i = 0; i < binaryStr.length; i++) {
            bytes[i] = binaryStr.charCodeAt(i);
        }
        let decodedString = new TextDecoder('utf-8').decode(bytes);
        let historicalPayload = JSON.parse(decodedString);

        // Vẽ lại toàn bộ dashboard bằng dữ liệu quá khứ
        updateProductDashboard(historicalPayload);

        // ✅ Đánh dấu đang ở chế độ lịch sử
        var caption = document.querySelector(".header-sub-caption");
        if (caption) {
            caption.dataset.isHistorical = "true";
            let dateDisplay = fileName;
            let match = fileName.match(/Thongke_(\d{2})_(\d{2})_(\d{4})/);
            if (match) dateDisplay = match[1] + "/" + match[2] + "/" + match[3];

        caption.textContent = "[CHE DO LICH SU] Bao cao ngay: " + dateDisplay +
                              " -- Chon 'Thoat' trong dropdown de quay ve du lieu thuc";
            caption.style.color = "#ef4444";
            caption.style.fontWeight = "bold";
        }

        // ✅ Rebuild dropdown để hiện nút "↩ Thoát"
        loadExportHistory();

    } catch(e) {
        alert("❌ Lỗi giải mã file lịch sử: " + e.message);
        console.error("applyHistoricalStateBase64 error:", e);
    }
}