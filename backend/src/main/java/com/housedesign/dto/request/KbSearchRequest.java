package com.housedesign.dto.request;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KbSearchRequest {
    /** 检索问题，1-500字符 */
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "检索问题最长500字符")
    private String query;

    /** 返回条数，默认5，取值1-20，越界按照边界收敛 */
    private Integer topK;

    /** 限定文档范围，不传则检索当前用户可见的全部文档 */
    private List<Long> documentIds;

    /** 相似度下限，不传取服务端默认配置 */
    @DecimalMax(value = "1.0", message = "minScore不合法")
    @DecimalMin(value = "0.0", message = "minScore不合法")
    private Double minScore;
}
