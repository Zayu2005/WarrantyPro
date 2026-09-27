package com.warrantypro.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 师傅排班（表 worker_schedule，管理员维护，派单候选过滤依据）。 */
@Data
@TableName("worker_schedule")
public class WorkerSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workerId;

    private LocalDate dutyDate;

    /** 班次：FULL 全天（预留早晚班扩展） */
    private String shift;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
