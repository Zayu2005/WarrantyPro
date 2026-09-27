package com.warrantypro.bootstrap.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warrantypro.dispatch.entity.WorkerProfile;
import com.warrantypro.dispatch.entity.WorkerSchedule;
import com.warrantypro.dispatch.mapper.WorkerProfileMapper;
import com.warrantypro.dispatch.mapper.WorkerScheduleMapper;
import com.warrantypro.estate.entity.Building;
import com.warrantypro.estate.entity.Community;
import com.warrantypro.estate.entity.Facility;
import com.warrantypro.estate.entity.House;
import com.warrantypro.estate.mapper.BuildingMapper;
import com.warrantypro.estate.mapper.CommunityMapper;
import com.warrantypro.estate.mapper.FacilityMapper;
import com.warrantypro.estate.mapper.HouseMapper;
import com.warrantypro.user.entity.SysUser;
import com.warrantypro.user.entity.SysUserRole;
import com.warrantypro.user.entity.UserHouse;
import com.warrantypro.user.mapper.SysRoleMapper;
import com.warrantypro.user.mapper.SysUserMapper;
import com.warrantypro.user.mapper.SysUserRoleMapper;
import com.warrantypro.user.mapper.UserHouseMapper;
import com.warrantypro.user.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 演示数据初始化（幂等：以手机号判存）。
 *
 * <p>数据设计让保修判定两条分支都能演示：3 栋竣工 2024-06-30，
 * 水电类（2 年）已过保、土建防水（5 年）仍在保修期内；电梯设施台账 2025~2027 在保。</p>
 *
 * <p>演示账号（密码统一 123456，仅限本地/演示环境）：</p>
 * <ul>
 *   <li>admin   系统管理员（ADMIN）</li>
 *   <li>kefu    客服小王（DISPATCHER）</li>
 *   <li>shifu   张建国师傅（WORKER）</li>
 *   <li>owner   李雷（OWNER，已绑定 3 栋 1 单元 101）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final UserHouseMapper userHouseMapper;
    private final NoticeMapper noticeMapper;
    private final WorkerProfileMapper workerProfileMapper;
    private final WorkerScheduleMapper workerScheduleMapper;
    private final CommunityMapper communityMapper;
    private final BuildingMapper buildingMapper;
    private final HouseMapper houseMapper;
    private final FacilityMapper facilityMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "admin")) > 0) {
            return;
        }

        // 房产
        Community community = new Community();
        community.setName("幸福家园小区");
        community.setAddress("示例市幸福路 88 号");
        community.setDeveloperName("宏图地产开发有限公司");
        community.setContact("13800009999");
        communityMapper.insert(community);

        Building building = new Building();
        building.setCommunityId(community.getId());
        building.setName("3栋");
        building.setCompletionDate(LocalDate.of(2024, 6, 30));
        buildingMapper.insert(building);

        House house = new House();
        house.setBuildingId(building.getId());
        house.setUnit("1");
        house.setRoomNo("101");
        house.setArea(new java.math.BigDecimal("89.50"));
        houseMapper.insert(house);

        Facility elevator = new Facility();
        elevator.setCommunityId(community.getId());
        elevator.setName("3 栋 1 号电梯");
        elevator.setType("ELEVATOR");
        elevator.setLocation("3 栋 1 单元");
        elevator.setSupplierName("快速电梯有限公司");
        elevator.setContactName("陈工");
        elevator.setContactPhone("13800007777");
        elevator.setInstallDate(LocalDate.of(2025, 1, 1));
        elevator.setWarrantyStart(LocalDate.of(2025, 1, 1));
        elevator.setWarrantyEnd(LocalDate.of(2027, 1, 1));
        facilityMapper.insert(elevator);

        // 账号
        Long admin = createUser("admin", "13800000001", "系统管理员", "ADMIN");
        createUser("kefu", "13800000002", "客服小王", "DISPATCHER");
        Long worker1 = createUser("shifu", "13800000003", "张建国", "WORKER");
        Long owner = createUser("owner", "13800000004", "李雷", "OWNER");
        Long worker2 = createUser("wangqiang", "13800000005", "王强", "WORKER");

        // 师傅画像（技能标签为 FaultCategory 中文口径）+ 今明两天排班
        insertWorkerProfile(worker1, "[\"水电\",\"土建防水\",\"暖通空调\"]", 4.80);
        insertWorkerProfile(worker2, "[\"水电\",\"门窗五金\"]", 4.60);
        insertSchedules(worker1);
        insertSchedules(worker2);

        // 业主绑定房屋（直接审核通过）
        UserHouse userHouse = new UserHouse();
        userHouse.setUserId(owner);
        userHouse.setHouseId(house.getId());
        userHouse.setRelation("OWNER");
        userHouse.setStatus("APPROVED");
        userHouse.setAuditedBy(admin);
        userHouseMapper.insert(userHouse);

        // 演示公告（鸿蒙端"服务"Tab 展示）
        insertNotice(community.getId(), "停水通知", "本周六 09:00-17:00 小区管网检修，3 栋暂停供水，请提前储水。", "停水");
        insertNotice(community.getId(), "电梯维保公告", "3 栋 1 号电梯将于本周日进行季度维保，维保期间请乘坐 2 号电梯。", "维保");
        insertNotice(community.getId(), "屋面防水普查", "小区将开展屋面防水专项普查，如发现渗漏请及时在 App 报修。", "其他");

        log.info("演示数据初始化完成：5 个账号（密码 123456）、小区/楼栋/房屋/电梯台账、3 条公告、2 位师傅画像与排班");
    }

    private void insertWorkerProfile(Long userId, String skillTags, double rating) {
        WorkerProfile profile = new WorkerProfile();
        profile.setUserId(userId);
        profile.setSkillTags(skillTags);
        profile.setMaxConcurrent(3);
        profile.setOnDuty(1);
        profile.setRatingAvg(java.math.BigDecimal.valueOf(rating));
        profile.setRatingCount(0);
        profile.setOrderTotal(0);
        profile.setOrderCompleted(0);
        workerProfileMapper.insert(profile);
    }

    private void insertSchedules(Long workerId) {
        for (int offset = 0; offset < 2; offset++) {
            WorkerSchedule schedule = new WorkerSchedule();
            schedule.setWorkerId(workerId);
            schedule.setDutyDate(java.time.LocalDate.now().plusDays(offset));
            schedule.setShift("FULL");
            workerScheduleMapper.insert(schedule);
        }
    }

    private void insertNotice(Long communityId, String title, String content, String type) {
        com.warrantypro.user.entity.Notice notice = new com.warrantypro.user.entity.Notice();
        notice.setCommunityId(communityId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setType(type);
        notice.setStatus("PUBLISHED");
        noticeMapper.insert(notice);
    }

    private Long createUser(String username, String phone, String realName, String roleCode) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setRealName(realName);
        user.setStatus(1);
        sysUserMapper.insert(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(sysRoleMapper.selectOne(new LambdaQueryWrapper<com.warrantypro.user.entity.SysRole>()
                .eq(com.warrantypro.user.entity.SysRole::getCode, roleCode)).getId());
        sysUserRoleMapper.insert(userRole);
        return user.getId();
    }
}
