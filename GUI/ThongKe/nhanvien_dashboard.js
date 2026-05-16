let hrmMasterDataset = [];
let filteredWorkforcePartition = [];

let salesBarChartInstance = null;
let shiftDonutChartInstance = null;

// Datatable paging configuration metrics
let currentPagerPageIndex = 0;
const tablePageSizeLimit = 5;

document.addEventListener("DOMContentLoaded", function () {
    initializeGraphicalShellStructures();
});

function initializeGraphicalShellStructures() {
    // Render standard initial bar chart frames matching Java data arrays
    salesBarChartInstance = new ApexCharts(document.querySelector("#chartSalesByEmployee"), {
        series: [{ name: 'Doanh số tạo ra', data: [0, 0, 0, 0, 0] }],
        chart: { type: 'bar', height: 255, toolbar: { show: false }, fontFamily: 'Inter' },
        plotOptions: { bar: { columnWidth: '45%', borderRadius: 6 } },
        colors: ['#0284c7'],
        xaxis: { categories: ['Nhân viên A', 'Nhân viên B', 'Nhân viên C', 'Nhân viên D', 'Nhân viên E'] },
        yaxis: { labels: { formatter: val => new Intl.NumberFormat('vi-VN').format(val) + "đ" } }
    });
    salesBarChartInstance.render();

    // Render configuration metrics for shift segments donuts
    shiftDonutChartInstance = new ApexCharts(document.querySelector("#chartShiftDistribution"), {
        series: [1, 1, 1],
        chart: { type: 'donut', height: 255, fontFamily: 'Inter' },
        labels: ['Ca Sáng', 'Ca Chiều', 'Ca Tối'],
        colors: ['#38bdf8', '#0ea5e9', '#7c3aed'],
        legend: { position: 'bottom' }
    });
    shiftDonutChartInstance.render();
}

/**
 * HIGH-PERFORMANCE DATA INJECTION BRIDGE POINT EXECUTES FROM JAVA APPLICATION LOOP
 */
function updateWorkforceDashboard(payload) {
    if (!payload) return;

    // 1. Assign numeric nodes into Counter Metric Grid Card Elements
    document.getElementById("txt-total-employees").innerText = payload.totalEmployees || 0;
    document.getElementById("txt-active-count").innerText = (payload.activeStaffCount || 0) + " tài khoản đang hoạt động";
    document.getElementById("txt-global-revenue").innerText = new Intl.NumberFormat('vi-VN').format(payload.globalWorkforceRevenue || 0) + "đ";
    document.getElementById("txt-top-performer").innerText = "Top Performer: " + payload.topPerformerName;
    document.getElementById("txt-total-hours").innerText = (payload.totalWorkHours || 0) + "h";
    document.getElementById("txt-avg-hours").innerText = "Trung bình: " + (payload.averageHoursPerEmployee || 0) + "h / nhân sự";
    document.getElementById("txt-late-count").innerText = (payload.totalLateOccurrences || 0) + " lượt";
    document.getElementById("txt-punctuality-rate").innerText = "Tỷ lệ đúng giờ: " + (payload.punctualityRate || 100) + "%";

    // 2. Refresh dynamic chart arrays elements seamlessly
    if (payload.salesByEmployee) {
        const categories = Object.keys(payload.salesByEmployee);
        const dataValues = Object.values(payload.salesByEmployee);
        salesBarChartInstance.updateOptions({ xaxis: { categories: categories } });
        salesBarChartInstance.updateSeries([{ name: 'Doanh thu cá nhân', data: dataValues }]);
    }

    if (payload.shiftDistribution) {
        const donutLabels = Object.keys(payload.shiftDistribution);
        const donutSeries = Object.values(payload.shiftDistribution);
        shiftDonutChartInstance.updateOptions({ labels: donutLabels });
        shiftDonutChartInstance.updateSeries(donutSeries);
    }

    // 3. Render AI Intelligence Bulletin Rows
    const insightsFeedBox = document.getElementById("insightsContainerTarget");
    insightsFeedBox.innerHTML = "";
    if (payload.workforceInsights && payload.workforceInsights.length > 0) {
        payload.workforceInsights.forEach(bulletText => {
            insightsFeedBox.innerHTML += `<div class="ai-insight-row-card">${bulletText}</div>`;
        });
    } else {
        insightsFeedBox.innerHTML = '<div style="padding:10px;color:#64748b;font-size:13px;">✅ Chỉ số vận hành tối ưu. Không ghi nhận biến động nhân sự cấp bách.</div>';
    }

    // 4. Render Luxury MVP Leaderboard Rows
    const leaderboardBox = document.getElementById("leaderboardContainerTarget");
    leaderboardBox.innerHTML = "";
    if (payload.employeeLeaderboard && payload.employeeLeaderboard.length > 0) {
        payload.employeeLeaderboard.forEach((staff, index) => {
            const displayChar = staff.name ? staff.name.charAt(0) : "S";
            leaderboardBox.innerHTML += `
                <div class="leader-row-matrix-item">
                    <div class="leader-cell-node" style="width:10%; font-weight:700; font-size:14px; color:#64748b;">#${index+1}</div>
                    <div class="leader-cell-node" style="width:12%;"><div class="circle-initials-avatar">${displayChar}</div></div>
                    <div class="leader-cell-node" style="width:48%;">
                        <div style="font-weight:600;">${staff.name}</div>
                        <span class="badge-pill-tier">${staff.role}</span>
                    </div>
                    <div class="leader-cell-node" style="width:30%; text-align:right; font-weight:700; color:var(--blue-premium-accent);">
                        ${new Intl.NumberFormat('vi-VN').format(staff.revenue)}đ
                        <div style="font-size:11px; color:#64748b; font-weight:400; margin-top:2px;">Hiệu suất: ${staff.efficiencyRate}%</div>
                    </div>
                </div>
            `;
        });
    } else {
        leaderboardBox.innerHTML = '<div style="text-align:center;padding:40px;color:#64748b;">Chưa có chỉ số xếp hạng doanh thu kỳ này.</div>';
    }

    // 5. Append Realtime Shift Tracker Stream elements
    const timelineBox = document.getElementById("timelineContainerTarget");
    timelineBox.innerHTML = "";
    if (payload.shiftTimeline && payload.shiftTimeline.length > 0) {
        payload.shiftTimeline.forEach(log => {
            timelineBox.innerHTML += `
                <div class="timeline-event-card">
                    <span class="timeline-stamp-lbl">Ngày ${log.dateLabel} - Lịch trình ca làm: ${log.shiftId}</span>
                    <div class="timeline-main-desc">${log.staff} check-in</div>
                    <div style="font-size:12px; color:#475569;">Thời gian thực nhận diện: <b>${log.timeFrame}</b></div>
                </div>
            `;
        });
    } else {
        timelineBox.innerHTML = '<div style="text-align:center;padding:40px;color:#64748b;">Chưa có nhật ký check-in ca kíp gần đây.</div>';
    }

    // 6. Bind complete dataset memory rows for high performance offline search structures
    if (payload.employeeGridMatrix) {
        hrmMasterDataset = payload.employeeGridMatrix;
        dispatchRealtimeFilterCoordinates();
    }
}

// =========================================================================
// 🔥 DIACRITIC REMOVAL FILTERING TECHNIQUE (O(1) PROCESSING COMPLEXITY)
// =========================================================================
function removeVietnameseAccents(str) {
    if (!str) return "";
    return str.toLowerCase()
        .normalize("NFD")
        .replace(/[̀-ͯ]/g, "")
        .replace(/đ/g, "d");
}

function dispatchRealtimeFilterCoordinates() {
    const rawSearchToken = document.getElementById("hrmSearchField").value;
    const cleanKeyword = removeVietnameseAccents(rawSearchToken.trim());
    const roleSelectorValue = document.getElementById("roleFilterBox").value;

    filteredWorkforcePartition = hrmMasterDataset.filter(staff => {
        const normalizedName = removeVietnameseAccents(staff.name);
        const matchesSearch = normalizedName.includes(cleanKeyword) || staff.id.toLowerCase().includes(cleanKeyword) || staff.phone.includes(cleanKeyword);
        const matchesRole = (roleSelectorValue === "ALL" || staff.role.includes(roleSelectorValue));
        return matchesSearch && matchesRole;
    });

    currentPagerPageIndex = 0; // Back to initial segment frame index
    compileDatatableDOMStructure();
}

function compileDatatableDOMStructure() {
    const tbody = document.getElementById("hrmTableRowsTarget");
    tbody.innerHTML = "";

    const startIndex = currentPagerPageIndex * tablePageSizeLimit;
    const endIndex = startIndex + tablePageSizeLimit;
    const currentPartitionSegment = filteredWorkforcePartition.slice(startIndex, endIndex);

    // Update index trackers coordinates indicators text metrics
    document.getElementById("txtPaginationDisplayLabel").innerText = 
        `Hiển thị ${filteredWorkforcePartition.length > 0 ? startIndex + 1 : 0} - ${Math.min(endIndex, filteredWorkforcePartition.length)} của ${filteredWorkforcePartition.length} nhân sự`;

    if (currentPartitionSegment.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:#64748b;padding:30px;">Không tìm thấy kết quả đối soát nhân sự phù hợp.</td></tr>`;
        return;
    }

    currentPartitionSegment.forEach(s => {
        tbody.innerHTML += `
            <tr>
                <td style="font-weight:600; color:var(--blue-premium-accent);">${s.id}</td>
                <td><b>${s.name}</b><div style="font-size:11px; color:#64748b;">Số đi trễ: ${s.lateCount} lần</div></td>
                <td>${s.phone}</td>
                <td><span style="font-weight:500;">${s.role}</span></td>
                <td style="text-align:right; font-weight:600; color:#475569;">${s.workHours}h</td>
                <td style="text-align:right; font-weight:700; color:var(--text-slate-primary);">${new Intl.NumberFormat('vi-VN').format(s.revenue)}đ</td>
                <td style="text-align:right;">
                    <button class="micro-action-btn" onclick="alert('ACTION:VIEW_${s.id}')">Hồ sơ</button>
                </td>
            </tr>
        `;
    });
}

function adjustPagerPagePointer(direction) {
    const maximumPossiblePages = Math.ceil(filteredWorkforcePartition.length / tablePageSizeLimit);
    const calculatedNextIndex = currentPagerPageIndex + direction;

    if (calculatedNextIndex >= 0 && calculatedNextIndex < maximumPossiblePages) {
        currentPagerPageIndex = calculatedNextIndex;
        compileDatatableDOMStructure();
    }
}