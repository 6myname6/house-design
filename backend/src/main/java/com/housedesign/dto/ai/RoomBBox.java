package com.housedesign.dto.ai;

import lombok.Data;

/**
 * 房间在户型图上的归一化矩形（原点左上，取值 0~1）。
 * 用于查看页户型小地图叠块；VLM 未给出时允许为 null。
 */
@Data
public class RoomBBox {

    /** 左上角 X（相对户型图宽度） */
    private Double x;

    /** 左上角 Y（相对户型图高度） */
    private Double y;

    /** 宽度（相对户型图宽度） */
    private Double w;

    /** 高度（相对户型图高度） */
    private Double h;
}
