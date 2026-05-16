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
}

function compileVisualCatalogGrid() {
    const container = document.getElementById("catalogCardsContainer");
    container.innerHTML = "";

    // Render limited elements on card grid view matrix for premium feel layouts
    const maxVisibleGridItems = 8;
    const targets = filteredProductDataset.slice(0, maxVisibleGridItems);

    if (targets.length === 0) {
        container.innerHTML = `<div style="grid-column: 1/-1; padding:40px; text-align:center; color:#64748b; font-size:14px;">
            No items match selected query boundaries.
        </div>`;
        return;
    }

    targets.forEach(p => {
        let tagHtml = "";
        if (p.intelligenceTag === "BEST_SELLER") tagHtml = `<span class="card-stock-badge bg-success-light">🔥 Top Bán Chạy</span>`;
        else if (p.intelligenceTag === "LOW_STOCK") tagHtml = `<span class="card-stock-badge bg-danger-light">⚠️ Tồn Kho Ít</span>`;
        else if (p.intelligenceTag === "SLOW_MOVING") tagHtml = `<span class="card-stock-badge bg-warning-light">🐢 Bán Chậm</span>`;
        else tagHtml = `<span class="card-stock-badge bg-success-light">✅ Ổn Định</span>`;

        // Safe evaluation fallback character rendering
        const displayChar = p.name ? p.name.charAt(0) : "📦";

        container.innerHTML += `
            <div class="saas-catalog-card">
                <div class="card-image-placeholder-frame">
                    ${displayChar}
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
    console.log(`[Action Trigger] PID: ${productId} | Action: ${eventType}`);
    alert(`Đã chọn sản phẩm ${productId}. Tính năng liên kết với Form chỉnh sửa chi tiết của Nghiệp vụ PBL3.`);
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