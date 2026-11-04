package com.housedesign.Service;

/**
 * 知识库入库编排接口。
 *
 * <p>创建文档时只登记元数据（PENDING），真正的解析、切块、向量化
 * 通过本接口在独立线程池异步执行。</p>
 */
public interface KbIngestService {

    /**
     * 提交入库任务（异步执行，立即返回）。
     *
     * @param documentId 文档 ID
     */
    void submit(Long documentId);

    /**
     * 执行一次入库（通常由线程池调用，也可手动调用做排查）。
     *
     * @param documentId 文档 ID
     */
    void ingest(Long documentId);
}
