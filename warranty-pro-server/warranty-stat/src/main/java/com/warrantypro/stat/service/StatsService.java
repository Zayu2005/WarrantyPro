package com.warrantypro.stat.service;

import com.warrantypro.stat.dto.OverviewStats;
import com.warrantypro.stat.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;

    public OverviewStats overview() {
        return new OverviewStats(
                statsMapper.selectPipeline(),
                statsMapper.selectRecentTrend(),
                statsMapper.countTodayNew(),
                statsMapper.countInProgress(),
                statsMapper.countPendingConfirm(),
                statsMapper.countMonthCompleted());
    }
}
