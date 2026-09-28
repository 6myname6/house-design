package com.housedesign.dto.scene;

import java.util.List;

import lombok.Data;

/**
 * photo-tour v2 场景配置（序列化为 t_generated_model.scene_config）。
 *
 * 契约见接口文档 §7.1：房间清单 / 入口 / 户型小地图底图 / 每房间效果图与热点。
 */
@Data
public class PhotoTourScene {

    /** 场景类型，固定 photo-tour（前端同时兼容旧标识 panorama-tour） */
    private String type = "photo-tour";

    /** 结构版本号；v1 无 rooms，前端按单房间降级 */
    private int version = 2;

    /** 风格中文名（展示用） */
    private String styleLabel;

    /** 用户原始户型图 URL，查看页小地图底图 */
    private String floorPlanImageUrl;

    /** 首屏房间 id（一般为客厅） */
    private String entryRoomId;

    /** 房间列表 */
    private List<RoomScene> rooms;
}
