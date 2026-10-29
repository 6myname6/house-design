package com.housedesign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.housedesign.entity.KbDocument;

/**
 * 知识库文档 Mapper。
 *
 * <p>文档的增删改查全部走 MyBatis-Plus 通用方法，无需自定义 SQL；
 * 状态流转由 Service 层用 updateById 完成。</p>
 */
public interface KbDocumentMapper extends BaseMapper<KbDocument> {

}
