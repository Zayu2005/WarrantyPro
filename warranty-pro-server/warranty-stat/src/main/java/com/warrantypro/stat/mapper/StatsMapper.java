package com.warrantypro.stat.mapper;

import com.warrantypro.stat.dto.StatusCount;
import com.warrantypro.stat.dto.TrendPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StatsMapper {

    @Select("""
            SELECT status, COUNT(*) AS count
            FROM repair_order
            WHERE deleted = 0
            GROUP BY status
            ORDER BY status
            """)
    List<StatusCount> selectPipeline();

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS count
            FROM repair_order
            WHERE deleted = 0
              AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            ORDER BY date
            """)
    List<TrendPoint> selectRecentTrend();

    @Select("SELECT COUNT(*) FROM repair_order WHERE deleted = 0 AND created_at >= CURDATE()")
    long countTodayNew();

    @Select("SELECT COUNT(*) FROM repair_order WHERE deleted = 0 AND status = 'IN_PROGRESS'")
    long countInProgress();

    @Select("SELECT COUNT(*) FROM repair_order WHERE deleted = 0 AND status = 'PENDING_CONFIRM'")
    long countPendingConfirm();

    @Select("""
            SELECT COUNT(*) FROM repair_order
            WHERE deleted = 0 AND status = 'COMPLETED'
              AND confirmed_at >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
            """)
    long countMonthCompleted();
}
