package com.warrantypro.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warrantypro.order.entity.Evaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EvaluationMapper extends BaseMapper<Evaluation> {

    /** 原子更新师傅评分，避免并发评价覆盖累计值。 */
    @Update("""
            UPDATE worker_profile
            SET rating_avg = ROUND((COALESCE(rating_avg, 0) * COALESCE(rating_count, 0) + #{stars})
                                   / (COALESCE(rating_count, 0) + 1), 2),
                rating_count = COALESCE(rating_count, 0) + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_id = #{workerId} AND deleted = 0
            """)
    int appendRating(@Param("workerId") Long workerId, @Param("stars") int stars);

    @Update("""
            UPDATE worker_profile
            SET order_completed = COALESCE(order_completed, 0) + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_id = #{workerId} AND deleted = 0
            """)
    int incrementCompleted(@Param("workerId") Long workerId);
}
