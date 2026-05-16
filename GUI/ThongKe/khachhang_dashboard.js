let systemMasterCollection = [];
let localizedFilteredPartition = [];
let customerRanksDonutInstance = null;

// Table pagination pointer limits
let currentPagerIndex = 0;
const pagerPageSizeValue = 5;

document.addEventListener("DOMContentLoaded", function () {
    preInitializeGraphicalShells();
});

function preInitializeGraphicalShells() {
    customerRanksDonutInstance = new ApexCharts(document.querySelector("#chartCustomerRanks"), {
        series: [1, 1, 1, 1],
        chart: { type: 'donut', height: 250, fontFamily: 'Inter' },
        labels: ['Đồng', 'Bạc', 'Vàng', 'VIP'],
        colors: ['#94a3b8', '#38bdf8', '#fbbf24', '#a78bfa'],
        legend: { position: 'bottom' }
    });
    customerRanksDonutInstance.render();
}

/**
 * MAIN ENTRY HOOK SIGNAL INVOKED VIA CORE JAVA RUNTIME ENVIRONMENT
 */
function updateCustomerDashboard(jsonPackage) {
    if (!jsonPackage) return;

    // 1. Assign KPI Metric counter representations
    document.getElementById("val-total-customers").innerText = jsonPackage.totalCustomers || 0;
    document.getElementById("val-new-customers-month").innerText = (jsonPackage.newCustomersThisMonth || 0) + " hội viên tháng này";
    document.getElementById("val-vip-count").innerText = jsonPackage.vipCount || 0;
    document.getElementById("val-return-rate").innerText = (jsonPackage.returnCustomerRate || 0) + "%";
    document.getElementById("val-avg-ticket").innerText = "Vé trung bình: " + new Intl.NumberFormat('vi-VN').format(jsonPackage.averageTicketSize || 0) + "đ";
    document.getElementById("val-total-points").innerText = new Intl.NumberFormat('vi-VN').format(jsonPackage.totalLoyaltyPoints || 0);

    // 2. Refreshes graphical vectors asynchronously 
    if (jsonPackage.rankDistributions) {
        const d = jsonPackage.rankDistributions;
        customerRanksDonutInstance.updateSeries([d.Dong || 0, d.Bac || 0, d.Vang || 0, d.KimCuong || 0]);
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