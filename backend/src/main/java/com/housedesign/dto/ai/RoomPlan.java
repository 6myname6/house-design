package com.housedesign.dto.ai;

import java.util.List;

import lombok.Data;

/**
 * 户型识别结果中的单个房间（VLM JSON ↔ Java）。
 */
@Data
public class RoomPlan {

    /** 房间英文标识（蛇形，如 living / master_bedroom），同套唯一 */
    private String id;

    /** 房间中文名，如「客厅」 */
    private String name;

    /** 估算面积量级文本，如「约18㎡」，仅展示，不做精确丈量承诺 */
    private String approxArea;

    /** 门窗/开间/朝向/相连空间等画面特征，拼入该房间生图 prompt */
    private String features;

    /** 相连房间 id 列表（拓扑），后端校验双向闭合 */
    private List<String> connects;

    /**
     * 通往各相连房间的门在当前房间画面中的方位，与 connects 同序；
     * 取值 left / center / right，缺失或越界由后端按 center 兜底。
     */
    private List<String> doorSides;

    /** 房间在户型图上的归一化矩形（原点左上，0~1），小地图叠块用；可能为空 */
    private RoomBBox bbox;
}
