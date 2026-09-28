package com.warrantypro.stat.dto;

import java.util.List;

public record OverviewStats(
        List<StatusCount> pipeline,
        List<TrendPoint> trend,
        long todayNew,
        long inProgress,
        long pendingConfirm,
        long monthCompleted
) {
}
