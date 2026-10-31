package com.xiaoying.asset;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssetMapper {

    @Insert("""
            INSERT INTO asset (user_id, template_id, original_name, mime_type, size_bytes, storage_path, created_at)
            VALUES (#{userId}, #{templateId}, #{originalName}, #{mimeType}, #{sizeBytes}, #{storagePath}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AssetEntity asset);

    /** 只返回属于该用户的素材，别人的素材查不到。 */
    @Select("""
            SELECT id, user_id, template_id, original_name, mime_type, size_bytes, storage_path, created_at
            FROM asset WHERE id = #{id} AND user_id = #{userId}
            """)
    AssetEntity findOwned(@Param("id") long id, @Param("userId") String userId);

    /** XML：IN 列表。不存在或属于其他用户的 id 不会出现在结果中。 */
    List<AssetEntity> findOwnedByIds(@Param("userId") String userId, @Param("ids") List<Long> ids);
}
