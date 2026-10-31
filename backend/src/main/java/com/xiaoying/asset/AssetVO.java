package com.xiaoying.asset;

import java.time.Instant;

/** 上传成功的素材。url 只有素材的主人能访问。 */
public record AssetVO(
        long id,
        String templateId,
        String originalName,
        String mimeType,
        long sizeBytes,
        String url,
        Instant createdAt) {

    static AssetVO from(AssetEntity e) {
        return new AssetVO(
                e.getId(),
                e.getTemplateId(),
                e.getOriginalName(),
                e.getMimeType(),
                e.getSizeBytes(),
                "/api/files/assets/" + e.getId(),
                e.getCreatedAt());
    }
}
