package com.housedesign.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KbSearchResponse {
    /** 回显问题 */
    private String query;
    /** 是否有达标命中 */
    private boolean hit;

    private List<KbSearchHit> results;
}
