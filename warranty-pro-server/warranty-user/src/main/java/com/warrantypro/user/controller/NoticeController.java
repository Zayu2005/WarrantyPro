package com.warrantypro.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warrantypro.common.result.Result;
import com.warrantypro.user.entity.Notice;
import com.warrantypro.user.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 物业公告（docs/02 FR-O-10）。 */
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeMapper noticeMapper;

    @GetMapping
    public Result<List<Notice>> list() {
        return Result.ok(noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, "PUBLISHED")
                .orderByDesc(Notice::getId)
                .last("LIMIT 20")));
    }
}
