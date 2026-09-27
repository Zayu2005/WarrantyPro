package com.warrantypro.user.dto;

/**
 * 我的房屋 VO：label 为「小区 楼栋 单元 房号」拼接，供报修表单直接展示。
 */
public record MyHouseVO(Long houseId, String label) {
}
