package com.warrantypro.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warrantypro.common.result.PageResult;
import com.warrantypro.common.result.Result;
import com.warrantypro.user.entity.Notice;
import com.warrantypro.user.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administrative notice directory. */
@RestController
@RequestMapping("/api/v1/admin/notices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminNoticeController {

    private final NoticeMapper noticeMapper;

    @GetMapping
    public Result<PageResult<Notice>> list(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long pageSize) {
        String trimmed = keyword == null ? "" : keyword.trim();
        long safePage = Math.max(1, page);
        long safePageSize = Math.min(100, Math.max(1, pageSize));
        Page<Notice> result = noticeMapper.selectPage(new Page<>(safePage, safePageSize),
                new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getDeleted, 0)
                        .eq(status != null && !status.isBlank(), Notice::getStatus, status)
                        .like(!trimmed.isEmpty(), Notice::getTitle, trimmed)
                        .orderByDesc(Notice::getId));
        return Result.ok(PageResult.of(result.getRecords(), result.getTotal(), safePage, safePageSize));
    }
}
