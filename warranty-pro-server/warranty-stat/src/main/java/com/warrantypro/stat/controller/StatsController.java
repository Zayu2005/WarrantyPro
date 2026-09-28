package com.warrantypro.stat.controller;

import com.warrantypro.common.result.Result;
import com.warrantypro.stat.dto.OverviewStats;
import com.warrantypro.stat.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 运营看板统计接口。 */
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','ADMIN')")
    public Result<OverviewStats> overview() {
        return Result.ok(statsService.overview());
    }
}
