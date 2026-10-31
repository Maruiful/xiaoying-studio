package com.xiaoying.template;

final class TemplateConverter {

    private TemplateConverter() {}

    static CategoryVO toVO(CategoryEntity e) {
        return new CategoryVO(e.getId(), e.getName());
    }

    static TemplateCardVO toCardVO(TemplateEntity e) {
        return new TemplateCardVO(
                e.getId(), e.getCategoryId(), e.getTitle(), e.getCoverUrl(), e.getRequiredImageCount());
    }

    static TemplateDetailVO toDetailVO(TemplateEntity e, String categoryName) {
        return new TemplateDetailVO(
                e.getId(),
                e.getCategoryId(),
                categoryName,
                e.getTitle(),
                e.getCoverUrl(),
                e.getDescription(),
                e.getRequiredImageCount(),
                e.getAcceptedMimeTypes(),
                e.getMaxFileSizeBytes());
    }
}
