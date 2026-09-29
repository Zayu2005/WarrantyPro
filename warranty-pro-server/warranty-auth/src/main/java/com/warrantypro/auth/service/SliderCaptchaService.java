package com.warrantypro.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warrantypro.auth.dto.SliderCaptchaVO;
import com.warrantypro.common.exception.BizException;
import com.warrantypro.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SliderCaptchaService {

    private static final int WIDTH = 260;
    private static final int HEIGHT = 130;
    private static final int PIECE_SIZE = 44;
    private static final int MIN_X = 68;
    private static final int MAX_X = WIDTH - PIECE_SIZE - 8;
    private static final long TTL_SECONDS = 180;
    private static final int TOLERANCE = 5;

    private static final ObjectMapper JSON = new ObjectMapper();
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();

    /** 可选真实背景图库：启动时扫描 resources/captcha/ 下的 png/jpg，缓存为 BufferedImage。 */
    private final List<BufferedImage> backgroundImages = new ArrayList<>();

    public SliderCaptchaService() {
        loadBackgroundImages();
    }

    /** 多套配色，每次随机取一套，避免千篇一律的浅青渐变。 */
    private static final Color[][] PALETTES = {
        {new Color(255, 200, 150), new Color(255, 236, 179)},   // 晨光（暖橙→浅金）
        {new Color(135, 206, 235), new Color(176, 224, 255)},   // 晴空（天蓝→浅青）
        {new Color(126, 154, 151), new Color(228, 236, 234)},   // 山雾（灰青→米白）
        {new Color(180, 140, 210), new Color(240, 214, 250)},   // 霞紫（薰衣草→浅粉）
        {new Color(46, 109, 103),  new Color(198, 230, 224)},   // 松林（深苔绿→浅薄荷）
        {new Color(196, 71, 46),  new Color(255, 214, 165)},   // 晚霞（砖红→淡金）
        {new Color(59, 122, 157),  new Color(176, 216, 232)},   // 湖蓝（靛蓝→浅青）
        {new Color(100, 116, 138), new Color(210, 222, 235)},   // 晨雾（冷灰→浅蓝）
    };

    public SliderCaptchaVO issue() {
        cleanExpired();
        if (challenges.size() >= 20_000) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "验证码请求过于频繁，请稍后再试");
        }

        int targetX = ThreadLocalRandom.current().nextInt(MIN_X, MAX_X + 1);
        int targetY = ThreadLocalRandom.current().nextInt(32, HEIGHT - PIECE_SIZE - 8);
        BufferedImage cleanBackground = createBackground();
        BufferedImage source = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D copyGraphics = source.createGraphics();
        copyGraphics.drawImage(cleanBackground, 0, 0, null);
        copyGraphics.dispose();
        Area shape = createPieceShape();

        Graphics2D backgroundGraphics = source.createGraphics();
        configure(backgroundGraphics);
        backgroundGraphics.translate(targetX, targetY);
        backgroundGraphics.setColor(new Color(17, 42, 39, 190));
        backgroundGraphics.fill(shape);
        backgroundGraphics.setColor(new Color(255, 255, 255, 210));
        backgroundGraphics.setStroke(new BasicStroke(2f));
        backgroundGraphics.draw(shape);
        backgroundGraphics.dispose();

        BufferedImage piece = new BufferedImage(PIECE_SIZE, PIECE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pieceGraphics = piece.createGraphics();
        configure(pieceGraphics);
        pieceGraphics.setClip(shape);
        pieceGraphics.drawImage(cleanBackground.getSubimage(targetX, targetY, PIECE_SIZE, PIECE_SIZE), 0, 0, null);
        pieceGraphics.setClip(null);
        pieceGraphics.setColor(new Color(255, 255, 255, 225));
        pieceGraphics.setStroke(new BasicStroke(2f));
        pieceGraphics.draw(shape);
        pieceGraphics.dispose();

        String id = UUID.randomUUID().toString();
        challenges.put(id, new Challenge(targetX, Instant.now().plusSeconds(TTL_SECONDS), false));
        return new SliderCaptchaVO(id, dataUri(source), dataUri(piece), WIDTH, HEIGHT,
                targetY, PIECE_SIZE, MAX_X);
    }

    public void verify(String challengeId, int sliderOffset) {
        verify(challengeId, sliderOffset, null);
    }

    public void verify(String challengeId, int sliderOffset, String trajectory) {
        Challenge challenge = challenges.get(challengeId);
        if (challenge == null || challenge.expiresAt().isBefore(Instant.now())
                || challenge.verified()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "滑动验证失败或已过期，请刷新后重试");
        }
        if (Math.abs(challenge.targetX() - sliderOffset) > TOLERANCE) {
            challenges.remove(challengeId, challenge);
            throw new BizException(ErrorCode.PARAM_INVALID, "滑动验证失败或已过期，请刷新后重试");
        }
        // 可选轨迹特征：缺失 / 解析失败均视为通过（保持向后兼容与降级路径），
        // 仅在轨迹明显不像人类时拒绝。
        if (trajectory != null && !trajectory.isBlank()) {
            if (!looksLikeHumanTrajectory(trajectory)) {
                challenges.remove(challengeId, challenge);
                throw new BizException(ErrorCode.PARAM_INVALID, "滑动验证失败或已过期，请刷新后重试");
            }
        }
        if (!challenges.replace(challengeId, challenge,
                new Challenge(challenge.targetX(), challenge.expiresAt(), true))) {
            throw new BizException(ErrorCode.PARAM_INVALID, "滑动验证失败或已过期，请刷新后重试");
        }
    }

    /**
     * 轻量人类特征判断（不依赖 AI）：
     * <ul>
     *   <li>总时长 250ms ~ 6000ms（过快为脚本直冲，过慢为异常）</li>
     *   <li>采样点数 ≥ 4</li>
     *   <li>至少一次速度谷值（相对平均速度）或方向反转</li>
     * </ul>
     * 阈值有意宽松，避免误伤正常触屏/慢速拖动。
     */
    boolean looksLikeHumanTrajectory(String json) {
        // 无轨迹 / 空白：降级通过（向后兼容，旧客户端不传轨迹）
        if (json == null || json.isBlank()) {
            return true;
        }
        JsonNode root;
        try {
            root = JSON.readTree(json);
        } catch (Exception e) {
            // 解析失败：降级通过，不阻断登录（记录在案便于排查）
            return true;
        }
        if (root == null || root.isNull()) {
            return true;
        }
        JsonNode pointsNode = root.path("points");
        if (!pointsNode.isArray() || pointsNode.size() < 4) {
            // 合法 JSON 但点数不足：视为机器特征 → 拒绝
            return false;
        }
        long durationMs = root.path("durationMs").asLong(0);
        if (durationMs < 250 || durationMs > 6000) {
            return false;
        }
        int n = pointsNode.size();
        double[] xs = new double[n];
        int prevDirection = 0;
        boolean reversed = false;
        double totalDistance = 0;
        for (int i = 0; i < n; i++) {
            xs[i] = pointsNode.get(i).path(0).asDouble(0);
            if (i > 0) {
                int dir = Double.compare(xs[i], xs[i - 1]);
                totalDistance += Math.abs(xs[i] - xs[i - 1]);
                if (prevDirection > 0 && dir < 0) {
                    reversed = true;
                }
                prevDirection = dir;
            }
        }
        if (totalDistance < 0.05) {
            return false;
        }
        // 谷值：存在某段步进明显低于全程均值（宽松系数 0.5）
        double avgStep = totalDistance / (n - 1);
        boolean hasValley = false;
        for (int i = 1; i < n - 1; i++) {
            double stepBefore = Math.abs(xs[i] - xs[i - 1]);
            if (stepBefore < avgStep * 0.5) {
                hasValley = true;
                break;
            }
        }
        return reversed || hasValley;
    }

    public void consumeVerified(String challengeId) {
        Challenge challenge = challenges.remove(challengeId);
        if (challenge == null || !challenge.verified() || challenge.expiresAt().isBefore(Instant.now())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请先完成滑动验证");
        }
    }

    private BufferedImage createBackground() {
        // 60% 优先用真实图片（若 resources/captcha/ 已放置），否则程序化生成
        if (!backgroundImages.isEmpty() && ThreadLocalRandom.current().nextInt(100) < 60) {
            BufferedImage base = backgroundImages.get(ThreadLocalRandom.current().nextInt(backgroundImages.size()));
            BufferedImage scaled = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            configure(g);
            g.drawImage(base, 0, 0, WIDTH, HEIGHT, null);
            g.dispose();
            return scaled;
        }
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        configure(graphics);

        Color[] palette = PALETTES[ThreadLocalRandom.current().nextInt(PALETTES.length)];
        Color start = palette[0];
        Color end = palette[1];
        graphics.setPaint(new GradientPaint(0, 0, start, WIDTH, HEIGHT, end));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        // 随机叠加 2 种装饰层，让画面更"真实"
        int style = ThreadLocalRandom.current().nextInt(4);
        if (style == 0) {
            // 圆点（原有风格）
            for (int i = 0; i < 34; i++) {
                int x = ThreadLocalRandom.current().nextInt(WIDTH);
                int y = ThreadLocalRandom.current().nextInt(HEIGHT);
                int size = ThreadLocalRandom.current().nextInt(8, 34);
                graphics.setColor(new Color(255, 255, 255, ThreadLocalRandom.current().nextInt(25, 95)));
                graphics.fill(new Ellipse2D.Double(x, y, size, size));
            }
        } else if (style == 1) {
            // 斜线网格
            graphics.setStroke(new BasicStroke(1f));
            for (int i = -HEIGHT; i < WIDTH + HEIGHT; i += 12) {
                graphics.setColor(new Color(255, 255, 255, 40 + ThreadLocalRandom.current().nextInt(40)));
                graphics.drawLine(i, 0, i + HEIGHT, HEIGHT);
                graphics.setColor(new Color(0, 0, 0, 25));
                graphics.drawLine(i + 6, HEIGHT, i + HEIGHT + 6, 0);
            }
        } else if (style == 2) {
            // 色块拼接
            for (int i = 0; i < 8; i++) {
                int x = ThreadLocalRandom.current().nextInt(WIDTH);
                int y = ThreadLocalRandom.current().nextInt(HEIGHT);
                int w = 30 + ThreadLocalRandom.current().nextInt(50);
                int h = 20 + ThreadLocalRandom.current().nextInt(30);
                graphics.setColor(new Color(ThreadLocalRandom.current().nextInt(200, 255),
                        ThreadLocalRandom.current().nextInt(200, 255),
                        ThreadLocalRandom.current().nextInt(200, 255),
                        40 + ThreadLocalRandom.current().nextInt(50)));
                graphics.fill(new Rectangle2D.Double(x, y, w, h));
            }
        } else {
            // 同心圆 + 斜线
            int cx = WIDTH / 2 + ThreadLocalRandom.current().nextInt(30) - 15;
            int cy = HEIGHT / 2 + ThreadLocalRandom.current().nextInt(20) - 10;
            for (int r = 12; r < 90; r += 14) {
                graphics.setColor(new Color(255, 255, 255, 30 + (r / 4)));
                graphics.setStroke(new BasicStroke(1.2f));
                graphics.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            }
            for (int i = 0; i < 10; i++) {
                int x = ThreadLocalRandom.current().nextInt(WIDTH);
                int y = ThreadLocalRandom.current().nextInt(HEIGHT);
                graphics.setColor(new Color(0, 0, 0, 30));
                graphics.drawLine(x, y, Math.min(WIDTH, x + 30), Math.min(HEIGHT, y + 18));
            }
        }

        // 噪点纹理（让背景"脏"一点，更像真实照片）
        int noise = 120;
        for (int i = 0; i < noise; i++) {
            int x = ThreadLocalRandom.current().nextInt(WIDTH);
            int y = ThreadLocalRandom.current().nextInt(HEIGHT);
            graphics.setColor(new Color(ThreadLocalRandom.current().nextInt(255),
                    ThreadLocalRandom.current().nextInt(255),
                    ThreadLocalRandom.current().nextInt(255),
                    10 + ThreadLocalRandom.current().nextInt(20)));
            graphics.fill(new Rectangle2D.Double(x, y, 2, 2));
        }

        graphics.dispose();
        return image;
    }

    private void loadBackgroundImages() {
        var resources = java.util.Arrays.asList(
                "/captcha/bg1.png", "/captcha/bg2.png", "/captcha/bg3.png",
                "/captcha/bg4.png", "/captcha/bg5.png", "/captcha/bg6.png",
                "/captcha/bg1.jpg", "/captcha/bg2.jpg", "/captcha/bg3.jpg",
                "/captcha/bg4.jpg", "/captcha/bg5.jpg", "/captcha/bg6.jpg"
        );
        for (String path : resources) {
            try (var in = SliderCaptchaService.class.getResourceAsStream(path)) {
                if (in == null) continue;
                BufferedImage img = ImageIO.read(in);
                if (img != null && img.getWidth() >= WIDTH && img.getHeight() >= HEIGHT) {
                    backgroundImages.add(img);
                }
            } catch (IOException ignored) {
                // 图片缺失/损坏时静默跳过，回退程序化
            }
        }
    }

    private Area createPieceShape() {
        Area shape = new Area(new RoundRectangle2D.Double(0, 8, PIECE_SIZE - 8,
                PIECE_SIZE - 8, 9, 9));
        shape.add(new Area(new Ellipse2D.Double(15, 0, 17, 17)));
        return shape;
    }

    private void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private String dataUri(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("验证码图片生成失败", e);
        }
    }

    private void cleanExpired() {
        Instant now = Instant.now();
        challenges.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record Challenge(int targetX, Instant expiresAt, boolean verified) {
    }
}
