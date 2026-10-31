package com.xiaoying.template;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoryMapper {

    @Select("SELECT id, name, sort_order, created_at FROM category ORDER BY sort_order, id")
    List<CategoryEntity> findAll();

    @Select("SELECT id, name, sort_order, created_at FROM category WHERE id = #{id}")
    CategoryEntity findById(@Param("id") String id);
}
