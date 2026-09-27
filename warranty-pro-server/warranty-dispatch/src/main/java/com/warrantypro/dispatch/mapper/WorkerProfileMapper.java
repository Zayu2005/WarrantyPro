package com.warrantypro.dispatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warrantypro.dispatch.entity.WorkerProfile;
import com.warrantypro.dispatch.entity.CandidateWorker;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WorkerProfileMapper extends BaseMapper<WorkerProfile> {

    /** 当日值班且在岗的师傅（派单候选池）。 */
    @Select("""
            SELECT p.user_id AS userId, u.real_name AS realName,
                   p.skill_tags AS skillTags, p.community_id AS communityId,
                   p.max_concurrent AS maxConcurrent, p.rating_avg AS ratingAvg
            FROM worker_profile p
            JOIN sys_user u ON u.id = p.user_id AND u.deleted = 0 AND u.status = 1
            JOIN worker_schedule s ON s.worker_id = p.user_id AND s.duty_date = #{date}
            WHERE p.deleted = 0 AND p.on_duty = 1
            """)
    List<CandidateWorker> selectDutyWorkers(@Param("date") LocalDate date);

    /** 师傅列表（含全部在岗标记，排班管理页用）。 */
    @Select("""
            SELECT p.user_id AS userId, u.real_name AS realName,
                   p.skill_tags AS skillTags, p.community_id AS communityId,
                   p.max_concurrent AS maxConcurrent, p.rating_avg AS ratingAvg
            FROM worker_profile p
            JOIN sys_user u ON u.id = p.user_id AND u.deleted = 0
            WHERE p.deleted = 0
            ORDER BY p.user_id
            """)
    List<CandidateWorker> selectAllWorkers();
}
