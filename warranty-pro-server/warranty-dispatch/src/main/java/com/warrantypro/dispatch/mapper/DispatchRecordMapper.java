package com.warrantypro.dispatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warrantypro.dispatch.dto.DispatchRecordVO;
import com.warrantypro.dispatch.entity.DispatchRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DispatchRecordMapper extends BaseMapper<DispatchRecord> {

    /** 派单记录（含师傅姓名，按轮次倒序）。 */
    @Select("""
            SELECT r.id AS id, r.round_no AS roundNo, r.mode AS mode,
                   r.worker_id AS workerId, u.real_name AS workerName,
                   r.score AS score, r.factors AS factors, r.reason AS reason,
                   r.status AS status,
                   DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i') AS createdAt
            FROM dispatch_record r
            LEFT JOIN sys_user u ON u.id = r.worker_id
            WHERE r.order_id = #{orderId}
            ORDER BY r.round_no DESC, r.id DESC
            """)
    List<DispatchRecordVO> selectVOByOrder(@Param("orderId") Long orderId);
}
