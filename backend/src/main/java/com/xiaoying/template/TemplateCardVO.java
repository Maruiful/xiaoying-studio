package com.xiaoying.template;

/** 模板列表中的卡片。 */
public record TemplateCardVO(String id, String categoryId, String title, String coverUrl, int requiredImageCount) {}
