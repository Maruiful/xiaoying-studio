package com.xiaoying.template;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL 在 resources/mapper/TemplateMapper.xml：需要 JSON 列的 resultMap 和动态条件。 */
@Mapper
public interface TemplateMapper {

    /** categoryId 为 null 时返回全部。 */
    List<TemplateEntity> findAll(@Param("categoryId") String categoryId);

    TemplateEntity findById(@Param("id") String id);
}
