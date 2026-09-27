package com.warrantypro.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warrantypro.user.dto.MyHouseVO;
import com.warrantypro.user.entity.UserHouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserHouseMapper extends BaseMapper<UserHouse> {

    @Select("""
            SELECT h.id AS houseId,
                   CONCAT(c.name, ' ', b.name, ' ', h.unit, ' 单元 ', h.room_no, ' 室') AS label
            FROM user_house uh
            JOIN house h ON h.id = uh.house_id AND h.deleted = 0
            JOIN building b ON b.id = h.building_id
            JOIN community c ON c.id = b.community_id
            WHERE uh.user_id = #{userId} AND uh.status = 'APPROVED'
            """)
    List<MyHouseVO> selectMyApprovedHouses(@Param("userId") Long userId);
}
