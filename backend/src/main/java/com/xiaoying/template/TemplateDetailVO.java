package com.xiaoying.template;

import java.util.List;

/** 模板详情，包含上传素材的要求。 */
public record TemplateDetailVO(
        String id,
        String categoryId,
        String categoryName,
        String title,
        String coverUrl,
        String description,
        int requiredImageCount,
        List<String> acceptedMimeTypes,
        long maxFileSizeBytes) {}
