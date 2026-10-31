package com.xiaoying.asset;

import java.time.Instant;
import lombok.Data;

/** 对应 asset 表。storage_path 是相对存储根目录的路径。 */
@Data
public class AssetEntity {
    private Long id;
    private String userId;
    private String templateId;
    private String originalName;
    private String mimeType;
    private long sizeBytes;
    private String storagePath;
    private Instant createdAt;
}
