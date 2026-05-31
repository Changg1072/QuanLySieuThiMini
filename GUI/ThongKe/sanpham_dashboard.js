let masterProductDataset = [];
let filteredProductDataset = [];
let topSellingChartInstance = null;
let categoryDonutChartInstance = null;

// Pagination boundaries
let currentTablePageOffset = 0;
const tablePageSizeLimit = 5;

document.addEventListener("DOMContentLoaded", function () {
    initializeSkeletonViewport();
});

function initializeSkeletonViewport() {
    // Initial ApexCharts rendering shells with fallback static configurations
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

/**
 * CORE BOUNDARY HOOK INJECTED FROM JAVA SWING ENGINE VIA WEBKIT PLATFORM
 */
function updateProductDashboard(payload) {
    if (!payload) return;

    // 1. Assign values to Counter Metrics Layer
    document.getElementById("txt-total-products").innerText = payload.totalProducts || 0;
    document.getElementById("txt-total-categories").innerText = (payload.totalCategories || 0) + " danh mục hàng hóa";
    document.getElementById("txt-low-stock-count").innerText = payload.lowStockAlerts || 0;
    document.getElementById("txt-valuation-display").innerText = new Intl.NumberFormat('vi-VN').format(payload.inventoryValuation || 0) + "đ";

    // 2. Parse top performer names smoothly
    if (payload.topSellingProducts && payload.topSellingProducts.length > 0) {
        document.getElementById("txt-top-product-name").innerText = payload.topSellingProducts[0].name;
        document.getElementById("txt-top-product-name").title = payload.topSellingProducts[0].name;
        document.getElementById("txt-top-product-qty").innerText = (payload.topSellingProducts[0].quantity || 0) + " đơn vị đã chốt";
    }

    // 3. Update Chart vectors asynchronously
    if (payload.topSellingProducts) {
        const barNames = payload.topSellingProducts.map(p => p.name);
        const barQuantities = payload.topSellingProducts.map(p => p.quantity);
        topSellingChartInstance.updateOptions({
            xaxis: { categories: barNames }
        });
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

    // 4. Fill Intelligent Insight text nodes
    const insightContainer = document.getElementById("containerInsightFeed");
    insightContainer.innerHTML = "";
    if (payload.operationalInsights && payload.operationalInsights.length > 0) {
        payload.operationalInsights.forEach(insightStr => {
            insightContainer.innerHTML += `<div>${insightStr}</div>`;
        });
    } else {
        insightContainer.innerHTML = "<div>✨ Trạng thái vận hành ổn định. Chưa ghi nhận biến động bất thường nào.</div>";
    }

    // 5. Store data locally into RAM arrays for handling ultra-fast filtering without roundtrips
    if (payload.productGridMatrix) {
        masterProductDataset = payload.productGridMatrix;
        
        // Dynamically compile categories filter options dropdown values
        const catSelector = document.getElementById("categorySelector");
        const detectedCategories = [...new Set(masterProductDataset.map(p => p.category))];
        
        // Wipe extra historical records except the ALL frame option
        catSelector.innerHTML = '<option value="ALL">Tất Cả Danh Mục</option>';
        detectedCategories.forEach(catName => {
            catSelector.innerHTML += `<option value="${catName}">${catName}</option>`;
        });

        dispatchFilterCoordinates();
        renderLowStockDetails();
    }
}

/**
 * EVALUATES COMPREHENSIVE COMBINATORIAL CRITERIA ACROSS IN-MEMORY REGISTRY
 */
function dispatchFilterCoordinates() {
    // 1. Áp dụng thuật toán tìm kiếm Tiếng Việt không dấu
    const searchRaw = document.getElementById("internalSearchBox").value;
    const searchVal = loaiBoDauTiengViet(searchRaw);
    
    const catVal = document.getElementById("categorySelector").value;
    const statusVal = document.getElementById("statusFilterSelector").value;

    filteredProductDataset = masterProductDataset.filter(product => {
        // Chuẩn hóa tên và mã sản phẩm để so sánh
        const normalizedName = loaiBoDauTiengViet(product.name);
        const normalizedId = loaiBoDauTiengViet(product.id);

        // Evaluate keyword intersection mapping
        const matchesSearch = normalizedId.includes(searchVal) || normalizedName.includes(searchVal);
        
        // Evaluate category mapping boundary
        const matchesCategory = (catVal === "ALL" || product.category === catVal);
        
        // Evaluate complex tag classifications
        const matchesStatus = (statusVal === "ALL" || product.intelligenceTag === statusVal);

        return matchesSearch && matchesCategory && matchesStatus;
    });

    // =========================================================================
    // 🔥 THUẬT TOÁN: SẮP XẾP SẢN PHẨM (CÒN HÀNG LÊN TRƯỚC, HẾT HÀNG XUỐNG CUỐI)
    // =========================================================================
    filteredProductDataset.sort((a, b) => {
        let scoreA = a.stock > 0 ? 0 : 1;
        let scoreB = b.stock > 0 ? 0 : 1;
        return scoreA - scoreB;
    });

    currentTablePageOffset = 0; // Reset pagination indexing anchor
    compileVisualCatalogGrid();
    compileVisualInventoryTable();
    if (typeof renderLowStockDetails === "function") renderLowStockDetails();
    updateFilteredKPIs();
    updateFilteredCharts(catVal, statusVal);
}

function compileVisualCatalogGrid() {
    const container = document.getElementById("catalogCardsContainer");
    container.innerHTML = "";

    // Render limited elements on card grid view matrix for premium feel layouts
    const maxVisibleGridItems = 8;
    const targets = filteredProductDataset.slice(0, maxVisibleGridItems);

    if (targets.length === 0) {
        container.innerHTML = `<div style="grid-column: 1/-1; padding:40px; text-align:center; color:#64748b; font-size:14px;">
            Không tìm thấy sản phẩm nào phù hợp với bộ lọc hiện tại.
        </div>`;
        return;
    }

    targets.forEach(p => {
        // =====================================================================
        // 1. XỬ LÝ FIX LỖI ICON (Dùng HTML Entities để tránh sinh ra ô vuông)
        // =====================================================================
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

        // =====================================================================
        // 2. HỆ THỐNG RENDER ẢNH THÔNG MINH
        // =====================================================================
        let imageRender = "";
        let frameStyle = "";
        
        if (p.image && p.image !== "") {
            // Đổi object-fit thành 'contain' để ảnh hiển thị trọn vẹn, không bị cắt
            // Thêm đệm lót padding: 16px để ảnh thu nhỏ lại, cách đều các viền
            imageRender = `<img src="${p.image}" style="width:100%; height:100%; object-fit:contain;" alt="${p.name}">`;
            frameStyle = "background: transparent; padding: 32px;"; 
        } else {
            // Không có ảnh -> Lấy chữ cái đầu tiên làm đại diện
            const displayChar = p.name ? p.name.charAt(0).toUpperCase() : "📦";
            imageRender = displayChar;
        }

        // =====================================================================
        // 3. ĐỔ HTML VÀO GIAO DIỆN
        // =====================================================================
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

    // Update operational metadata string coordinates metrics
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

function triggerCoreDataSync() {
    // This can be set to call out back to Java runtime inside compiled executable environments
    console.log("[WebKit Environment Hook] Invoking data refresh request across pipeline...");
    // Fallback client simulation if running standalone container
    alert("Đang gử̉i lệnh đồng bộ luồng dữ liệu async tới Java Swing Controller...");
}

function handleQuickActionClick(productId, eventType) {
    if (eventType === 'VIEW') {
        // 🔥 ĐÃ FIX: Dùng window.javaConnector thay vì typeof
        if (window.javaConnector) {
            // Gọi qua Java thành công!
            window.javaConnector.openProductDetail(productId);
        } else {
            // Chạy chay trên trình duyệt ngoài (Chrome/Edge)
            alert(`[Chế độ Web] Yêu cầu mở Form Chi Tiết cho mã: ${productId}`);
        }
    }
}

/**
 * INTERFACE CONNECTOR ENVELOPE CALLED DIRECTLY FROM JAVA FOR ALTERNATIVE PIPELINES
 */
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
    // Xóa các ký tự kết hợp Unicode
    return str.normalize("NFD").replace(/[\u0300-\u036f]/g, "").trim();
}
// =========================================================================
// 🔥 HÀM MỚI: TỰ ĐỘNG LỌC VÀ HIỂN THỊ DANH SÁCH CHI TIẾT HỤT KHO
// =========================================================================
function renderLowStockDetails() {
    const container = document.getElementById("lowStockDetailsContainer");
    const listTarget = document.getElementById("lowStockListTarget");
    
    // Lọc lấy tất cả sản phẩm bị dán mác LOW_STOCK từ dữ liệu tổng
    const lowStockItems = masterProductDataset.filter(p => p.intelligenceTag === "LOW_STOCK");
    
    if (lowStockItems.length > 0) {
        container.style.display = "block"; // Bật khung cảnh báo lên
        listTarget.innerHTML = "";
        
        // Render từng item
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
        // Nếu kho dồi dào, tự động ẩn khung cảnh báo này đi
        container.style.display = "none";
    }
}
// =========================================================================
// 🔥 HÀM MỚI: CẬP NHẬT 4 THẺ KPI TRÊN CÙNG DỰA THEO BỘ LỌC
// =========================================================================
function updateFilteredKPIs() {
    // 1. Tổng SKU & Danh mục
    document.getElementById("txt-total-products").innerText = filteredProductDataset.length;
    let uniqueCats = new Set(filteredProductDataset.map(p => p.category)).size;
    document.getElementById("txt-total-categories").innerText = uniqueCats + " danh mục hàng hóa";

    // 2. Sản Phẩm Top 1 (trong bộ lọc hiện tại)
    let sortedBySales = [...filteredProductDataset].sort((a, b) => (b.unitsSold || 0) - (a.unitsSold || 0));
    if (sortedBySales.length > 0 && sortedBySales[0].unitsSold > 0) {
        document.getElementById("txt-top-product-name").innerText = sortedBySales[0].name;
        document.getElementById("txt-top-product-name").title = sortedBySales[0].name;
        document.getElementById("txt-top-product-qty").innerText = sortedBySales[0].unitsSold + " đơn vị đã chốt";
    } else {
        document.getElementById("txt-top-product-name").innerText = "--";
        document.getElementById("txt-top-product-qty").innerText = "0 đơn vị";
    }

    // 3. Cảnh Báo Hụt Kho
    let lowStockCount = filteredProductDataset.filter(p => p.intelligenceTag === "LOW_STOCK").length;
    document.getElementById("txt-low-stock-count").innerText = lowStockCount;

    // 4. Giá trị tổng kho ước tính (Giá Bán * Tồn Kho)
    let estimatedValue = filteredProductDataset.reduce((sum, p) => sum + (p.price * p.stock), 0);
    document.getElementById("txt-valuation-display").innerText = new Intl.NumberFormat('vi-VN').format(estimatedValue) + "đ";
}

// =========================================================================
// 🔥 HÀM MỚI 2.0: BIẾN HÌNH BIỂU ĐỒ BÁNH & CỘT THEO ĐA BỘ LỌC
// =========================================================================
function updateFilteredCharts(selectedCategory, selectedStatus) {
    // 1. Logic cập nhật Biểu đồ Cột (Sản Phẩm Dẫn Đầu Doanh Số)
    let sortedBySales = [...filteredProductDataset].sort((a, b) => (b.unitsSold || 0) - (a.unitsSold || 0));
    let top5Sales = sortedBySales.slice(0, 5);

    // Gán mặc định nếu không tìm thấy dữ liệu
    let barNames = top5Sales.length > 0 ? top5Sales.map(p => p.name) : ["Chưa có dữ liệu"];
    let barQuantities = top5Sales.length > 0 ? top5Sales.map(p => p.unitsSold || 0) : [0];

    topSellingChartInstance.updateOptions({ xaxis: { categories: barNames } });
    topSellingChartInstance.updateSeries([{ name: 'Đơn vị bán lẻ', data: barQuantities }]);

    // 2. Logic Biến hình cho Biểu đồ Donut (Bánh xoay)
    const donutTitleObj = document.querySelector('.distribution-donut .chart-card-heading');

    // Xử lý trường hợp bộ lọc quá khắt khe, không có sản phẩm nào
    if (filteredProductDataset.length === 0) {
        donutTitleObj.innerText = "Không có sản phẩm phù hợp";
        categoryDonutChartInstance.updateOptions({ labels: ['Trống'], colors: ['#f1f5f9'] });
        categoryDonutChartInstance.updateSeries([1]);
        return;
    }

    // Kiểm tra xem người dùng có đang dùng bộ lọc nào không
    if (selectedCategory === "ALL" && selectedStatus === "ALL") {
        // TRƯỜNG HỢP A: Không lọc gì cả -> Hiển thị Cơ cấu theo Phân Loại (Mặc định)
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
        // TRƯỜNG HỢP B: Có dùng bộ lọc (Danh mục HOẶC Trạng thái) -> Bóc tách chi tiết từng Sản phẩm
        let titleParts = [];
        if (selectedCategory !== "ALL") titleParts.push(selectedCategory);
        if (selectedStatus !== "ALL") {
            // Map mã trạng thái sang tiếng Việt cho tiêu đề đẹp hơn
            const statusNames = {
                "BEST_SELLER": "🔥 Bán Chạy",
                "LOW_STOCK": "⚠️ Tồn Kho Thấp",
                "SLOW_MOVING": "🐢 Bán Chậm"
            };
            titleParts.push(statusNames[selectedStatus] || selectedStatus);
        }
        
        donutTitleObj.innerText = "Tỷ Trọng Kho: " + titleParts.join(" - ");
        
        // Sắp xếp sản phẩm theo tồn kho từ cao xuống thấp
        let sortedByStock = [...filteredProductDataset].sort((a, b) => b.stock - a.stock);
        
        // Lấy Top 6 sản phẩm tồn kho nhiều nhất, phần còn lại gộp vào "Các SP Khác"
        let topStock = sortedByStock.slice(0, 6);
        let others = sortedByStock.slice(6);

        let donutLabels = topStock.map(p => p.name);
        let donutSeries = topStock.map(p => p.stock);

        if (others.length > 0) {
            let othersStock = others.reduce((sum, p) => sum + p.stock, 0);
            donutLabels.push("Các SP Khác");
            donutSeries.push(othersStock);
        }

        categoryDonutChartInstance.updateOptions({ 
            labels: donutLabels,
            // Thêm màu xám nhạt ở cuối cho mảng "Các SP Khác"
            colors: ['#2563eb', '#10b981', '#f59e0b', '#84cc16', '#a855f7', '#ec4899', '#cbd5e1']
        });
        categoryDonutChartInstance.updateSeries(donutSeries);
    }
}

// =========================================================================
// 🔥 HÀM MỚI: XUẤT FILE TOÀN BỘ TRẠNG THÁI GIAO DIỆN
// =========================================================================
function triggerSystemExport() {
    if (window.javaConnector) {
        // Bắn tín hiệu qua Java để ghi file vào ổ D:
        window.javaConnector.exportDashboardData();
    } else {
        // Fallback khi bạn mở file HTML chạy chay trên Chrome
        alert("Đang chạy chế độ Web độc lập! JSON State của giao diện đã sẵn sàng để xuất.");
    }
}

// =========================================================================
// 🔥 HÀM MỚI: TẢI DANH SÁCH LỊCH SỬ FILE VÀ PHỤC DỰNG GIAO DIỆN
// =========================================================================

function loadExportHistory() {
    if (window.javaConnector) {
        let historyJson = window.javaConnector.getExportHistoryList();
        
        // 🔥 DEBUG: In ra console để kiểm tra Java trả về gì
        console.log("History JSON from Java:", historyJson);
        
        let files = JSON.parse(historyJson);
        console.log("Files count:", files.length);
        
        let selector = document.getElementById("historySelector");
        selector.innerHTML = '<option value="">Lich su xuat file</option>';
        
        files.forEach(function(f) {
            console.log("Processing file:", f); // Xem tên file thật
            
            let displayName = f.replace(".json", "");
            
            // Xử lý cả 2 trường hợp tên file
            if (f.indexOf("_") !== -1) {
                let parts = displayName.split("_"); // Tách theo dấu _
                // parts = ["Thongke", "31", "05", "2026"]
                if (parts.length === 4) {
                    displayName = "[" + parts[1] + "/" + parts[2] + "/" + parts[3] + "] Bao cao kho";
                } else if (parts.length >= 2) {
                    // Fallback: lấy 3 phần cuối
                    let n = parts.length;
                    displayName = "[" + parts[n-3] + "/" + parts[n-2] + "/" + parts[n-1] + "] Bao cao";
                }
            }
            
            let opt = document.createElement("option");
            opt.value = f;
            opt.innerText = displayName;
            selector.appendChild(opt);
        });
    } else {
        console.log("javaConnector chua san sang!");
    }
}

// Hàm 2: Khi user chọn 1 file -> Lấy data từ Java -> Cập nhật toàn bộ web
function loadHistoricalData(fileName) {
    if (!fileName || fileName === "") return; // Nếu chọn dòng mặc định thì bỏ qua

    if (window.javaConnector) {
        // Thay vì kéo về, yêu cầu Java tự đọc và đẩy xuống
        window.javaConnector.readAndLoadExportFile(fileName);
    }
}

// Hàm 2B: Java gọi ngược lại hàm này, truyền Base64 vào để Javascript giải mã
function applyHistoricalStateBase64(base64Data, fileName) {
    try {
        // Giải mã Base64 sang chuẩn UTF-8 (Giữ nguyên vẹn Tiếng Việt)
        let decodedString = decodeURIComponent(escape(window.atob(base64Data)));
        let historicalPayload = JSON.parse(decodedString);
        
        // Vẽ lại toàn bộ trang web bằng dữ liệu quá khứ
        updateProductDashboard(historicalPayload); 
        
        // Bật cảnh báo giao diện đang ở chế độ quá khứ
        document.querySelector(".header-sub-caption").innerText = "[CHE DO LICH SU] Dang xem du lieu tu file: " + fileName;
        document.querySelector(".header-sub-caption").style.color = "#ef4444";
        document.querySelector(".header-sub-caption").style.fontWeight = "bold";
    } catch(e) {
        alert("Lỗi giải mã file lịch sử: " + e.message);
    }
}