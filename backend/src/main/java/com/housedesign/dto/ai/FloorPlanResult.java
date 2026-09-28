package com.housedesign.dto.ai;

import java.util.List;

import lombok.Data;

/**
 * 视觉模型识别户型图的输出结果（反序列化 VLM 返回的 JSON）。
 *
 * 契约见接口文档 §4.1 阶段① / §7.1：
 * 房间清单、相邻关系、面积量级全部来自用户真实户型图，不写死房间。
 */
@Data
public class FloorPlanResult {

    /** 户型类型，如「两室一厅」（仅展示/日志用） */
    private String apartmentType;

    /** 入口房间 id（首屏房间，通常为客厅），必须存在于 rooms 中 */
    private String entryRoomId;

    /** 识别出的房间清单（1~app.ai.max-rooms 个） */
    private List<RoomPlan> rooms;
}
