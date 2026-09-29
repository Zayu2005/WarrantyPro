package com.warrantypro.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 SliderCaptchaService 的轻量轨迹判定：
 * - 缺失/空轨迹：通过（向后兼容）
 * - 人类式轨迹（有停顿/方向反转）：通过
 * - 匀速直冲轨迹（无谷值无反转）：拒绝
 * - 时长过短：拒绝
 * - 点数不足：拒绝
 */
class SliderCaptchaServiceTest {

    private final SliderCaptchaService service = new SliderCaptchaService();

    @Test
    void nullOrBlankTrajectoryPasses() {
        assertTrue(service.looksLikeHumanTrajectory(null));
        assertTrue(service.looksLikeHumanTrajectory(""));
        assertTrue(service.looksLikeHumanTrajectory("   "));
    }

    @Test
    void malformedJsonPassesAsDegraded() {
        // 解析失败 → 降级通过（不阻断）
        assertTrue(service.looksLikeHumanTrajectory("not a json"));
    }

    @Test
    void validJsonButTooFewPointsRejected() {
        // 合法 JSON 但点数不足 → 拒绝
        assertFalse(service.looksLikeHumanTrajectory("{\"points\":[]}"));
        assertFalse(service.looksLikeHumanTrajectory("{\"durationMs\":1000,\"points\":[[0,0],[0.5,500],[1,1000]]}"));
    }

    @Test
    void humanLikeTrajectoryWithValleyPasses() {
        String traj = "{\"durationMs\":1420,\"points\":[[0,0],[0.02,40],[0.15,90],[0.2,120],[0.18,150],[0.35,210],[0.4,260],[0.38,300],[0.55,420],[0.6,540],[0.58,620],[0.8,900],[0.85,1100],[0.95,1300],[1,1420]]}";
        assertTrue(service.looksLikeHumanTrajectory(traj));
    }

    @Test
    void uniformRobotTrajectoryRejected() {
        // 完美匀速：无谷值、无反转
        String traj = "{\"durationMs\":1500,\"points\":[[0,0],[0.1,150],[0.2,300],[0.3,450],[0.4,600],[0.5,750],[0.6,900],[0.7,1050],[0.8,1200],[0.9,1350],[1,1500]]}";
        assertFalse(service.looksLikeHumanTrajectory(traj));
    }

    @Test
    void tooShortDurationRejected() {
        String traj = "{\"durationMs\":120,\"points\":[[0,0],[0.5,60],[1,120]]}";
        assertFalse(service.looksLikeHumanTrajectory(traj));
    }

    @Test
    void tooFewPointsRejected() {
        String traj = "{\"durationMs\":800,\"points\":[[0,0],[1,800]]}";
        assertFalse(service.looksLikeHumanTrajectory(traj));
    }

    @Test
    void noHorizontalProgressRejected() {
        // 总距离 < 0.05 → 拒绝
        String traj = "{\"durationMs\":1000,\"points\":[[0,0],[0.001,200],[0.002,400],[0.003,600],[0.004,800],[0.005,1000]]}";
        assertFalse(service.looksLikeHumanTrajectory(traj));
    }

    @Test
    void directionReversalPasses() {
        // 先快后慢且有一次回头（reversed=true）
        String traj = "{\"durationMs\":1200,\"points\":[[0,0],[0.3,300],[0.5,450],[0.4,600],[0.6,900],[0.9,1100],[1,1200]]}";
        assertTrue(service.looksLikeHumanTrajectory(traj));
    }
}
