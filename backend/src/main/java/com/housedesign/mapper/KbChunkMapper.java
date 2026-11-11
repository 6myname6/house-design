package com.housedesign.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.housedesign.entity.KbChunk;

/**
 * 知识库分块 Mapper。
 *
 * <p>
 * 除通用 CRUD 外，额外提供批量插入：一份文档动辄几十到几百个分块，
 * 逐条 insert 会产生同量级的数据库往返，入库耗时不可接受，
 * 因此合并成单条 INSERT ... VALUES (...),(...) 提交。
 * </p>
 */
public interface KbChunkMapper extends BaseMapper<KbChunk> {

        /**
         * 批量插入分块。
         *
         * <p>
         * created_at 交给数据库默认值，不在 SQL 中显式赋值。
         * 调用方需自行控制单批数量（建议 ≤200 块），避免 SQL 过长。
         * </p>
         *
         * @param chunks 待插入分块，不可为空
         * @return 实际插入行数
         */
        @Insert("<script>" +
                        "INSERT INTO t_kb_chunk " +
                        "(document_id, version, chunk_no, heading, content, content_hash, token_count, char_count, is_active) "
                        +
                        "VALUES " +
                        "<foreach collection='list' item='c' separator=','>" +
                        "(#{c.documentId}, #{c.version}, #{c.chunkNo}, #{c.heading}, #{c.content}, " +
                        "#{c.contentHash}, #{c.tokenCount}, #{c.charCount}, #{c.isActive})" +
                        "</foreach>" +
                        "</script>")
        int insertBatch(@Param("list") List<KbChunk> chunks);

        /**
         * 关键词召回：走ngram FULLTEXT，按相关度倒序；只取id/document_id ，正文后续批量回查
         * 
         * @param query       搜索关键词
         * @param documentIds 限定文档范围
         * @param limit       返回结果数量
         * @return 候选文档列表
         */
        @Select("<script> " +
                        "select id,document_id from t_kb_chunk " +
                        "where is_active = 1 "
                        + "and match(heading,content) against (#{query} in natural language mode) " +
                        "and document_id in " +
                        "<foreach collection = 'documentIds' item='d' open='(' separator=',' close=')'>#{d}</foreach> "
                        +
                        "order by match(heading,content) against(#{query} in natural language mode) desc " +
                        " limit #{limit} " + "</script> ")
        List<KbChunk> keywordSearch(@Param("query") String query, @Param("documentIds") List<Long> documentIds,
                        @Param("limit") int limit);
}
