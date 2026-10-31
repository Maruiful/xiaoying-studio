package com.xiaoying.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.xiaoying.common.BizException;
import com.xiaoying.common.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TemplateServiceTest {

    private final CategoryMapper categoryMapper = mock(CategoryMapper.class);
    private final TemplateMapper templateMapper = mock(TemplateMapper.class);
    private final TemplateService service = new TemplateService(categoryMapper, templateMapper);

    @Test
    @DisplayName("模板不存在时抛出 TEMPLATE_NOT_FOUND")
    void getTemplateThrowsWhenMissing() {
        given(templateMapper.findById("nope")).willReturn(null);

        assertThatThrownBy(() -> service.getTemplate("nope"))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.TEMPLATE_NOT_FOUND);
    }

    @Test
    @DisplayName("分类参数为空白时不做筛选")
    void blankCategoryMeansNoFilter() {
        given(templateMapper.findAll(isNull())).willReturn(List.of());

        assertThat(service.listTemplates("  ")).isEmpty();
        verify(templateMapper).findAll(null);
    }

    @Test
    @DisplayName("详情中带上分类名称和素材要求")
    void detailIncludesCategoryNameAndRequirements() {
        TemplateEntity t = new TemplateEntity();
        t.setId("demo");
        t.setCategoryId("creative");
        t.setTitle("示例");
        t.setRequiredImageCount(2);
        t.setAcceptedMimeTypes(List.of("image/jpeg"));
        t.setMaxFileSizeBytes(1024);
        CategoryEntity c = new CategoryEntity();
        c.setId("creative");
        c.setName("创意图片");
        given(templateMapper.findById("demo")).willReturn(t);
        given(categoryMapper.findById("creative")).willReturn(c);

        TemplateDetailVO vo = service.getTemplate("demo");

        assertThat(vo.categoryName()).isEqualTo("创意图片");
        assertThat(vo.requiredImageCount()).isEqualTo(2);
        assertThat(vo.acceptedMimeTypes()).containsExactly("image/jpeg");
    }
}
