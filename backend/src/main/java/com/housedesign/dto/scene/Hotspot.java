package com.housedesign.dto.scene;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 房间效果图上的跳转圆点（坐标为相对当前房间画面的归一化值，原点左上，0~1）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hotspot {

    /** 目标房间 id（必须同套存在） */
    private String targetRoomId;

    /** 圆点旁展示的目标房间名 */
    private String label;

    /** 水平位置（0~1），由门方位规则化映射 */
    private double x;

    /** 垂直位置（0~1），固定 0.66（门洞大致高度） */
    private double y;
}
