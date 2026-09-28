package com.housedesign.dto.scene;

import java.util.List;

import com.housedesign.dto.ai.RoomBBox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * photo-tour v2 中的单个房间（前端渲染单元）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomScene {

    /** 房间英文标识，同套唯一 */
    private String id;

    /** 房间中文名 */
    private String name;

    /** 估算面积量级文本，如约18㎡ */
    private String approxArea;

    /** 该房间写实宽幅效果图 URL */
    private String imageUrl;

    /** 户型图上的归一化矩形，小地图叠块用；可能为 null */
    private RoomBBox bbox;

    /** 通往相邻房间的圆点热点 */
    private List<Hotspot> hotspots;
}
