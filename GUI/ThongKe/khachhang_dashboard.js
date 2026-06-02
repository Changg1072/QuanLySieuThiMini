let systemMasterCollection = [];
let localizedFilteredPartition = [];
let customerRanksDonutInstance = null;
let originalDashboardData = null;
let tierRevenueBarChartInstance = null;

// Table pagination pointer limits
let currentPagerIndex = 0;
const pagerPageSizeValue = 5;

document.addEventListener("DOMContentLoaded", function () {
    preInitializeGraphicalShells();
});

function preInitializeGraphicalShells() {
    customerRanksDonutInstance = new ApexCharts(document.querySelector("#chartCustomerRanks"), {
        series: [1, 1, 1, 1, 1],
        chart: { type: 'donut', height: 250, fontFamily: 'Inter' },
        labels: ['Không hạng', 'Đồng', 'Bạc', 'Vàng', 'VIP'],
        colors: ['#cbd5e1', '#94a3b8', '#38bdf8', '#fbbf24', '#a78bfa'],
        legend: { position: 'bottom' }
    });
    customerRanksDonutInstance.render();

    tierRevenueBarChartInstance = new ApexCharts(document.querySelector("#chartTierRevenue"), {
        series: [
            { name: 'Số Hóa Đơn', type: 'column', data: [0,0,0,0,0,0] },
            { name: 'Tổng Tiền (đ)', type: 'column', data: [0,0,0,0,0,0] }
        ],
        chart: { type: 'bar', height: 250, fontFamily: 'Inter', toolbar: { show: false }, zoom: { enabled: false } },
        plotOptions: { bar: { columnWidth: '60%', borderRadius: 4 } },
        colors: ['#0ea5e9', '#10b981'],
        labels: ['Vãng Lai', 'Kh.Hạng', 'Đồng', 'Bạc', 'Vàng', 'VIP'], // 6 NHÃN CHUẨN XÁC
        dataLabels: { enabled: false },
        stroke: { width: 1, colors: ['transparent'] },
        yaxis: [
            {
                title: {
                    text: 'Hóa Đơn',
                    style: { fontSize: '12px', fontWeight: 500, fontFamily: 'Inter' }
                },
                labels: { formatter: val => Math.round(val) }
            },
            {
                opposite: true,
                title: {
                    text: 'Doanh Thu',
                    style: { fontSize: '12px', fontWeight: 500, fontFamily: 'Inter' }
                },
                labels: {
                    formatter: val => (val / 1000000).toFixed(1) + 'Tr'
                }
            }
        ],
        legend: { position: 'top' }
    });
    tierRevenueBarChartInstance.render();
}
/**
 * MAIN ENTRY HOOK SIGNAL INVOKED VIA CORE JAVA RUNTIME ENVIRONMENT
 */
function updateCustomerDashboard(jsonPackage) {
    if (!jsonPackage) return;

    originalDashboardData = jsonPackage; // Store the original data

    // 1. Assign KPI Metric counter representations
    document.getElementById("val-total-customers").innerText = jsonPackage.totalCustomers || 0;
    document.getElementById("val-new-customers-month").innerText = (jsonPackage.newCustomersThisMonth || 0) + " hội viên tháng này";
    document.getElementById("val-vip-count").innerText = jsonPackage.vipCount || 0;
    document.getElementById("val-total-invoices").innerText = new Intl.NumberFormat('vi-VN').format(jsonPackage.totalInvoiceCount || 0) + " đơn";
    document.getElementById("val-avg-ticket").innerText = "Vé trung bình: " + new Intl.NumberFormat('vi-VN').format(jsonPackage.averageTicketSize || 0) + "đ";
    document.getElementById("val-total-points").innerText = new Intl.NumberFormat('vi-VN').format(jsonPackage.totalLoyaltyPoints || 0);
    document.getElementById("val-return-invoices").innerText = new Intl.NumberFormat('vi-VN').format(jsonPackage.returnedInvoiceCount || 0) + " đơn";
    document.getElementById("val-refund-amount").innerText = "Hoàn tiền: " + new Intl.NumberFormat('vi-VN').format(jsonPackage.totalRefundAmount || 0) + "đ";

    // 2. Refreshes graphical vectors asynchronously (ĐÃ CẬP NHẬT 5 HẠNG THẺ BAO GỒM KHÔNG HẠNG)
    if (jsonPackage.rankDistributions && customerRanksDonutInstance) {
        const d = jsonPackage.rankDistributions;
        customerRanksDonutInstance.updateSeries([
            d.KhongHang || 0, 
            d.Dong || 0, 
            d.Bac || 0, 
            d.Vang || 0, 
            d.KimCuong || 0
        ]);
    }

    // 3. Build AI Operational Intelligence Feed Bullets
    const insightBox = document.getElementById("insightsContainerTarget");
    insightBox.innerHTML = "";
    if (jsonPackage.intelligenceInsights && jsonPackage.intelligenceInsights.length > 0) {
        jsonPackage.intelligenceInsights.forEach(str => {
            insightBox.innerHTML += `<div class="insight-item-bullet">${str}</div>`;
        });
    } else {
        insightBox.innerHTML = '<div style="color:#64748b;font-size:13px;padding:10px;">✅ Chưa có chỉ báo hành vi khẩn cấp nào phát sinh.</div>';
    }

    // 4. Populate Loyalty Ranking Top Spenders
    const leaderboardBox = document.getElementById("leaderboardContainerTarget");
    leaderboardBox.innerHTML = "";
    if (jsonPackage.loyaltyLeaderboard && jsonPackage.loyaltyLeaderboard.length > 0) {
        jsonPackage.loyaltyLeaderboard.forEach((user, idx) => {
            let badgeClass = "bg-diamond";
            if (idx === 0) badgeClass = "bg-gold";
            const initialChar = user.name ? user.name.charAt(0) : "C";

            leaderboardBox.innerHTML += `
                <div class="leader-row-item">
                    <div class="leader-cell" style="width:10%; font-weight:700;">#${idx+1}</div>
                    <div class="leader-cell" style="width:15%;"><div class="avatar-badge-circle">${initialChar}</div></div>
                    <div class="leader-cell" style="width:45%;">
                        <div style="font-weight:600;">${user.name}</div>
                        <span class="leader-badge-pill ${badgeClass}">${user.tier}</span>
                    </div>
                    <div class="leader-cell" style="width:30%; text-align:right; font-weight:700; color:var(--purple-vibrant);">
                        ${new Intl.NumberFormat('vi-VN').format(user.totalSpent)}đ
                        <div style="font-size:11px;color:#64748b;font-weight:400;">${user.ordersCount} hóa đơn</div>
                    </div>
                </div>
            `;
        });
    } else {
        leaderboardBox.innerHTML = '<div style="text-align:center;padding:40px;color:#64748b;">Chưa có dữ liệu tích lũy chi tiêu hóa đơn.</div>';
    }

    // 5. Append Relational Global Purchase Timeline Nodes
    const timelineBox = document.getElementById("timelineContainerTarget");
    timelineBox.innerHTML = "";
    if (jsonPackage.purchaseTimeline && jsonPackage.purchaseTimeline.length > 0) {
        jsonPackage.purchaseTimeline.forEach(log => {
            timelineBox.innerHTML += `
                <div class="timeline-micro-card">
                    <span class="timeline-stamp-lbl">${log.time} - Bill ${log.id}</span>
                    <div class="timeline-main-txt">${log.customer} mua sắm</div>
                    <div style="font-size:12px;color:#475569;">
                        Giá trị: <b>${new Intl.NumberFormat('vi-VN').format(log.amount)}đ</b> | hình thức: ${log.method}
                    </div>
                </div>
            `;
        });
    } else {
        timelineBox.innerHTML = '<div style="text-align:center;padding:40px;color:#64748b;">Chưa ghi nhận giao dịch phát sinh gần đây.</div>';
    }

    // 6. Bind full table objects into local memory models for instant pagination
    if (jsonPackage.customerGridMatrix) {
        systemMasterCollection = jsonPackage.customerGridMatrix;
        // Tự động gọi lại bộ lọc để vẽ bảng theo trạng thái ô tìm kiếm/dropdown hiện tại
        handleRealtimeFiltering();
    }
}

// =========================================================================
// 🔥 CORE DIACRITIC REMOVAL SEARCH TECHNIQUE (CLONED FROM JAVA LOGIC LAYER)
// =========================================================================
function stripVietnameseAccents(str) {
    if (!str) return "";
    return str.toLowerCase()
        .normalize("NFD")
        .replace(/[̀-ͯ]/g, "")
        .replace(/đ/g, "d");
}

function handleRealtimeFiltering() {
    const rawKeyword = document.getElementById("crmSearchInput").value;
    const cleanSearchKeyword = stripVietnameseAccents(rawKeyword.trim());
    const targetTierFilter = document.getElementById("tierSelector").value;

    localizedFilteredPartition = systemMasterCollection.filter(c => {
        const strippedName = stripVietnameseAccents(c.name);
        const matchesSearch = strippedName.includes(cleanSearchKeyword) || c.phone.includes(cleanSearchKeyword) || c.id.toLowerCase().includes(cleanSearchKeyword);
        const matchesTier = (targetTierFilter === "ALL" || c.tier === targetTierFilter);
        return matchesSearch && matchesTier;
    });

    currentPagerIndex = 0; // Reset index back to absolute first view
    renderStructuredTableDOM();
}

function renderStructuredTableDOM() {
    const tbody = document.getElementById("crmTableBodyTarget");
    tbody.innerHTML = "";

    const pointerStart = currentPagerIndex * pagerPageSizeValue;
    const pointerEnd = pointerStart + pagerPageSizeValue;
    const viewSegment = localizedFilteredPartition.slice(pointerStart, pointerEnd);

    // Synchronize tracker elements display properties
    document.getElementById("txtPagerLabel").innerText = 
        `Hiển thị ${localizedFilteredPartition.length > 0 ? pointerStart + 1 : 0} - ${Math.min(pointerEnd, localizedFilteredPartition.length)} của ${localizedFilteredPartition.length} tài khoản`;

    if (viewSegment.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:#64748b;padding:30px;">Không tìm thấy hồ sơ khách hàng khớp với điều kiện lọc.</td></tr>`;
        return;
    }

    viewSegment.forEach(c => {
        tbody.innerHTML += `
            <tr>
                <td style="font-weight:600;color:var(--blue-accent);">${c.id}</td>
                <td><b>${c.name}</b><div style="font-size:11px;color:#64748b;">Gia nhập: ${c.joinDate}</div></td>
                <td>${c.phone}</td>
                <td style="text-align:center;"><span class="leader-badge-pill" style="background:#f1f5f9;color:#334155;border:1px solid #cbd5e1;font-weight:600;">${c.tier}</span></td>
                <td style="text-align:right;font-weight:600;color:#b45309;">${new Intl.NumberFormat('vi-VN').format(c.points)}</td>
                <td style="text-align:right;font-weight:700;color:var(--text-slate-heavy);">${new Intl.NumberFormat('vi-VN').format(c.totalSpent)}đ</td>
                <td style="text-align:right;">
                    <button class="micro-action-pill-btn" onclick="alert('ACTION:VIEW_${c.id}')">Hồ sơ</button>
                </td>
            </tr>
        `;
    });
}

function modifyPagePointer(direction) {
    const computedMaxPages = Math.ceil(localizedFilteredPartition.length / pagerPageSizeValue);
    const validatedNextPage = currentPagerIndex + direction;

    if (validatedNextPage >= 0 && validatedNextPage < computedMaxPages) {
        currentPagerIndex = validatedNextPage;
        renderStructuredTableDOM();
    }
}

// =========================================================================
// 🔥 HỆ THỐNG LỌC TÌM KIẾM THỜI GIAN THỰC & CẬP NHẬT KPI
// =========================================================================

// Hàm loại bỏ dấu tiếng việt để tìm kiếm không dấu
function removeVietnameseAccents(str) {
    if (!str) return "";
    return str.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d");
}

// Bắt sự kiện khi gõ phím hoặc chọn dropdown
function handleRealtimeFiltering() {
    const rawSearchToken = document.getElementById("crmSearchInput").value;
    const cleanKeyword = removeVietnameseAccents(rawSearchToken.trim());
    const tierSelectorValue = document.getElementById("tierSelector").value;

    // Lọc mảng dữ liệu gốc (đã được Java đẩy xuống)
    if (!systemMasterCollection || systemMasterCollection.length === 0) return;

    localizedFilteredPartition = systemMasterCollection.filter(c => {
        const safeName = c.name ? removeVietnameseAccents(c.name) : "";
        const safePhone = c.phone ? c.phone.toLowerCase() : "";
        const safeId = c.id ? c.id.toLowerCase() : "";
        const safeTier = c.tier ? c.tier : "Đồng";

        // Kiểm tra khớp từ khóa tìm kiếm (Mã, Tên, SĐT)
        const matchesSearch = cleanKeyword === "" || 
                              safeName.includes(cleanKeyword) || 
                              safePhone.includes(cleanKeyword) || 
                              safeId.includes(cleanKeyword);
                              
        // Kiểm tra khớp hạng thẻ
        let matchesTier = false;
        if (tierSelectorValue === "ALL") {
            matchesTier = true;
        } else if (tierSelectorValue === "Vip") {
            matchesTier = (safeTier.toLowerCase() === "vip");
        } else {
            matchesTier = (safeTier === tierSelectorValue);
        }
        
        return matchesSearch && matchesTier;
    });

    currentPagerIndex = 0; // Reset về trang 1
    renderCustomerTable(); // Vẽ lại bảng
    updateFilteredKPIsAndCharts(); // Tự động nhảy số KPI
}

// Hàm vẽ lại bảng theo dữ liệu đã lọc
function renderCustomerTable() {
    const tbody = document.getElementById("crmTableBodyTarget");
    tbody.innerHTML = "";

    const startIndex = currentPagerIndex * pagerPageSizeValue;
    const endIndex = startIndex + pagerPageSizeValue;
    const viewSegment = localizedFilteredPartition.slice(startIndex, endIndex);

    const totalItems = localizedFilteredPartition.length;
    const startLabel = totalItems > 0 ? startIndex + 1 : 0;
    const endLabel = Math.min(endIndex, totalItems);

    document.getElementById("txtPagerLabel").innerText = `Hiển thị ${startLabel} - ${endLabel} của ${totalItems} khách hàng`;

    if (viewSegment.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#64748b; padding:30px;">Không tìm thấy hồ sơ khách hàng khớp với điều kiện lọc.</td></tr>`;
        return;
    }

    viewSegment.forEach(c => {
        tbody.innerHTML += `
            <tr>
                <td style="font-weight:600;color:var(--blue-accent);">${c.id}</td>
                <td><b>${c.name}</b><div style="font-size:11px;color:#64748b;">Gia nhập: ${c.joinDate}</div></td>
                <td>${c.phone}</td>
                <td style="text-align:center;"><span class="leader-badge-pill" style="background:#f1f5f9;color:#334155;border:1px solid #cbd5e1;font-weight:600;">${c.tier}</span></td>
                <td style="text-align:right;font-weight:600;color:#b45309;">${new Intl.NumberFormat('vi-VN').format(c.points || 0)}</td>
                <td style="text-align:right;font-weight:700;color:var(--text-slate-heavy);">${new Intl.NumberFormat('vi-VN').format(c.totalSpent || 0)}đ</td>
                <td style="text-align:right;">
                    <button class="micro-action-pill-btn" onclick="alert('ACTION:VIEW_${c.id}')">Hồ sơ</button>
                </td>
            </tr>
        `;
    });
}

// Chuyển trang cho bảng dữ liệu đã lọc
function modifyPagePointer(direction) {
    const maxPages = Math.ceil(localizedFilteredPartition.length / pagerPageSizeValue);
    const calculatedNextIndex = currentPagerIndex + direction;
    if (calculatedNextIndex >= 0 && calculatedNextIndex < maxPages) {
        currentPagerIndex = calculatedNextIndex;
        renderCustomerTable();
    }
}

// Hàm nhảy số 4 thẻ KPI và Biểu đồ Bánh
function updateFilteredKPIsAndCharts() {
    const rawSearchToken = document.getElementById("crmSearchInput").value;
    const cleanKeyword = removeVietnameseAccents(rawSearchToken.trim());
    const tierSelectorValue = document.getElementById("tierSelector").value;

    if (cleanKeyword === "" && tierSelectorValue === "ALL" && originalDashboardData) {
        document.getElementById("val-total-customers").innerText = originalDashboardData.totalCustomers || 0;
        document.getElementById("val-vip-count").innerText = originalDashboardData.vipCount || 0;
        document.getElementById("val-total-invoices").innerText = new Intl.NumberFormat('vi-VN').format(originalDashboardData.totalInvoiceCount || 0) + " đơn";
        document.getElementById("val-avg-ticket").innerText = "Vé trung bình: " + new Intl.NumberFormat('vi-VN').format(originalDashboardData.averageTicketSize || 0) + "đ";
        document.getElementById("val-total-points").innerText = new Intl.NumberFormat('vi-VN').format(originalDashboardData.totalLoyaltyPoints || 0);

        if (customerRanksDonutInstance && originalDashboardData.rankDistributions) {
            const d = originalDashboardData.rankDistributions;
            customerRanksDonutInstance.updateSeries([
                d.KhongHang || 0, d.Dong || 0, d.Bac || 0, d.Vang || 0, d.KimCuong || 0
            ]);
        }
        
        if (tierRevenueBarChartInstance && originalDashboardData.tierStats) {
            const ts = originalDashboardData.tierStats;
            tierRevenueBarChartInstance.updateSeries([
                { name: 'Số Hóa Đơn', data: [ts.KhachVangLai.count, ts.KhongHang.count, ts.Dong.count, ts.Bac.count, ts.Vang.count, ts.KimCuong.count] },
                { name: 'Tổng Tiền (đ)', data: [ts.KhachVangLai.revenue, ts.KhongHang.revenue, ts.Dong.revenue, ts.Bac.revenue, ts.Vang.revenue, ts.KimCuong.revenue] }
            ]);
        }
        return; 
    }

    let totalSpent = 0, totalPoints = 0, totalOrders = 0;
    let totalReturns = 0, totalRefunds = 0;
    let rankCounts = { 'Không hạng': 0, 'Đồng': 0, 'Bạc': 0, 'Vàng': 0, 'Vip': 0 };
    
    let tierBills = { 'Khách vãng lai': 0, 'Không hạng': 0, 'Đồng': 0, 'Bạc': 0, 'Vàng': 0, 'Vip': 0 };
    let tierRevs  = { 'Khách vãng lai': 0, 'Không hạng': 0, 'Đồng': 0, 'Bạc': 0, 'Vàng': 0, 'Vip': 0 };

    localizedFilteredPartition.forEach(c => {
        totalSpent += (c.totalSpent || 0);
        totalPoints += (c.points || 0);
        totalOrders += (c.ordersCount || 0);
        totalReturns += (c.returnCount || 0); // 🔥 Thêm dòng này
        totalRefunds += (c.refundAmount || 0);

        let t = c.tier || 'Không hạng';
        let normT = 'Không hạng';
        if (t === 'Khách vãng lai') normT = 'Khách vãng lai';
        else if (t.toLowerCase() === 'vip') normT = 'Vip';
        else if (t === 'Vàng') normT = 'Vàng';
        else if (t === 'Bạc') normT = 'Bạc';
        else if (t === 'Đồng') normT = 'Đồng';

        if (normT !== 'Khách vãng lai') rankCounts[normT]++;
        tierBills[normT] += (c.ordersCount || 0);
        tierRevs[normT] += (c.totalSpent || 0);
    });

    document.getElementById("val-total-customers").innerText = localizedFilteredPartition.filter(c => c.id !== "KVL").length;
    document.getElementById("val-vip-count").innerText = rankCounts['Vip'] + rankCounts['Vàng'];
    document.getElementById("val-total-invoices").innerText = new Intl.NumberFormat('vi-VN').format(totalOrders) + " đơn";
    document.getElementById("val-return-invoices").innerText = new Intl.NumberFormat('vi-VN').format(totalReturns) + " đơn";
    document.getElementById("val-refund-amount").innerText = "Hoàn tiền: " + new Intl.NumberFormat('vi-VN').format(Math.round(totalRefunds)) + "đ";

    let avgTicket = totalOrders > 0 ? (totalSpent / totalOrders) : 0;
    document.getElementById("val-avg-ticket").innerText = "Vé trung bình: " + new Intl.NumberFormat('vi-VN').format(Math.round(avgTicket)) + "đ";
    document.getElementById("val-total-points").innerText = new Intl.NumberFormat('vi-VN').format(totalPoints);

    if (customerRanksDonutInstance) {
        customerRanksDonutInstance.updateSeries([
            rankCounts['Không hạng'], rankCounts['Đồng'], rankCounts['Bạc'], rankCounts['Vàng'], rankCounts['Vip']
        ]);
    }
    
    if (tierRevenueBarChartInstance) {
        tierRevenueBarChartInstance.updateSeries([
            { name: 'Số Hóa Đơn', data: [tierBills['Khách vãng lai'], tierBills['Không hạng'], tierBills['Đồng'], tierBills['Bạc'], tierBills['Vàng'], tierBills['Vip']] },
            { name: 'Tổng Tiền (đ)', data: [tierRevs['Khách vãng lai'], tierRevs['Không hạng'], tierRevs['Đồng'], tierRevs['Bạc'], tierRevs['Vàng'], tierRevs['Vip']] }
        ]);
    }
}
