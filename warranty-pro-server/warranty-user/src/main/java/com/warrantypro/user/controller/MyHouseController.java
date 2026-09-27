package com.warrantypro.user.controller;

import com.warrantypro.common.result.Result;
import com.warrantypro.common.security.LoginUser;
import com.warrantypro.user.dto.MyHouseVO;
import com.warrantypro.user.mapper.UserHouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 我的房屋（docs/07 §2.1，报修表单选房数据源）。 */
@RestController
@RequestMapping("/api/v1/houses")
@RequiredArgsConstructor
public class MyHouseController {

    private final UserHouseMapper userHouseMapper;

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('OWNER','WORKER')")
    public Result<List<MyHouseVO>> my(@AuthenticationPrincipal LoginUser user) {
        return Result.ok(userHouseMapper.selectMyApprovedHouses(user.userId()));
    }
}
