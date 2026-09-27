package com.warrantypro.dispatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warrantypro.dispatch.entity.WorkerSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WorkerScheduleMapper extends BaseMapper<WorkerSchedule> {

    /** 区间内的排班（排班管理页格子）。 */
    @Select("""
            SELECT * FROM worker_schedule
            WHERE duty_date BETWEEN #{start} AND #{end}
            """)
    List<WorkerSchedule> selectRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
