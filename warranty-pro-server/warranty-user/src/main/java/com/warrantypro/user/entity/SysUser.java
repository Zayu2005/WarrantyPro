package com.warrantypro.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户（表 sys_user）。 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 联系方式（非登录账号） */
    private String phone;

    private String passwordHash;

    private String realName;

    private String avatar;

    /** 1 启用 / 0 停用 */
    private Integer status;

    private LocalDateTime lastLoginAt;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
