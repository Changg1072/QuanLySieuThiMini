let hrmMasterDataset = [];
let filteredWorkforcePartition = [];

let salesBarChartInstance = null;
let shiftDonutChartInstance = null;

let currentPagerPageIndex = 0;
const tablePageSizeLimit = 5;

document.addEventListener("DOMContentLoaded", function () {
    initializeGraphicalShellStructures();
});

function initializeGraphicalShellStructures() {
    // 🔥 ĐÃ SỬA: Cấu hình biểu đồ Cột Kép (Stacked/Grouped Bar)
    salesBarChartInstance = new ApexCharts(document.querySelector("#chartSalesByEmployee"), {
        series: [
            { name: 'Doanh Thu', data: [0, 0, 0, 0, 0] },
            { name: 'Chi Phí Lương', data: [0, 0, 0, 0, 0] }
        ],
        chart: { type: 'bar', height: 255, toolbar: { show: false }, fontFamily: 'Inter' },
        plotOptions: { bar: { horizontal: false, columnWidth: '55%', borderRadius: 4, dataLabels: { position: 'top' } } },
        dataLabels: { enabled: false },
        stroke: { show: true, width: 2, colors: ['transparent'] },
        colors: ['#10b981', '#f59e0b'], // Xanh ngọc cho Doanh Thu, Vàng Cam cho Lương
        xaxis: { categories: ['NV A', 'NV B', 'NV C', 'NV D', 'NV E'] },
        yaxis: { labels: { formatter: val => new Intl.NumberFormat('vi-VN').format(val) + "đ" } },
        fill: { opacity: 1 },
        tooltip: { y: { formatter: function (val) { return new Intl.NumberFormat('vi-VN').format(val) + " VNĐ" } } }
    });
    salesBarChartInstance.render();

    shiftDonutChartInstance = new ApexCharts(document.querySelector("#chartShiftDistribution"), {
        series: [1, 1, 1],
        chart: { type: 'donut', height: 255, fontFamily: 'Inter' },
        labels: ['Ca Sáng', 'Ca Chiều', 'Ca Tối'],
        colors: ['#38bdf8', '#0ea5e9', '#7c3aed'],
        legend: { position: 'bottom' }
    });
    shiftDonutChartInstance.render();
}

function updateWorkforceDashboard(payload) {
    if (!payload) return;

    // 1. Cập nhật thẻ KPI
    document.getElementById("txt-total-employees").innerText = payload.totalEmployees || 0;
    document.getElementById("txt-active-count").innerText = (payload.activeStaffCount || 0) + " đang hoạt động";
    document.getElementById("txt-global-revenue").innerText = new Intl.NumberFormat('vi-VN').format(payload.globalWorkforceRevenue || 0) + "đ";
    document.getElementById("txt-top-performer").innerText = "Top: " + payload.topPerformerName;
    
    // 🔥 ĐÃ SỬA: Nạp dữ liệu Quỹ lương thay vì Giờ công
    document.getElementById("txt-total-payroll").innerText = new Intl.NumberFormat('vi-VN').format(payload.totalPayroll || 0) + "đ";
    document.getElementById("txt-total-hours").innerText = "Dựa trên tổng " + (payload.totalWorkHours || 0) + "h công";
    
    document.getElementById("txt-late-count").innerText = (payload.totalLateOccurrences || 0) + " lượt";
    document.getElementById("txt-punctuality-rate").innerText = "Đúng giờ: " + (payload.punctualityRate || 100) + "%";

    // 2. Cập nhật biểu đồ kép Doanh Thu vs Lương
    if (payload.dualBarChart) {
        salesBarChartInstance.updateOptions({
            xaxis: { 
                categories: payload.dualBarChart.categories 
            },
            series: [
                { name: 'Doanh Thu', data: payload.dualBarChart.revenues },
                { name: 'Chi Phí Lương', data: payload.dualBarChart.salaries }
            ]
        }, false, true); // Thêm cờ ép biểu đồ tự động tính toán lại kích thước hiển thị
    }

    if (payload.shiftDistribution) {
        shiftDonutChartInstance.updateOptions({ labels: Object.keys(payload.shiftDistribution) });
        shiftDonutChartInstance.updateSeries(Object.values(payload.shiftDistribution));
    }

    // 3. AI Insights
    const insightsFeedBox = document.getElementById("insightsContainerTarget");
    insightsFeedBox.innerHTML = "";
    if (payload.workforceInsights && payload.workforceInsights.length > 0) {
        payload.workforceInsights.forEach(bulletText => {
            insightsFeedBox.innerHTML += `<div class="ai-insight-row-card">${bulletText}</div>`;
        });
    }

    // 4. Leaderboard
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
                    <div class="leader-cell-node" style="width:30%; text-align:right; font-weight:700; color:var(--emerald-success);">
                        ${new Intl.NumberFormat('vi-VN').format(staff.revenue)}đ
                        <div style="font-size:11px; color:#64748b; font-weight:400; margin-top:2px;">Hiệu suất: ${staff.efficiencyRate}%</div>
                    </div>
                </div>
            `;
        });
    }

    // 5. Timeline
    const timelineBox = document.getElementById("timelineContainerTarget");
    timelineBox.innerHTML = "";
    if (payload.shiftTimeline && payload.shiftTimeline.length > 0) {
        payload.shiftTimeline.forEach(log => {
            timelineBox.innerHTML += `
                <div class="timeline-event-card">
                    <span class="timeline-stamp-lbl">Ngày ${log.dateLabel} - Lịch trình ca: ${log.shiftId}</span>
                    <div class="timeline-main-desc">${log.staff} check-in</div>
                    <div style="font-size:12px; color:#475569;">Thời gian thực nhận diện: <b>${log.timeFrame}</b></div>
                </div>
            `;
        });
    }

    // 6. Datatable Binding
    if (payload.employeeGridMatrix) {
        hrmMasterDataset = payload.employeeGridMatrix;
        dispatchRealtimeFilterCoordinates();
    }
}

function removeVietnameseAccents(str) {
    if (!str) return "";
    return str.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d");
}

function dispatchRealtimeFilterCoordinates() {
    // 1. Lấy từ khóa từ giao diện (HTML)
    const rawSearchToken = document.getElementById("hrmSearchField").value;
    const cleanKeyword = removeVietnameseAccents(rawSearchToken.trim());
    const roleSelectorValue = document.getElementById("roleFilterBox").value;

    // 2. Lọc mảng dữ liệu gốc (hrmMasterDataset)
    filteredWorkforcePartition = hrmMasterDataset.filter(staff => {
        // Chuẩn hóa dữ liệu chống Null/Undefined để tránh sập bộ lọc
        const safeName = staff.name ? removeVietnameseAccents(staff.name) : "";
        const safeId = staff.id ? staff.id.toLowerCase() : "";
        const safePhone = staff.phone ? staff.phone.toLowerCase() : "";
        const safeRole = staff.role ? staff.role : "";

        // So khớp từ khóa (Tìm theo Tên, Mã NV hoặc SĐT)
        const matchesSearch = cleanKeyword === "" || 
                              safeName.includes(cleanKeyword) || 
                              safeId.includes(cleanKeyword) || 
                              safePhone.includes(cleanKeyword);
                              
        // So khớp Dropdown chức vụ
        const matchesRole = (roleSelectorValue === "ALL" || safeRole.includes(roleSelectorValue));
        
        return matchesSearch && matchesRole;
    });

    // 3. Reset phân trang về trang 1 và vẽ lại bảng
    currentPagerPageIndex = 0;
    compileDatatableDOMStructure();
}

function compileDatatableDOMStructure() {
    const tbody = document.getElementById("hrmTableRowsTarget");
    tbody.innerHTML = "";

    // Tính toán vị trí cắt mảng cho Phân trang
    const startIndex = currentPagerPageIndex * tablePageSizeLimit;
    const endIndex = startIndex + tablePageSizeLimit;
    const currentPartitionSegment = filteredWorkforcePartition.slice(startIndex, endIndex);

    // Cập nhật nhãn đếm "Hiển thị 1 - 5 của X nhân viên"
    const totalItems = filteredWorkforcePartition.length;
    const startLabel = totalItems > 0 ? startIndex + 1 : 0;
    const endLabel = Math.min(endIndex, totalItems);
    
    document.getElementById("txtPaginationDisplayLabel").innerText = 
        `Hiển thị ${startLabel} - ${endLabel} của ${totalItems} nhân sự`;

    // NẾU TÌM KHÔNG THẤY AI -> Báo rỗng
    if (currentPartitionSegment.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#64748b; padding:40px; font-size:14px;">🔍 Không tìm thấy nhân viên nào khớp với bộ lọc.</td></tr>`;
        return;
    }

    // NẾU CÓ DỮ LIỆU -> Vẽ từng dòng (Nối dây nút Hồ Sơ sang Java)
    currentPartitionSegment.forEach(s => {
        const penaltyStr = s.penalty > 0 ? '-' + new Intl.NumberFormat('vi-VN').format(s.penalty) + 'đ' : '0đ';
        
        tbody.innerHTML += `
            <tr>
                <td style="font-weight:600; color:var(--blue-premium-accent);">${s.id}</td>
                <td>
                    <b>${s.name}</b>
                    <div style="font-size:11px; color:#64748b; margin-top:2px;">Điện thoại: ${s.phone || '---'}</div>
                </td>
                <td><span style="font-weight:600; color:#475569;">${s.workHours}h</span></td>
                <td style="text-align:right; font-weight:700; color:var(--emerald-success);">${new Intl.NumberFormat('vi-VN').format(s.revenue)}đ</td>
                <td style="text-align:right; font-weight:700; color:var(--amber-warning);">${new Intl.NumberFormat('vi-VN').format(s.salary)}đ</td>
                <td style="text-align:right; font-weight:600; color:var(--rose-danger);">${penaltyStr}</td>
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