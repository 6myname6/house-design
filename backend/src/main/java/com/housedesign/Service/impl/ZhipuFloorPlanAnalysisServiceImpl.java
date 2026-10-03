package com.housedesign.Service.impl;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.housedesign.Service.FloorPlanAnalysisService;
import com.housedesign.dto.ai.FloorPlanResult;
import com.housedesign.dto.ai.RoomBBox;
import com.housedesign.dto.ai.RoomPlan;
import com.housedesign.entity.DesignProject;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于智谱视觉模型（LangChain4j ChatModel，glm-4.6v-flash）的户型图识别。
 *
 * 数据流：本地户型图 → base64 DataURL（localhost 智谱云端拉不到）→
 * 固定 JSON 契约的系统提示 + 图片 → VLM → 剥离代码围栏 → Jackson 反序列化 →
 * 结构校验与规范化（连接双向闭合 / 房间数上限 / bbox 裁剪 / 入口兜底）。
 *
 * 失败一律抛 IllegalStateException，由上层异步流水线决定重试与 FAILED 落库；
 * 不在本类吞异常或兜底固定户型（避免产出与用户户型无关的漫游）。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ZhipuFloorPlanAnalysisServiceImpl implements FloorPlanAnalysisService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.max-rooms:6}")
    private int maxRooms;

    @Value("${app.storage.location:./storage}")
    private String storageLocation;

    @Value("${app.storage.public-base-url}")
    private String publicBaseUrl;

    @Override
    public FloorPlanResult analyze(DesignProject project) {
        String imageUrl = project.getDesignImageUrl();
        if (!StringUtils.hasText(imageUrl)) {
            throw new IllegalStateException("项目没有户型图，无法识别");
        }

        // 1.组装多模态消息（系统契约 + 一句任务说明 + 户型图）
        SystemMessage systemMessage = SystemMessage.from(buildSystemPrompt());
        List<Content> contents = List.of(
                TextContent.from("请识别这张图片，若这张图片是一套户型图，则识别这套户型图的房间结构，否则说明这张图片不是户型图，无法识别。严格按约定只输出 JSON。"),
                buildImageContent(imageUrl));

        // 2.调视觉模型
        String raw;
        try {
            ChatResponse response = chatModel.chat(systemMessage, UserMessage.from(contents));
            raw = response.aiMessage().text();
            log.info("户型识别原始响应：projectId={}, resp={}", project.getId(), abbreviate(raw, 800));
        } catch (Exception e) {
            log.warn("户型识别视觉模型调用失败：projectId={}", project.getId(), e);
            throw new IllegalStateException("户型图识别模型调用失败：" + safeMessage(e), e);
        }

        // 3.解析 + 校验规范化
        FloorPlanResult result = parse(raw);
        normalize(result);
        log.info("户型识别成功：projectId={}, 户型={}, 房间数={}, 入口={}",
                project.getId(), result.getApartmentType(), result.getRooms().size(), result.getEntryRoomId());
        return result;
    }

    // ---- 消息构造 ----

    private String buildSystemPrompt() {
        return """
                你是一名资深住宅户型分析师。用户会给你一张住宅户型图，你必须只输出一个 JSON 对象，
                不要输出任何解释文字、markdown 标题或 JSON 以外的内容。JSON 结构如下：
                {
                  "apartmentType": "户型类型，如 两室一厅",
                  "entryRoomId": "入户首先进入的主要房间英文 id，通常是客厅 living",
                  "rooms": [
                    {
                      "id": "房间英文蛇形小写标识，如 living/dining/kitchen/master_bedroom/second_bedroom/study/bathroom，同套唯一",
                      "name": "房间中文名，如 客厅",
                      "approxArea": "依据图中尺寸标注或相对比例估算的面积量级，如 约18㎡；无法判断时给合理估值",
                      "features": "30~60字：开间/进深感受、门窗位置与朝向、与其他空间的连通方式，将用于生成该房间写实装修效果图",
                      "connects": ["有实际门洞直接相连的房间 id"],
                      "doorSides": ["与 connects 严格同序，站在该房间中央、面朝房间主要开窗方向时，通往该相连房间的门在画面的方位：left/center/right"],
                      "bbox": {"x": 0.0, "y": 0.0, "w": 0.0, "h": 0.0}
                    }
                  ]
                }
                硬性要求：
                1. 只列主要室内空间（客厅、餐厅、厨房、卫生间、卧室、书房等），最多 %d 个；阳台、玄关、过道等小空间并入相邻房间，不单独列出；
                2. bbox 是该房间在整张户型图中的归一化矩形：左上角为原点，x/y/w/h 均为 0~1 的小数；无法判断时 bbox 输出 null；
                3. connects 必须双向闭合：A 连接 B，则 B 的 connects 也必须包含 A；禁止出现 rooms 中不存在的 id；
                4. entryRoomId 必须是 rooms 中已存在的 id，优先客厅；
                5. 依据图中尺寸标注和相对比例判断面积，绝不臆造户型图中不存在的房间。
                """.formatted(maxRooms);
    }

    /**
     * 本地存储图片读盘转 base64（localhost 地址智谱云端无法访问）；
     * 公网 URL（OSS）直接交模型下载。
     */
    private ImageContent buildImageContent(String imageUrl) {
        if (imageUrl.startsWith(publicBaseUrl)) {
            String relative = imageUrl.substring(publicBaseUrl.length()).replaceFirst("^/", "");
            Path base = Paths.get(storageLocation).toAbsolutePath().normalize();
            Path file = base.resolve(relative).normalize();
            // normalize 后必须仍在存储根目录内，防路径穿越
            if (!file.startsWith(base)) {
                throw new IllegalStateException("非法的本地文件路径：" + relative);
            }
            try {
                byte[] bytes = Files.readAllBytes(file);
                String mime = mimeFromFileName(file.getFileName().toString());
                return ImageContent.from(Base64.getEncoder().encodeToString(bytes), mime);
            } catch (Exception e) {
                log.error("读取本地户型图失败：{}", file, e);
                throw new IllegalStateException("读取本地户型图失败：" + relative, e);
            }
        }
        // OSS 等公网地址：要求公网可达（公共读或有效签名 URL）
        return ImageContent.from(URI.create(imageUrl));
    }

    // ---- 解析与规范化 ----

    private FloorPlanResult parse(String raw) {
        String json = stripCodeFence(raw);
        try {
            FloorPlanResult result = objectMapper.readValue(json, FloorPlanResult.class);
            if (result == null || result.getRooms() == null || result.getRooms().isEmpty()) {
                throw new IllegalStateException("识别结果中没有房间");
            }
            return result;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("户型识别 JSON 解析失败：raw={}", abbreviate(raw, 300), e);
            throw new IllegalStateException("户型识别结果无法解析：" + abbreviate(raw, 200), e);
        }
    }

    /**
     * 校验并规范化 VLM 输出：
     * 房间数截断 → id/name 清洗唯一 → connects 去噪 → 双向闭合 → doorSides 对齐 → bbox 裁剪 → 入口兜底。
     */
    private void normalize(FloorPlanResult result) {
        List<RoomPlan> rooms = result.getRooms();
        if (rooms.size() > maxRooms) {
            log.warn("户型识别房间数 {} 超过上限 {}，截断前 {} 个", rooms.size(), maxRooms, maxRooms);
            result.setRooms(new ArrayList<>(rooms.subList(0, maxRooms)));
            rooms = result.getRooms();
        }

        // 1.id/name 必填、id 规范化为蛇形且唯一
        Set<String> ids = new HashSet<>();
        for (RoomPlan room : rooms) {
            if (!StringUtils.hasText(room.getId()) || !StringUtils.hasText(room.getName())) {
                throw new IllegalStateException("识别结果存在缺少 id/name 的房间");
            }
            room.setId(normalizeId(room.getId()));
            room.setName(room.getName().trim());
            if (!ids.add(room.getId())) {
                throw new IllegalStateException("识别结果房间 id 重复：" + room.getId());
            }
            if (room.getConnects() == null) {
                room.setConnects(new ArrayList<>());
            }
        }

        // 2.connects 清洗：去空去重、剔除自连与不存在的 id
        for (RoomPlan room : rooms) {
            LinkedHashSet<String> connects = new LinkedHashSet<>();
            for (String c : room.getConnects()) {
                if (!StringUtils.hasText(c)) {
                    continue;
                }
                String id = normalizeId(c);
                if (ids.contains(id) && !id.equals(room.getId())) {
                    connects.add(id);
                }
            }
            room.setConnects(new ArrayList<>(connects));
        }

        // 3.双向闭合 + doorSides 与 connects 同序对齐 + bbox 裁剪
        for (RoomPlan room : rooms) {
            for (String targetId : room.getConnects()) {
                RoomPlan target = findById(rooms, targetId);
                if (!target.getConnects().contains(room.getId())) {
                    target.getConnects().add(room.getId());
                }
            }
            List<String> sides = room.getDoorSides() == null ? List.of() : room.getDoorSides();
            List<String> aligned = new ArrayList<>();
            for (int i = 0; i < room.getConnects().size(); i++) {
                aligned.add(normalizeDoorSide(i < sides.size() ? sides.get(i) : null));
            }
            room.setDoorSides(aligned);
            room.setBbox(normalizeBBox(room.getBbox()));
        }

        // 4.entryRoomId 必须存在；缺失时优先客厅，再退首个房间
        String entry = StringUtils.hasText(result.getEntryRoomId()) ? normalizeId(result.getEntryRoomId()) : null;
        if (entry == null || !ids.contains(entry)) {
            entry = rooms.stream().map(RoomPlan::getId)
                    .filter(id -> id.contains("living"))
                    .findFirst()
                    .orElse(rooms.get(0).getId());
        }
        result.setEntryRoomId(entry);
    }

    private RoomPlan findById(List<RoomPlan> rooms, String id) {
        return rooms.stream().filter(r -> r.getId().equals(id)).findFirst().orElseThrow();
    }

    private String normalizeId(String raw) {
        return raw.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }

    private String normalizeDoorSide(String side) {
        if (side == null) {
            return "center";
        }
        return switch (side.trim().toLowerCase()) {
            case "left" -> "left";
            case "right" -> "right";
            default -> "center";
        };
    }

    private RoomBBox normalizeBBox(RoomBBox b) {
        if (b == null || b.getX() == null || b.getY() == null || b.getW() == null || b.getH() == null) {
            return null;
        }
        double x = clamp(b.getX(), 0d, 1d);
        double y = clamp(b.getY(), 0d, 1d);
        double w = clamp(b.getW(), 0d, 1d - x);
        double h = clamp(b.getH(), 0d, 1d - y);
        if (w <= 0.01d || h <= 0.01d) {
            return null;
        }
        b.setX(x);
        b.setY(y);
        b.setW(w);
        b.setH(h);
        return b;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // ---- 小工具 ----

    /** 剥离 VLM 可能包裹的 ```json ... ``` 代码围栏 */
    private String stripCodeFence(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(json|JSON)?", "").trim();
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3).trim();
            }
        }
        return s;
    }

    private String mimeFromFileName(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        // jpg/jpeg 及其它情况统一按 jpeg（户型图上传白名单仅这五类）
        return "image/jpeg";
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : abbreviate(e.getMessage(), 200);
    }

    private String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
