let hrmMasterDataset = [];
let filteredWorkforcePartition = [];

let salesBarChartInstance = null;
let shiftDonutChartInstance = null;

let currentPagerPageIndex = 0;
const tablePageSizeLimit = 5;

let calViewYear  = new Date().getFullYear();
let calViewMonth = new Date().getMonth(); 
let calSelStart  = null; 
let calSelEnd    = null; 
let calPickStep  = 1;

document.addEventListener("DOMContentLoaded", function () {
    initializeGraphicalShellStructures();
    initDatePickers(); // <--- GỌI KHỞI TẠO LỊCH Ở ĐÂY
});
function initDatePickers() {
    const today = new Date();
    calViewYear  = today.getFullYear();
    calViewMonth = today.getMonth();
    // Mặc định: từ đầu tháng đến hôm nay
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

    const hint = document.getElementById("calSelectionHint");
    hint.innerText = (calPickStep === 1) ? "Chọn ngày bắt đầu" : "Chọn ngày kết thúc";
}

function onCalDayClick(date) {
    if (calPickStep === 1) {
        calSelStart = date;
        calSelEnd   = null;
        calPickStep = 2;
    } else {
        if (date < calSelStart) {
            calSelEnd   = calSelStart;
            calSelStart = date;
        } else {
            calSelEnd = date;
        }
        calPickStep = 1;
    }
    renderCalendarGrid();
    updateDateRangeLabel();
}

function updateDateRangeLabel() {
    const label = document.getElementById("dateRangeDisplayLabel");
    if (calSelStart && calSelEnd) {
        label.innerText = fmtDate(calSelStart) + "  →  " + fmtDate(calSelEnd);
    } else if (calSelStart) {
        label.innerText = fmtDate(calSelStart) + "  →  ...";
    } else {
        label.innerText = "Chọn khoảng thời gian...";
    }
}

function fmtDate(d) {
    return String(d.getDate()).padStart(2,'0') + '/' + String(d.getMonth()+1).padStart(2,'0') + '/' + d.getFullYear();
}

function fmtDateISO(d) {
    return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0');
}

function confirmDateRange() {
    if (!calSelStart || !calSelEnd) {
        document.getElementById("calSelectionHint").innerText = "Vui lòng chọn đủ 2 ngày!";
        return;
    }
    document.getElementById("inlineCalendarPanel").style.display = "none";
    
    // Gửi tín hiệu báo cho Java biết ngày đã thay đổi
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
function initializeGraphicalShellStructures() {
    salesBarChartInstance = new ApexCharts(document.querySelector("#chartSalesByEmployee"), {
        series: [
            { name: 'Doanh Thu', data: [0, 0, 0, 0, 0] },
            { name: 'Chi Phí Lương', data: [0, 0, 0, 0, 0] }
        ],
        chart: { type: 'bar', height: 255, toolbar: { show: false }, fontFamily: 'Inter' },
        plotOptions: { bar: { horizontal: false, columnWidth: '55%', borderRadius: 4, dataLabels: { position: 'top' } } },
        dataLabels: { enabled: false },
        stroke: { show: true, width: 2, colors: ['transparent'] },
        colors: ['#10b981', '#f59e0b'],
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

    document.getElementById("txt-total-employees").innerText = payload.totalEmployees || 0;
    document.getElementById("txt-active-count").innerText = (payload.activeStaffCount || 0) + " đang hoạt động";
    document.getElementById("txt-global-revenue").innerText = new Intl.NumberFormat('vi-VN').format(payload.globalWorkforceRevenue || 0) + "đ";
    document.getElementById("txt-top-performer").innerText = "Top: " + payload.topPerformerName;
    document.getElementById("txt-total-payroll").innerText = new Intl.NumberFormat('vi-VN').format(payload.totalPayroll || 0) + "đ";
    document.getElementById("txt-total-hours").innerText = "Dựa trên tổng " + (payload.totalWorkHours || 0) + "h công";
    document.getElementById("txt-late-count").innerText = (payload.totalLateOccurrences || 0) + " lượt";
    document.getElementById("txt-punctuality-rate").innerText = "Đúng giờ: " + (payload.punctualityRate || 100) + "%";

    if (payload.dualBarChart) {
        salesBarChartInstance.updateOptions({
            xaxis: { categories: payload.dualBarChart.categories },
            series: [
                { name: 'Doanh Thu', data: payload.dualBarChart.revenues },
                { name: 'Chi Phí Lương', data: payload.dualBarChart.salaries }
            ]
        }, false, true);
    }

    if (payload.shiftDistribution) {
        shiftDonutChartInstance.updateOptions({ labels: Object.keys(payload.shiftDistribution) });
        shiftDonutChartInstance.updateSeries(Object.values(payload.shiftDistribution));
    }

    const insightsFeedBox = document.getElementById("insightsContainerTarget");
    insightsFeedBox.innerHTML = "";
    if (payload.workforceInsights && payload.workforceInsights.length > 0) {
        payload.workforceInsights.forEach(bulletText => {
            insightsFeedBox.innerHTML += `<div class="ai-insight-row-card">${bulletText}</div>`;
        });
    }

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

    if (payload.employeeGridMatrix) {
        hrmMasterDataset = payload.employeeGridMatrix;
        dispatchRealtimeFilterCoordinates();
    }

    // ✅ Reset về chế độ Live sau khi nhận dữ liệu mới
    var caption = document.querySelector(".header-sub-caption");
    if (caption && !caption.dataset.isHistorical) {
        caption.innerText = "Hệ thống phân tích chấm công, theo dõi hiệu suất và quỹ lương thời gian thực";
        caption.style.color = "";
        caption.style.fontWeight = "";
    }
}

function removeVietnameseAccents(str) {
    if (!str) return "";
    return str.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d");
}

function dispatchRealtimeFilterCoordinates() {
    const rawSearchToken = document.getElementById("hrmSearchField").value;
    const cleanKeyword = removeVietnameseAccents(rawSearchToken.trim());
    const roleSelectorValue = document.getElementById("roleFilterBox").value;

    filteredWorkforcePartition = hrmMasterDataset.filter(staff => {
        const safeName = staff.name ? removeVietnameseAccents(staff.name) : "";
        const safeId = staff.id ? staff.id.toLowerCase() : "";
        const safePhone = staff.phone ? staff.phone.toLowerCase() : "";
        const safeRole = staff.role ? staff.role : "";
        const safeStatus = staff.status ? staff.status : "Đang Làm Việc";

        const matchesSearch = cleanKeyword === "" || 
                              safeName.includes(cleanKeyword) || 
                              safeId.includes(cleanKeyword) || 
                              safePhone.includes(cleanKeyword);
                              
        let matchesRole = false;
        if (roleSelectorValue === "ALL") {
            matchesRole = true;
        } else if (roleSelectorValue === "Đã Nghỉ") {
            matchesRole = (safeStatus === "Đã Nghỉ");
        } else {
            matchesRole = (safeRole === roleSelectorValue && safeStatus !== "Đã Nghỉ");
        }
        
        return matchesSearch && matchesRole;
    });

    currentPagerPageIndex = 0;
    compileDatatableDOMStructure();
    updateFilteredKPIsAndCharts();
}

function compileDatatableDOMStructure() {
    const tbody = document.getElementById("hrmTableRowsTarget");
    tbody.innerHTML = "";

    const startIndex = currentPagerPageIndex * tablePageSizeLimit;
    const endIndex = startIndex + tablePageSizeLimit;
    const currentPartitionSegment = filteredWorkforcePartition.slice(startIndex, endIndex);

    const totalItems = filteredWorkforcePartition.length;
    const startLabel = totalItems > 0 ? startIndex + 1 : 0;
    const endLabel = Math.min(endIndex, totalItems);
    
    document.getElementById("txtPaginationDisplayLabel").innerText = 
        `Hiển thị ${startLabel} - ${endLabel} của ${totalItems} nhân sự`;

    if (currentPartitionSegment.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#64748b; padding:40px; font-size:14px;">🔍 Không tìm thấy nhân viên nào khớp với bộ lọc.</td></tr>`;
        return;
    }

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

function updateFilteredKPIsAndCharts() {
    let totalRevenue = 0;
    let totalPayroll = 0;
    let totalLate = 0;
    let topPerformerName = "--";
    let highestRev = -1;

    filteredWorkforcePartition.forEach(s => {
        totalRevenue += (s.revenue || 0);
        totalPayroll += (s.salary || 0);
        totalLate += (s.lateCount || 0);
        if ((s.revenue || 0) > highestRev) {
            highestRev = s.revenue;
            topPerformerName = s.name;
        }
    });

    document.getElementById("txt-active-count").innerText = filteredWorkforcePartition.length + " nhân sự";
    document.getElementById("txt-global-revenue").innerText = new Intl.NumberFormat('vi-VN').format(totalRevenue) + "đ";
    document.getElementById("txt-total-payroll").innerText = new Intl.NumberFormat('vi-VN').format(totalPayroll) + "đ";
    document.getElementById("txt-late-count").innerText = totalLate + " lượt";
    document.getElementById("txt-top-performer").innerText = highestRev > 0 ? "Top: " + topPerformerName : "Top: --";

    let sortedBySales = [...filteredWorkforcePartition].sort((a, b) => (b.revenue || 0) - (a.revenue || 0));
    let top5 = sortedBySales.slice(0, 5);

    let chartCategories = top5.length > 0 ? top5.map(s => s.name) : ["Không có DL"];
    let chartRevenues = top5.length > 0 ? top5.map(s => s.revenue) : [0];
    let chartSalaries = top5.length > 0 ? top5.map(s => s.salary) : [0];

    salesBarChartInstance.updateOptions({ xaxis: { categories: chartCategories } });
    salesBarChartInstance.updateSeries([
        { name: 'Doanh Thu', data: chartRevenues },
        { name: 'Chi Phí Lương', data: chartSalaries }
    ]);
}

// =========================================================================
// 🔥 XUẤT FILE VÀ PHỤC DỰNG LỊCH SỬ THỐNG KÊ
// =========================================================================

/**
 * Gọi Java xuất file. Kiểm tra javaConnector bằng nhiều cách để đảm bảo ổn định.
 */
function triggerSystemExport() {
    if (typeof window.javaConnector !== 'undefined' && window.javaConnector !== null) {
        try {
            // Lấy khoảng thời gian từ lịch để đặt tên file
            let fileName = "Thong_Ke_Nhan_Vien";
            if (typeof calSelStart !== 'undefined' && calSelStart && calSelEnd) {
                const s = fmtDate(calSelStart).replace(/\//g, "_");
                const e = fmtDate(calSelEnd).replace(/\//g, "_");
                fileName = `Thong_Ke_Nhan_Vien_Tu_${s}_Den_${e}.json`;
            } else {
                fileName += "_" + new Date().getTime() + ".json";
            }
            
            // Gửi đúng 1 tham số tên file sang Java
            window.javaConnector.exportDashboardData(fileName);
            
        } catch (err) {
            alert("⚠️ LỖI KHI GỌI JAVA:\n" + err.message);
        }
    } else {
        alert("⚠️ CHƯA KẾT NỐI VỚI JAVA BACKEND! Hãy đợi trang load xong.");
    }
}
/**
 * Load danh sách file lịch sử vào dropdown.
 * Được gọi từ Java sau khi trang load và sau mỗi lần xuất file.
 */
function loadExportHistory() {
    if (typeof window.javaConnector === 'undefined' || window.javaConnector === null) return;
    
    try {
        let historyJson = window.javaConnector.getExportHistoryList();
        let files = JSON.parse(historyJson);
        
        let selector = document.getElementById("historySelector");
        selector.options.length = 0; // Xóa sạch danh sách cũ

        let defaultOpt = document.createElement("option");
        defaultOpt.value = "";
        defaultOpt.textContent = "Lịch sử xuất file";
        selector.appendChild(defaultOpt);

        // Nút thoát chỉ hiện khi đang xem lịch sử
        var caption = document.querySelector(".header-sub-caption");
        if (caption && caption.dataset.isHistorical === "true") {
            let exitOpt = document.createElement("option");
            exitOpt.value = "__EXIT_HISTORY__";
            exitOpt.textContent = "Thoát - Quay về dữ liệu thực";
            selector.appendChild(exitOpt);
        }

        files.forEach(function(f) {
            let displayName = f;
            // Trích xuất ngày từ tên file: Thong_Ke_Nhan_Vien_Tu_DD_MM_YYYY_Den_DD_MM_YYYY.json
            let match = f.match(/Tu_(\d{2})_(\d{2})_(\d{4})_Den_(\d{2})_(\d{2})_(\d{4})/);
            if (match) {
                displayName = `[${match[1]}/${match[2]}/${match[3]} - ${match[4]}/${match[5]}/${match[6]}] Báo Cáo Nhân Viên`;
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

function loadHistoricalData(fileName) {
    if (!fileName || fileName === "") return;

    if (fileName === "__EXIT_HISTORY__") {
        exitHistoryMode();
        return;
    }

    if (typeof window.javaConnector === 'undefined' || window.javaConnector === null) return;
    try {
        window.javaConnector.readAndLoadExportFile(fileName);
    } catch(e) {
        alert("⚠️ Lỗi tải file lịch sử: " + e.message);
    }
}
function exitHistoryMode() {
    // 🔥 1. RESET CALENDAR VỀ MẶC ĐỊNH (Đầu tháng -> Hôm nay)
    const today = new Date();
    calSelStart = new Date(today.getFullYear(), today.getMonth(), 1);
    calSelEnd   = new Date(today);
    calViewYear  = today.getFullYear();
    calViewMonth = today.getMonth();
    calPickStep  = 1;
    updateDateRangeLabel();

    // 2. Trả lại tiêu đề mặc định
    var caption = document.querySelector(".header-sub-caption");
    if (caption) {
        delete caption.dataset.isHistorical;
        caption.innerText = "Hệ thống phân tích chấm công, theo dõi hiệu suất và quỹ lương thời gian thực";
        caption.style.color = "";
        caption.style.fontWeight = "";
    }

    // 3. Reset Dropdown
    var selector = document.getElementById("historySelector");
    if (selector) selector.value = "";
    loadExportHistory(); 

    // 4. Đồng bộ lại dữ liệu Database
    alert("ACTION:SYNC");
}
function applyHistoricalStateBase64(base64Data, fileName) {
    try {
        let binaryStr = atob(base64Data);
        let bytes = new Uint8Array(binaryStr.length);
        for (let i = 0; i < binaryStr.length; i++) bytes[i] = binaryStr.charCodeAt(i);
        let decodedString = new TextDecoder('utf-8').decode(bytes);
        
        // Bơm dữ liệu JSON lên Dashboard
        let historicalPayload = JSON.parse(decodedString);
        updateWorkforceDashboard(historicalPayload);
        
        let dateDisplay = fileName;
        let match = fileName.match(/Tu_(\d{2})_(\d{2})_(\d{4})_Den_(\d{2})_(\d{2})_(\d{4})/);

        if (match) {
            // 🔥 ÉP LỊCH TRÊN GIAO DIỆN NHẢY THEO NGÀY CỦA FILE LỊCH SỬ
            calSelStart = new Date(parseInt(match[3]), parseInt(match[2]) - 1, parseInt(match[1]));
            calSelEnd   = new Date(parseInt(match[6]), parseInt(match[5]) - 1, parseInt(match[4]));
            calViewYear  = calSelStart.getFullYear();
            calViewMonth = calSelStart.getMonth();
            calPickStep  = 1;
            updateDateRangeLabel();

            dateDisplay = `${match[1]}/${match[2]}/${match[3]} → ${match[4]}/${match[5]}/${match[6]}`;
        }

        // Cập nhật tiêu đề cảnh báo
        var caption = document.querySelector(".header-sub-caption");
        if (caption) {
            caption.dataset.isHistorical = "true";
            caption.textContent = "[CHẾ ĐỘ LỊCH SỬ] Báo cáo: " + dateDisplay + " -- Chọn 'Thoát' để về hiện tại";
            caption.style.color = "#ef4444";
            caption.style.fontWeight = "bold";
        }

        loadExportHistory();

    } catch(e) {
        alert("❌ Lỗi giải mã file lịch sử: " + e.message);
    }
}