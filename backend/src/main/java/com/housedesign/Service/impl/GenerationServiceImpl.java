package com.housedesign.Service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.housedesign.Service.FileStorageService;
import com.housedesign.Service.FloorPlanAnalysisService;
import com.housedesign.Service.GenerationService;
import com.housedesign.common.BusinessException;
import com.housedesign.common.UserContext;
import com.housedesign.dto.ai.FloorPlanResult;
import com.housedesign.dto.ai.RoomPlan;
import com.housedesign.dto.response.GenerationResponse;
import com.housedesign.dto.scene.Hotspot;
import com.housedesign.dto.scene.PhotoTourScene;
import com.housedesign.dto.scene.RoomScene;
import com.housedesign.entity.DesignProject;
import com.housedesign.entity.DesignStyle;
import com.housedesign.entity.GeneratedModel;
import com.housedesign.entity.GenerationStatus;
import com.housedesign.mapper.GeneratedModelMapper;
import com.housedesign.mapper.ProjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class GenerationServiceImpl implements GenerationService {

    /** 热点圆点在房间画面中的固定垂直位置（门洞大致高度） */
    private static final double HOTSPOT_Y = 0.66;

    /** 左/中/右三档门方位对应的水平基准位置 */
    private static final double X_LEFT = 0.24;
    private static final double X_CENTER = 0.50;
    private static final double X_RIGHT = 0.76;

    /** 同项目生成中任务的 Redis 占位 key 前缀（原子 SETNX 防并发，替代有竞态的 selectCount 检查） */
    private static final String ACTIVE_KEY_PREFIX = "generation:active:";

    /**
     * 占位 TTL 兜底：正常路径任务终态时主动释放；
     * JVM 崩溃/线程被杀导致未释放时，30 分钟后自动放行，避免项目被永久锁死。
     */
    private static final Duration ACTIVE_KEY_TTL = Duration.ofMinutes(30);

    private final ThreadPoolTaskExecutor generationExecutor;
    private final GeneratedModelMapper generatedModelMapper;
    private final ProjectMapper projectMapper;
    private final FileStorageService fileStorageService;
    private final AIimageService aIimageService;
    private final FloorPlanAnalysisService floorPlanAnalysisService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    // 工具方法

    // 实体->响应体
    private GenerationResponse toResponse(GeneratedModel model) {
        GenerationResponse resp = new GenerationResponse();
        resp.setId(model.getId());
        resp.setProjectId(model.getProjectId());
        resp.setStatus(model.getStatus());
        resp.setProvider(model.getProvider());
        resp.setModelUrl(model.getModelUrl());
        resp.setPreviewImageUrl(model.getPreviewImageUrl());
        resp.setPanoramaUrl(model.getPanoramaUrl());
        resp.setSceneConfig(model.getSceneConfig());
        resp.setErrorMessage(model.getErrorMessage());
        resp.setCreatedAt(model.getCreatedAt());
        resp.setUpdatedAt(model.getUpdatedAt());
        return resp;
    }

    // 发起生成
    @Override
    public GenerationResponse generateRequest(Long projectId) {
        // 当前用户id
        Long userId = UserContext.getUserId();
        log.info("发起生成：projectId={}，userId={}", projectId, userId);

        // 1.校验项目是否属于本人
        DesignProject designProject = projectMapper.selectById(projectId);
        if (designProject == null || !designProject.getUserId().equals(userId)) {
            log.warn("发起生成失败或不是你的项目！:projectId = {},userId = {}", projectId, userId);
            throw new BusinessException(404, "项目不存在！");
        }
        // 2.校验项目是否有设计图
        if (designProject.getDesignImageUrl() == null || designProject.getDesignImageUrl().isBlank()) {
            log.warn("发起生成失败，暂无设计图：projectId={},userId={}", projectId, userId);
            throw new BusinessException(404, "设计图纸不存在！");
        }
        // 3.防并发：Redis 原子占位（SETNX）。不能用 selectCount 检查——两个并行请求会在对方
        // INSERT 提交前同时查到 0，双双放行（check-then-act 竞态，实测任务 17/18 复现）。
        // Redis 单线程串行执行 SETNX，同项目同时只有一个请求占位成功。
        String activeKey = ACTIVE_KEY_PREFIX + projectId;
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(activeKey, String.valueOf(userId), ACTIVE_KEY_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("项目已有生成中任务，拒绝重复发起：projectId={}", projectId);
            throw new BusinessException(409, "该项目正在生成中，请等待当前任务完成");
        }
        // 4.建PENDING任务落库（状态机起点）；入库/派发失败必须释放占位，否则项目锁到 TTL
        GeneratedModel record = new GeneratedModel();
        record.setProjectId(projectId);
        record.setUserId(userId);
        record.setStatus(GenerationStatus.PENDING);
        try {
            generatedModelMapper.insert(record);
            log.info("发起生成成功，任务已创建：taskId={},projectId={}", record.getId(), projectId);
            generationExecutor.execute(() -> runGeneration(record, designProject, activeKey));
        } catch (RuntimeException e) {
            stringRedisTemplate.delete(activeKey);
            throw e;
        }
        return toResponse(record);
    }

    // 启动生成（两阶段：读户型 -> 逐房间生图）；终态（SUCCESS/FAILED）一定释放项目占位
    private void runGeneration(GeneratedModel record, DesignProject designProject, String activeKey) {

        // 1.状态改PROCESSING,落库
        record.setStatus(GenerationStatus.PROCESSING);
        generatedModelMapper.updateById(record);
        try {
            // 2.阶段①视觉模型识别户型（失败重试1次）
            FloorPlanResult floorPlan = analyzeFloorPlanWithRetry(designProject);

            // 3.阶段②逐房间串行生图并下载落盘（单间失败重试1次，仍败则整套 FAILED）
            Map<String, String> roomImageUrls = new LinkedHashMap<>();
            for (RoomPlan room : floorPlan.getRooms()) {
                String prompt = buildRoomPrompt(designProject, room);
                String remoteUrl = generateImageWithRetry(prompt, room.getName());
                String localUrl = fileStorageService.downloadFromUrl(remoteUrl, "panoramas");
                roomImageUrls.put(room.getId(), localUrl);
                log.info("房间效果图完成：taskId={}, room={}, url={}", record.getId(), room.getName(), localUrl);
            }

            // 4.组装 photo-tour v2 场景并落库
            PhotoTourScene scene = buildScene(designProject, floorPlan, roomImageUrls);
            record.setProvider("zhipu");
            record.setPanoramaUrl(roomImageUrls.get(floorPlan.getEntryRoomId()));
            record.setPreviewImageUrl(roomImageUrls.get(floorPlan.getEntryRoomId()));
            record.setSceneConfig(objectMapper.writeValueAsString(scene));
            record.setStatus(GenerationStatus.SUCCESS);
            generatedModelMapper.updateById(record);
            log.info("生成成功：taskId={}, 房间数={}", record.getId(), roomImageUrls.size());
        } catch (Exception e) {
            // 5. 任何一步失败->FAILED+记录原因
            record.setStatus(GenerationStatus.FAILED);
            record.setErrorMessage(e.getMessage() == null ? "未知错误"
                    : e.getMessage().substring(0, Math.min(1024, e.getMessage().length())));
            generatedModelMapper.updateById(record);
            log.error("生成失败：taskId={}", record.getId(), e);
        } finally {
            // 6.无论成功失败都释放占位，允许该项目再次发起；Redis 异常不影响落库结果
            try {
                stringRedisTemplate.delete(activeKey);
            } catch (RuntimeException re) {
                log.error("释放生成占位失败（将等 TTL 自动过期）：key={}", activeKey, re);
            }
        }
    }

    // 户型识别（最多2次尝试）
    private FloorPlanResult analyzeFloorPlanWithRetry(DesignProject designProject) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return floorPlanAnalysisService.analyze(designProject);
            } catch (RuntimeException e) {
                last = e;
                log.warn("户型识别第{}次失败：projectId={}", attempt, designProject.getId(), e);
                sleepBeforeRetry(attempt);
            }
        }
        throw new IllegalStateException("户型图识别失败，请换一张更清晰的户型图重试", last);
    }

    // 单房间生图（最多2次尝试）
    private String generateImageWithRetry(String prompt, String roomName) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return aIimageService.generateImageUrl(prompt);
            } catch (RuntimeException e) {
                last = e;
                log.warn("房间[{}]生图第{}次失败", roomName, attempt, e);
                sleepBeforeRetry(attempt);
            }
        }
        throw new IllegalStateException("房间「" + roomName + "」效果图生成失败，请重新生成", last);
    }

    // 重试前等待（第1次失败后等3秒；第2次失败后不再等待）
    private void sleepBeforeRetry(int attempt) {
        if (attempt >= 2) {
            return;
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("生成任务被中断", ie);
        }
    }

    // 组装单个房间的生图提示词：全局风格 + 房间专属特征 + 统一镜头契约
    private String buildRoomPrompt(DesignProject project, RoomPlan room) {
        StringBuilder sb = new StringBuilder("室内装修写实效果图，单个空间：").append(room.getName());
        if (StringUtils.hasText(room.getApproxArea())) {
            sb.append("（").append(room.getApproxArea().trim()).append("）");
        }
        DesignStyle designStyle = DesignStyle.fromCode(project.getStyleLabel());
        if (designStyle != null) {
            sb.append("。装修风格：").append(designStyle.getLabel())
                    .append("，").append(designStyle.getPrompt());
        }
        if (StringUtils.hasText(project.getStyle())) {
            sb.append("。用户补充要求：").append(project.getStyle().trim());
        }
        if (StringUtils.hasText(room.getFeatures())) {
            sb.append("。空间特征：").append(room.getFeatures().trim());
        }
        sb.append("。镜头要求：站在房间中央、人眼高度、水平广角透视，画面写实、光线自然、材质真实，")
                .append("无人物、无文字、无水印、无鱼眼畸变、无全景拼接缝");
        return sb.toString();
    }

    // 组装 photo-tour v2 场景配置（含门方位->热点坐标的规则化映射）
    private PhotoTourScene buildScene(DesignProject project, FloorPlanResult plan,
            Map<String, String> roomImageUrls) {
        Map<String, RoomPlan> planById = plan.getRooms().stream()
                .collect(Collectors.toMap(RoomPlan::getId, r -> r, (a, b) -> a, LinkedHashMap::new));

        List<RoomScene> roomScenes = new ArrayList<>();
        for (RoomPlan room : plan.getRooms()) {
            // 同方位出现多个门时计数，用于横向错开避免圆点重叠
            Map<String, Integer> sideOrdinal = new HashMap<>();
            List<Hotspot> hotspots = new ArrayList<>();
            List<String> connects = room.getConnects();
            List<String> sides = room.getDoorSides();
            for (int i = 0; i < connects.size(); i++) {
                RoomPlan target = planById.get(connects.get(i));
                if (target == null) {
                    continue;
                }
                String side = i < sides.size() ? sides.get(i) : "center";
                int ordinal = sideOrdinal.merge(side, 1, Integer::sum);
                hotspots.add(new Hotspot(target.getId(), target.getName(), hotspotX(side, ordinal), HOTSPOT_Y));
            }
            roomScenes.add(new RoomScene(room.getId(), room.getName(), room.getApproxArea(),
                    roomImageUrls.get(room.getId()), room.getBbox(), hotspots));
        }

        PhotoTourScene scene = new PhotoTourScene();
        DesignStyle designStyle = DesignStyle.fromCode(project.getStyleLabel());
        scene.setStyleLabel(designStyle != null ? designStyle.getLabel() : null);
        scene.setFloorPlanImageUrl(project.getDesignImageUrl());
        scene.setEntryRoomId(plan.getEntryRoomId());
        scene.setRooms(roomScenes);
        return scene;
    }

    // 门方位 -> 水平坐标；同方位第2、3个门错位错开并夹在画面内
    private double hotspotX(String side, int sameSideOrdinal) {
        double base = switch (side) {
            case "left" -> X_LEFT;
            case "right" -> X_RIGHT;
            default -> X_CENTER;
        };
        double offset = switch (sameSideOrdinal) {
            case 2 -> "left".equals(side) ? 0.12 : -0.12;
            case 3 -> "left".equals(side) ? -0.12 : 0.12;
            default -> 0d;
        };
        return Math.max(0.08, Math.min(0.92, base + offset));
    }

    // 查询单个任务,轮询
    @Override
    public GenerationResponse querySingleTask(Long id) {
        Long userId = UserContext.getUserId();
        GeneratedModel generatedModel = generatedModelMapper.selectById(id);
        if (generatedModel == null || !generatedModel.getUserId().equals(userId)) {
            log.warn("查询生成任务失败-不存在或越权：taskId={}, userId={}", id, userId);
            throw new BusinessException(404, "生成任务不存在");
        }
        return toResponse(generatedModel);
    }

    // 项目下的生成记录（倒序）
    @Override
    public List<GenerationResponse> listByProject(Long projectId) {
        Long userId = UserContext.getUserId();
        log.info("查询项目生成记录：projectId={}, userId={}", projectId, userId);
        // 先校验项目归属，防越权列举他人项目的生成记录
        DesignProject project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            log.warn("查询项目生成记录失败-项目不存在或越权：projectId={}, userId={}", projectId, userId);
            throw new BusinessException(404, "项目不存在");
        }
        return generatedModelMapper.selectList(
                new LambdaQueryWrapper<GeneratedModel>()
                        .eq(GeneratedModel::getProjectId, projectId)
                        .orderByDesc(GeneratedModel::getCreatedAt))
                .stream().map(this::toResponse).toList();
    }

    // 当前用户所有生成记录（倒序）
    @Override
    public List<GenerationResponse> listAll() {
        Long userId = UserContext.getUserId();
        log.info("查询我的生成记录：userId={}", userId);
        return generatedModelMapper.selectList(
                new LambdaQueryWrapper<GeneratedModel>()
                        .eq(GeneratedModel::getUserId, userId)
                        .orderByDesc(GeneratedModel::getCreatedAt))
                .stream().map(this::toResponse).toList();
    }
}
