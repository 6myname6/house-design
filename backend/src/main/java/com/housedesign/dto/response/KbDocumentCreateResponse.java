package com.housedesign.dto.response;

import com.housedesign.entity.KbDocumentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库文档创建结果。
 *
 * <p>只回传标识与初始状态：入库是异步的，创建成功不代表已可检索，
 * 前端需凭 documentId 轮询详情接口拿最终状态。</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KbDocumentCreateResponse {

    /** 文档 ID，后续查询与轮询用 */
    private Long documentId;

    /** 初始状态，固定为 PENDING */
    private KbDocumentStatus status;
}
