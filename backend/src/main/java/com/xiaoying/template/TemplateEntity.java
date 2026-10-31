package com.xiaoying.template;

import java.time.Instant;
import java.util.List;
import lombok.Data;

/** 对应 template 表。accepted_mime_types 为 JSON 列，经 StringListTypeHandler 映射为 List。 */
@Data
public class TemplateEntity {
    private String id;
    private String categoryId;
    private String title;
    private String coverUrl;
    private String description;
    private int requiredImageCount;
    private List<String> acceptedMimeTypes;
    private long maxFileSizeBytes;
    private int sortOrder;
    private Instant createdAt;
}
